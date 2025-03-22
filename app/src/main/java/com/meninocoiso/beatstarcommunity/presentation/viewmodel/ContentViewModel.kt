package com.meninocoiso.beatstarcommunity.presentation.viewmodel

import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meninocoiso.beatstarcommunity.data.repository.ChartRepository
import com.meninocoiso.beatstarcommunity.data.repository.ContentDownloadRepository
import com.meninocoiso.beatstarcommunity.data.repository.SettingsRepository
import com.meninocoiso.beatstarcommunity.domain.enums.OperationType
import com.meninocoiso.beatstarcommunity.domain.model.Chart
import com.meninocoiso.beatstarcommunity.service.DownloadEvent
import com.meninocoiso.beatstarcommunity.service.DownloadServiceConnection
import dagger.hilt.android.lifecycle.HiltViewModel
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
import javax.inject.Inject
import javax.inject.Named

sealed class ContentState {
    data object Idle : ContentState()
    data class Downloading(val chartId: String, val progress: Float) : ContentState()
    data class Extracting(val chartId: String, val progress: Float) : ContentState()
    data class Error(val chartId: String, val message: String) : ContentState()
    data class Installed(val chartId: String) : ContentState()
}

private const val TAG = "ContentViewModel"

@HiltViewModel
class ContentViewModel @Inject constructor(
    private val downloadServiceConnection: DownloadServiceConnection,
    private val contentDownloadRepository: ContentDownloadRepository,
    private val settingsRepository: SettingsRepository,
    @Named("Local") private val localChartRepository: ChartRepository,
) : ViewModel() {

    private val _contentStates = MutableStateFlow<Map<String, ContentState>>(emptyMap())
    private val contentStates: StateFlow<Map<String, ContentState>> = _contentStates.asStateFlow()

    // Event flow for one-time notifications
    private val _events = MutableSharedFlow<DownloadEvent>(extraBufferCapacity = 10)
    val events: SharedFlow<DownloadEvent> = _events.asSharedFlow()

    // Cache for folder URI to reduce repository calls
    private var cachedFolderUri: Uri? = null

    init {
        observeDownloadEvents()
    }

    private fun observeDownloadEvents() {
        viewModelScope.launch {
            downloadServiceConnection.observeDownload().collect { event ->
                handleDownloadEvent(event)
            }
        }
    }

    private fun handleDownloadEvent(event: DownloadEvent) {
        val chartId = event.chartId

        // Update the state based on the event
        when (event) {
            is DownloadEvent.Progress ->
                updateState(chartId, ContentState.Downloading(chartId, event.progress))

            is DownloadEvent.Extracting ->
                updateState(chartId, ContentState.Extracting(chartId, event.progress))

            is DownloadEvent.Complete -> {
                updateState(chartId, ContentState.Installed(chartId))
                emitEvent(event)
            }

            is DownloadEvent.Error -> {
                updateState(chartId, ContentState.Error(chartId, event.message))
                emitEvent(event)
            }
        }
    }

    // Emit an event to subscribers without suspending
    private fun emitEvent(event: DownloadEvent) {
        _events.tryEmit(event)
    }

    // Update state efficiently with .update
    private fun updateState(chartId: String, state: ContentState) {
        _contentStates.update { currentStates ->
            currentStates.toMutableMap().apply {
                this[chartId] = state
            }
        }
    }

    fun checkInstallationStatus(chartId: String) {
        /*val state = if (chart.isInstalled == true) {
            ContentState.Installed(chart.id)
        } else {
            ContentState.Idle
        }*/
        viewModelScope.launch {
            val state = localChartRepository.getChart(chartId).first().getOrNull()?.let { chart ->
                if (chart.isInstalled == true) ContentState.Installed(chartId) else ContentState.Idle
            } ?: ContentState.Idle

            updateState(chartId, state)
        }
    }

    // Download a chart - with proper error handling
    fun downloadChart(chart: Chart) {
        val chartId = chart.id

        // Update state immediately for UI feedback
        updateState(chartId, ContentState.Downloading(chartId, 0f))

        // Start the download with error handling
        viewModelScope.launch {
            try {
                downloadServiceConnection.startDownload(
                    chartId = chartId,
                    chartUrl = (chart.availableVersion ?: chart.latestVersion).chartUrl,
                    chartName = "${chart.track} - ${chart.artist}",
                    isUpdate = chart.availableVersion != null
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start download", e)
                updateState(chartId, ContentState.Error(chartId, "Failed to start download"))
                emitEvent(DownloadEvent.Error(chartId, "Failed to start download"))
            }
        }
    }

    // Delete a chart
    fun deleteChart(
        chart: Chart,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            runCatching {
                // Update the chart in local database first
                val updateResult = localChartRepository
                    .updateChart(chart.id, OperationType.DELETE)
                    .first()
                updateResult.getOrThrow() // Will throw if update failed

                // Delete the actual chart files
                contentDownloadRepository.deleteChart(chart.id)

                // Reset the state
                updateState(chart.id, ContentState.Idle)
            }
                .onFailure {
                    Log.e(TAG, "Failed to delete chart", it)
                    onError("Failed to delete chart")
                }
                .onSuccess {
                    Log.d(TAG, "Chart deleted successfully")
                    onSuccess()
                }
        }
    }

    // Get chart state efficiently - reusing the existing StateFlow
    fun getContentState(chartId: String): StateFlow<ContentState> {
        return contentStates
            .map { it[chartId] ?: ContentState.Idle }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Lazily,
                initialValue = _contentStates.value[chartId] ?: ContentState.Idle
            )
    }

    // Folder URI handling with caching for better performance
    suspend fun getFolderUri(): Uri? {
        return cachedFolderUri ?: settingsRepository.getFolderUri()?.toUri()?.also {
            cachedFolderUri = it
        }
    }

    suspend fun setFolderUri(uri: Uri) {
        cachedFolderUri = uri
        settingsRepository.setFolderUri(uri.toString())
    }

    // Helper method to get the current state of a chart synchronously
    fun getCurrentState(chartId: String): ContentState {
        return _contentStates.value[chartId] ?: ContentState.Idle
    }
}