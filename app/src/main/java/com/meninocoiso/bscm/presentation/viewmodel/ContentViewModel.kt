package com.meninocoiso.bscm.presentation.viewmodel

import DownloadEvent
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.data.repository.DownloadRepository
import com.meninocoiso.bscm.data.repository.SettingsRepository
import com.meninocoiso.bscm.domain.enums.ErrorType
import com.meninocoiso.bscm.domain.model.Chart
import com.meninocoiso.bscm.domain.model.internal.Settings
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
    settingsRepository: SettingsRepository,
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
        val chartId = event.id

        try {
            // Update the state based on the event
            when (event) {
                is DownloadEvent.Started ->
                    updateState(chartId, DownloadState.Downloading(chartId, 0f))

                is DownloadEvent.Progress ->
                    updateState(chartId, DownloadState.Downloading(chartId, event.progress))

                is DownloadEvent.Extracting ->
                    updateState(chartId, DownloadState.Extracting(chartId, event.progress))

                is DownloadEvent.Complete -> {
                    updateState(chartId, DownloadState.Installed(chartId))
                    emitEvent(event)
                    // Clear any pending operations
                    clearChartOperation(chartId)
                }

                is DownloadEvent.Error -> {
                    updateState(chartId, DownloadState.Error(chartId, event.message, event.type))
                    emitEvent(event)
                    // Clear any pending operations
                    clearChartOperation(chartId)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling download event: $event", e)
        }
    }

    private suspend fun clearChartOperation(chartId: String) {
        chartOperationsLock.withLock {
            chartOperations.remove(chartId)
        }
    }

    private suspend fun setChartOperation(chartId: String, operation: String): Boolean {
        return chartOperationsLock.withLock {
            if (chartOperations.containsKey(chartId)) {
                false // Operation already in progress
            } else {
                chartOperations[chartId] = operation
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
    private fun updateState(chartId: String, state: DownloadState) {
        _downloadStates.update { currentStates ->
            currentStates.toMutableMap().apply {
                this[chartId] = state
            }
        }
    }

    /**
     * Downloads a chart
     */
    fun downloadChart(chart: Chart) {
        val chartId = chart.id

        viewModelScope.launch {
            try {
                // Check if operation is already in progress
                if (!setChartOperation(chartId, "download")) {
                    Log.w(TAG, "Download operation already in progress for chart: $chartId")
                    updateState(chartId, DownloadState.Error(
                        chartId,
                        context.getString(R.string.operation_in_progress),
                        ErrorType.DOWNLOAD_ERROR
                    ))
                    return@launch
                }

                // Update state immediately for UI feedback
                updateState(chartId, DownloadState.Downloading(chartId, 0f))

                // Validate chart data
                val version = chart.availableVersion ?: chart.latestVersion
                if (version.bundleUrl.isBlank()) {
                    throw IllegalArgumentException("Bundle URL is empty")
                }

                // Start the download
                downloadServiceMonitor.startDownload(
                    id = chartId,
                    contentId = chart.contentId,
                    name = "${chart.track} - ${chart.artist}",
                    bundleUrl = version.bundleUrl,
                    isUpdate = chart.availableVersion != null
                )

                Log.d(TAG, "Download started for chart: $chartId")

            } catch (e: Exception) {
                Log.e(TAG, "Failed to start download for chart: $chartId", e)

                // Clear operation and update state
                clearChartOperation(chartId)
                val errorMessage = when (e) {
                    is IllegalArgumentException -> e.message ?: "Invalid chart data"
                    else -> context.getString(R.string.failed_to_start_download)
                }

                updateState(chartId, DownloadState.Error(chartId, errorMessage, ErrorType.DOWNLOAD_ERROR))
                emitEvent(DownloadEvent.Error(chartId, errorMessage, ErrorType.DOWNLOAD_ERROR))
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
        val chartId = chart.id

        viewModelScope.launch {
            try {
                // Check if operation is already in progress
                if (!setChartOperation(chartId, "delete")) {
                    val errorMsg = context.getString(R.string.operation_in_progress)
                    onError(errorMsg)
                    return@launch
                }

                // Validate chart state
                val currentState = _downloadStates.value[chartId]
                if (currentState is DownloadState.Downloading || currentState is DownloadState.Extracting) {
                    clearChartOperation(chartId)
                    val errorMsg = context.getString(R.string.cannot_delete_during_download)
                    onError(errorMsg)
                    return@launch
                }

                // Remove files and persist deletion state from a single repository path.
                downloadRepository.deleteChart(chartId, chart.contentId)

                // Reset the state and clear operation
                updateState(chartId, DownloadState.Idle)
                clearChartOperation(chartId)

                Log.d(TAG, "Chart deleted successfully: $chartId")
                onSuccess()

            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete chart: $chartId", e)
                clearChartOperation(chartId)

                val errorMessage = when (e) {
                    is SecurityException -> context.getString(R.string.permission_denied_delete)
                    is IOException -> context.getString(R.string.storage_error_delete)
                    else -> context.getString(R.string.failed_to_delete_chart)
                }

                onError(errorMessage)
            }
        }
    }

    fun getDownloadState(chartId: String): StateFlow<DownloadState> {
        return downloadStates
            .map { it[chartId] ?: DownloadState.Idle }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Lazily,
                initialValue = _downloadStates.value[chartId] ?: DownloadState.Idle
            )
    }
}