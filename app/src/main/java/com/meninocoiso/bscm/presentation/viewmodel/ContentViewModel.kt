package com.meninocoiso.bscm.presentation.viewmodel

import DownloadEvent
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.data.repository.CacheRepository
import com.meninocoiso.bscm.domain.repository.ChartRepository
import com.meninocoiso.bscm.data.repository.DownloadRepository
import com.meninocoiso.bscm.data.repository.SettingsRepository
import com.meninocoiso.bscm.domain.enums.ErrorType
import com.meninocoiso.bscm.domain.enums.OperationOption
import com.meninocoiso.bscm.domain.model.Chart
import com.meninocoiso.bscm.domain.model.internal.Settings
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.io.IOException
import javax.inject.Inject
import javax.inject.Named

private const val TAG = "ContentViewModel"

@HiltViewModel
class ContentViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val downloadServiceMonitor: DownloadServiceMonitor,
    private val downloadRepository: DownloadRepository,
    private val cacheRepository: CacheRepository,
    private val settingsRepository: SettingsRepository,
    @param:Named("Local") private val localChartRepository: ChartRepository,
) : ViewModel() {

    private val _contentStates = MutableStateFlow<Map<String, ContentState>>(emptyMap())
    private val contentStates: StateFlow<Map<String, ContentState>> = _contentStates.asStateFlow()

    // Event flow for one-time notifications
    private val _events = MutableSharedFlow<DownloadEvent>(
        extraBufferCapacity = 10,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<DownloadEvent> = _events.asSharedFlow()

    // Cache for folder URI to reduce repository calls
    private var cachedFolderUri: Uri? = null
    private val folderUriLock = Mutex()

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
                            ContentState.Downloading(chart.id, 0f) // Will be updated by events
                        } else {
                            ContentState.Idle
                        }
                    }
                    isInstalled -> ContentState.Installed(chart.id)
                    else -> ContentState.Idle
                }

                updateState(chart.id, state)
                // Log.d(TAG, "Status checked for chart ${chart.id}: $state")
            } catch (e: Exception) {
                Log.e(TAG, "Error checking chart status for ${chart.id}", e)
                updateState(chart.id, ContentState.Error(
                    chart.id,
                    "Failed to check chart status",
                    ErrorType.UNKNOWN
                ))
            }
        }
    }

    private suspend fun handleDownloadEvent(event: DownloadEvent) {
        val chartId = event.chartId

        try {
            // Update the state based on the event
            when (event) {
                is DownloadEvent.Started ->
                    updateState(chartId, ContentState.Downloading(chartId, 0f))

                is DownloadEvent.Progress ->
                    updateState(chartId, ContentState.Downloading(chartId, event.progress))

                is DownloadEvent.Extracting ->
                    updateState(chartId, ContentState.Extracting(chartId, event.progress))

                is DownloadEvent.Complete -> {
                    updateState(chartId, ContentState.Installed(chartId))
                    emitEvent(event)
                    // Clear any pending operations
                    clearChartOperation(chartId)
                }

                is DownloadEvent.Error -> {
                    updateState(chartId, ContentState.Error(chartId, event.message, event.type))
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
    private fun updateState(chartId: String, state: ContentState) {
        _contentStates.update { currentStates ->
            currentStates.toMutableMap().apply {
                this[chartId] = state
            }
        }
    }

    /**
     * Downloads a chart with improved error handling and duplicate prevention
     */
    fun downloadChart(chart: Chart) {
        val chartId = chart.id

        viewModelScope.launch {
            try {
                // Check if operation is already in progress
                if (!setChartOperation(chartId, "download")) {
                    Log.w(TAG, "Download operation already in progress for chart: $chartId")
                    updateState(chartId, ContentState.Error(
                        chartId,
                        context.getString(R.string.operation_in_progress),
                        ErrorType.DOWNLOAD_ERROR
                    ))
                    return@launch
                }

                // Update state immediately for UI feedback
                updateState(chartId, ContentState.Downloading(chartId, 0f))

                // Validate chart data
                val version = chart.availableVersion ?: chart.latestVersion
                if (version.bundleUrl.isBlank()) {
                    throw IllegalArgumentException("Bundle URL is empty")
                }

                // Start the download
                downloadServiceMonitor.startDownload(
                    chartId = chartId,
                    bundleUrl = version.bundleUrl,
                    chartName = "${chart.track} - ${chart.artist}",
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

                updateState(chartId, ContentState.Error(chartId, errorMessage, ErrorType.DOWNLOAD_ERROR))
                emitEvent(DownloadEvent.Error(chartId, errorMessage, ErrorType.DOWNLOAD_ERROR))
            }
        }
    }


    /**
     * Deletes a chart with improved error handling and validation
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
                val currentState = _contentStates.value[chartId]
                if (currentState is ContentState.Downloading || currentState is ContentState.Extracting) {
                    clearChartOperation(chartId)
                    val errorMsg = context.getString(R.string.cannot_delete_during_download)
                    onError(errorMsg)
                    return@launch
                }

                // Update the chart in local database first
                val updateResult = localChartRepository
                    .updateChart(chartId, OperationOption.DELETE)
                    .first()

                updateResult.getOrThrow() // Will throw if update failed

                // Delete the actual chart files
                try {
                    downloadRepository.deleteChart(chartId)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to delete chart files, but database was updated", e)
                    // Continue - the database update is more important
                }

                // Reset the state and clear operation
                updateState(chartId, ContentState.Idle)
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

    /**
     * Get chart state efficiently - reusing the existing StateFlow
     */
    fun getContentState(chartId: String): StateFlow<ContentState> {
        return contentStates
            .map { it[chartId] ?: ContentState.Idle }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Lazily,
                initialValue = _contentStates.value[chartId] ?: ContentState.Idle
            )
    }

    /**
     * Gets all content states for batch operations
     */
    fun getAllContentStates(): StateFlow<Map<String, ContentState>> = contentStates

    /**
     * Folder URI handling with caching and improved error handling
     */
    suspend fun getFolderUri(): Uri? {
        return folderUriLock.withLock {
            try {
                cachedFolderUri ?: cacheRepository.getFolderUri()?.also { uri ->
                    // Validate URI is still accessible
                    if (isUriAccessible(uri)) {
                        cachedFolderUri = uri
                    } else {
                        Log.w(TAG, "Cached folder URI is no longer accessible")
                        cacheRepository.setFolderUri("") // Clear invalid URI
                        return@withLock null
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting folder URI", e)
                null
            }
        }
    }

    suspend fun setFolderUri(uri: Uri) {
        folderUriLock.withLock {
            try {
                if (isUriAccessible(uri)) {
                    cachedFolderUri = uri
                    cacheRepository.setFolderUri(uri.toString())
                    Log.d(TAG, "Folder URI updated successfully")
                } else {
                    throw IllegalArgumentException("Provided URI is not accessible")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error setting folder URI", e)
                throw e
            }
        }
    }

    /**
     * Validates that a URI is still accessible
     */
    private fun isUriAccessible(uri: Uri): Boolean {
        return try {
            val documentFile = DocumentFile.fromTreeUri(context, uri)
            documentFile?.exists() == true && documentFile.canWrite()
        } catch (e: Exception) {
            Log.w(TAG, "URI accessibility check failed", e)
            false
        }
    }

    /**
     * Clears the cached folder URI (useful when permissions are revoked)
     */
    suspend fun clearFolderUriCache() {
        folderUriLock.withLock {
            cachedFolderUri = null
        }
    }

    /**
     * Gets statistics about download operations
     */
    suspend fun getDownloadStatistics(): DownloadStatistics {
        val states = _contentStates.value
        return DownloadStatistics(
            totalCharts = states.size,
            installedCharts = states.values.count { it is ContentState.Installed },
            downloadingCharts = states.values.count { it is ContentState.Downloading },
            extractingCharts = states.values.count { it is ContentState.Extracting },
            errorCharts = states.values.count { it is ContentState.Error },
            activeDownloads = downloadServiceMonitor.getActiveDownloads().size
        )
    }

    /*override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "ContentViewModel cleared")
    }*/
}

/**
 * Enhanced ContentState with better error information
 */
sealed class ContentState {
    data object Idle : ContentState()
    data class Downloading(val chartId: String, val progress: Float) : ContentState()
    data class Extracting(val chartId: String, val progress: Float) : ContentState()
    data class Error(
        val chartId: String,
        val message: String,
        val type: ErrorType? = null,
        val timestamp: Long = System.currentTimeMillis()
    ) : ContentState()
    data class Installed(val chartId: String) : ContentState()
}

/**
 * Data class for download statistics
 */
data class DownloadStatistics(
    val totalCharts: Int,
    val installedCharts: Int,
    val downloadingCharts: Int,
    val extractingCharts: Int,
    val errorCharts: Int,
    val activeDownloads: Int
)