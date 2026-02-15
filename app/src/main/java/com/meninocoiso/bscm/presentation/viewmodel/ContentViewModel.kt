package com.meninocoiso.bscm.presentation.viewmodel

import DownloadEvent
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.data.manager.ChartManager
import com.meninocoiso.bscm.data.repository.DownloadRepository
import com.meninocoiso.bscm.data.repository.SettingsRepository
import com.meninocoiso.bscm.domain.enums.ErrorType
import com.meninocoiso.bscm.domain.enums.OperationOption
import com.meninocoiso.bscm.domain.model.Chart
import com.meninocoiso.bscm.domain.model.internal.Settings
import com.meninocoiso.bscm.domain.result.ContentResult
import com.meninocoiso.bscm.domain.state.DownloadState
import com.meninocoiso.bscm.monitor.DownloadServiceMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.io.IOException
import javax.inject.Inject

private const val TAG = "ContentViewModel"

@HiltViewModel
class ContentViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val downloadServiceMonitor: DownloadServiceMonitor,
    private val downloadRepository: DownloadRepository,
    private val settingsRepository: SettingsRepository,
    private val chartManager: ChartManager,
) : ViewModel() {

    private val _downloadStates = MutableStateFlow<Map<String, DownloadState>>(emptyMap())
    private val downloadStates: StateFlow<Map<String, DownloadState>> = _downloadStates.asStateFlow()

    // Event flow for one-time notifications
    private val _events = MutableSharedFlow<DownloadEvent>(
        extraBufferCapacity = 10,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<DownloadEvent> = _events.asSharedFlow()

    // Track operation status to prevent concurrent operations on same chart
    private val chartOperations = mutableMapOf<String, String>()
    private val chartOperationsLock = Mutex()

    val isGameplayVideoPreviewEnabled = settingsRepository.settingsFlow
        .map { it.enableGameplayPreviewVideo }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = Settings().enableGameplayPreviewVideo
        )

    init {
        observeDownloadEvents()
    }

    private fun observeDownloadEvents() {
        viewModelScope.launch {
            downloadRepository.downloadEvents.collect { event ->
                handleDownloadEvent(event)
            }
        }
    }

    /**
     * Checks the installation status of a chart
     */
    fun checkStatus(chart: Chart) {
        viewModelScope.launch {
            try {
                val isInstalled = chart.isInstalled == true
                val isDownloadActive = downloadServiceMonitor.isDownloadActive(chart.id)

                val state = when {
                    isDownloadActive -> {
                        // Check what type of download is active
                        val activeDownloads = downloadServiceMonitor.getActiveDownloads()
                        if (chart.id in activeDownloads) {
                            DownloadState.Downloading(chart.id, 0f) // Will be updated by events
                        } else {
                            DownloadState.Idle
                        }
                    }
                    isInstalled -> DownloadState.Installed(chart.id)
                    else -> DownloadState.Idle
                }

                updateState(chart.id, state)
                // Log.d(TAG, "Status checked for chart ${chart.id}: $state")
            } catch (e: Exception) {
                Log.e(TAG, "Error checking chart status for ${chart.id}", e)
                updateState(chart.id, DownloadState.Error(
                    chart.id,
                    "Failed to check chart status",
                    ErrorType.UNKNOWN
                ))
            }
        }
    }

    private suspend fun handleDownloadEvent(event: DownloadEvent) {
        val contentId = event.id

        try {
            // Update the state based on the event
            when (event) {
                is DownloadEvent.Started ->
                    updateState(contentId, DownloadState.Downloading(contentId, 0f))

                is DownloadEvent.Progress ->
                    updateState(contentId, DownloadState.Downloading(contentId, event.progress))

                is DownloadEvent.Extracting ->
                    updateState(contentId, DownloadState.Extracting(contentId, event.progress))

                is DownloadEvent.Complete -> {
                    updateState(contentId, DownloadState.Installed(contentId))
                    emitEvent(event)
                    // Clear any pending operations
                    clearChartOperation(contentId)
                }

                is DownloadEvent.Error -> {
                    updateState(contentId, DownloadState.Error(contentId, event.message, event.type))
                    emitEvent(event)
                    // Clear any pending operations
                    clearChartOperation(contentId)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling download event: $event", e)
        }
    }

    private suspend fun clearChartOperation(contentId: String) {
        chartOperationsLock.withLock {
            chartOperations.remove(contentId)
        }
    }

    private suspend fun setChartOperation(contentId: String, operation: String): Boolean {
        return chartOperationsLock.withLock {
            if (chartOperations.containsKey(contentId)) {
                false // Operation already in progress
            } else {
                chartOperations[contentId] = operation
                true
            }
        }
    }

    // Emit an event to subscribers without suspending
    private fun emitEvent(event: DownloadEvent) {
        val success = _events.tryEmit(event)
        if (!success) {
            Log.w(TAG, "Failed to emit event: $event (buffer may be full)")
        }
    }

    // Update state efficiently with .update
    private fun updateState(contentId: String, state: DownloadState) {
        _downloadStates.update { currentStates ->
            currentStates.toMutableMap().apply {
                this[contentId] = state
            }
        }
    }

    /**
     * Downloads a chart
     */
    fun downloadChart(chart: Chart) {
        val contentId = chart.id

        viewModelScope.launch {
            try {
                // Check if operation is already in progress
                if (!setChartOperation(contentId, "download")) {
                    Log.w(TAG, "Download operation already in progress for chart: $contentId")
                    updateState(contentId, DownloadState.Error(
                        contentId,
                        context.getString(R.string.operation_in_progress),
                        ErrorType.DOWNLOAD_ERROR
                    ))
                    return@launch
                }

                // Update state immediately for UI feedback
                updateState(contentId, DownloadState.Downloading(contentId, 0f))

                // Validate chart data
                val version = chart.availableVersion ?: chart.latestVersion
                if (version.bundleUrl.isBlank()) {
                    throw IllegalArgumentException("Bundle URL is empty")
                }

                // Start the download
                downloadServiceMonitor.startDownload(
                    id = contentId,
                    name = "${chart.track} - ${chart.artist}",
                    bundleUrl = version.bundleUrl,
                    isUpdate = chart.availableVersion != null
                )

                Log.d(TAG, "Download started for chart: $contentId")

            } catch (e: Exception) {
                Log.e(TAG, "Failed to start download for chart: $contentId", e)

                // Clear operation and update state
                clearChartOperation(contentId)
                val errorMessage = when (e) {
                    is IllegalArgumentException -> e.message ?: "Invalid chart data"
                    else -> context.getString(R.string.failed_to_start_download)
                }

                updateState(contentId, DownloadState.Error(contentId, errorMessage, ErrorType.DOWNLOAD_ERROR))
                emitEvent(DownloadEvent.Error(contentId, errorMessage, ErrorType.DOWNLOAD_ERROR))
            }
        }
    }


    /**
     * Deletes a chart
     */
    fun deleteChart(
        chart: Chart,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val contentId = chart.contentId

        if (contentId.isNullOrBlank()) {
            onError("Only charts downloaded via the app can be deleted.")
            return
        }

        viewModelScope.launch {
            try {
                // Check if operation is already in progress
                if (!setChartOperation(contentId, "delete")) {
                    val errorMsg = context.getString(R.string.operation_in_progress)
                    onError(errorMsg)
                    return@launch
                }

                // Validate chart state
                val currentState = _downloadStates.value[contentId]
                if (currentState is DownloadState.Downloading || currentState is DownloadState.Extracting) {
                    clearChartOperation(contentId)
                    val errorMsg = context.getString(R.string.cannot_delete_during_download)
                    onError(errorMsg)
                    return@launch
                }

                // Delete the actual chart files
                try {
                    downloadRepository.deleteChart(contentId)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to delete chart files, but database was updated", e)
                    // Continue - the database update is more important
                }

                // Update the chart in local database
                val updateResult = chartManager
                    .updateContent(contentId, OperationOption.DELETE)

                if (updateResult is ContentResult.Error) {
                    throw IllegalStateException(updateResult.message)
                }

                // Reset the state and clear operation
                updateState(contentId, DownloadState.Idle)
                clearChartOperation(contentId)

                Log.d(TAG, "Chart deleted successfully: $contentId")
                onSuccess()

            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete chart: $contentId", e)
                clearChartOperation(contentId)

                val errorMessage = when (e) {
                    is SecurityException -> context.getString(R.string.permission_denied_delete)
                    is IOException -> context.getString(R.string.storage_error_delete)
                    else -> context.getString(R.string.failed_to_delete_chart)
                }

                onError(errorMessage)
            }
        }
    }

    fun getDownloadState(contentId: String): StateFlow<DownloadState> {
        return downloadStates
            .map { it[contentId] ?: DownloadState.Idle }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Lazily,
                initialValue = _downloadStates.value[contentId] ?: DownloadState.Idle
            )
    }
}