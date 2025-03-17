package com.meninocoiso.beatstarcommunity.presentation.viewmodel

import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meninocoiso.beatstarcommunity.data.repository.ChartRepository
import com.meninocoiso.beatstarcommunity.data.repository.ContentDownloadRepository
import com.meninocoiso.beatstarcommunity.data.repository.SettingsRepository
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

sealed class ContentDownloadState {
    data object Idle : ContentDownloadState()
    data class Downloading(val chartId: String, val progress: Float) : ContentDownloadState()
    data class Extracting(val chartId: String, val progress: Float) : ContentDownloadState()
    data class Error(val chartId: String, val message: String) : ContentDownloadState()
    data class Installed(val chartId: String) : ContentDownloadState()
}

private const val TAG = "ContentViewModel"

@HiltViewModel
class ContentViewModel @Inject constructor(
    private val downloadServiceConnection: DownloadServiceConnection,
    private val contentDownloadRepository: ContentDownloadRepository,
    private val settingsRepository: SettingsRepository,
    @Named("Local") private val localChartRepository: ChartRepository,
) : ViewModel() {

    /// Map chartId to its download state
    private val _downloadStates = MutableStateFlow<Map<String, ContentDownloadState>>(emptyMap())
    // val downloadStates: StateFlow<Map<String, ContentDownloadState>> = _downloadStates.asStateFlow()

    private val _eventFlow = MutableSharedFlow<DownloadEvent>()
    val eventFlow: SharedFlow<DownloadEvent> = _eventFlow.asSharedFlow()

    init {
        viewModelScope.launch {
            downloadServiceConnection.observeDownload().collect { event ->
                val chartId = event.chartId
                val currentStates = _downloadStates.value.toMutableMap()

                when (event) {
                    is DownloadEvent.Progress -> {
                        currentStates[chartId] = ContentDownloadState.Downloading(chartId, event.progress)
                    }
                    is DownloadEvent.Extracting -> {
                        currentStates[chartId] = ContentDownloadState.Extracting(chartId, event.progress)
                    }
                    is DownloadEvent.Complete -> {
                        currentStates[chartId] = ContentDownloadState.Installed(chartId)
                        _eventFlow.emit(event) // Emit single event
                    }
                    is DownloadEvent.Error -> {
                        currentStates[chartId] = ContentDownloadState.Error(chartId, event.message)
                        _eventFlow.emit(event) // Emit single event
                    }
                }

                _downloadStates.value = currentStates
            }
        }
    }

    // Check if a chart is installed
    fun checkInstallationStatus(chart: Chart) {
        val chartId = chart.id
        val currentStates = _downloadStates.value.toMutableMap()

        if (chart.isInstalled == true) {
            currentStates[chartId] = ContentDownloadState.Installed(chartId)
        } else {
            currentStates[chartId] = ContentDownloadState.Idle
        }

        _downloadStates.value = currentStates
    }

    // Download a chart
    fun downloadChart(chart: Chart) {
        val chartId = chart.id

        // Update state immediately for UI feedback
        updateState(chartId, ContentDownloadState.Downloading(chartId, 0f))

        // Start the download
        viewModelScope.launch {
            downloadServiceConnection.startDownload(
                chartId = chartId,
                chartUrl = chart.latestVersion.chartUrl,
                chartName = "${chart.track} - ${chart.artist}"
            )
        }
    }

    fun deleteChart(
        chart: Chart,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            runCatching {
                // Update the chart in the local database
                val updateResult = localChartRepository.updateChart(chart.id, false).first()

                // Update the download state
                updateState(chart.id, ContentDownloadState.Idle)

                updateResult.getOrThrow() // Throw if the result is a failure

                // Delete the chart from the user's device
                try {
                    contentDownloadRepository.deleteChart(chart.id)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to delete chart", e)
                    throw e
                }
            }.onSuccess {
                onSuccess()
            }.onFailure { error ->
                Log.e("DownloadViewModel", "Failed to delete chart", error)
                onError("Failed to delete chart")
            }
        }
    }

    private fun updateState(chartId: String, state: ContentDownloadState) {
        val currentStates = _downloadStates.value.toMutableMap()
        currentStates[chartId] = state
        _downloadStates.value = currentStates
    }

    // Then consumers can observe the state rather than use callbacks
    fun getDownloadState(chartId: String): StateFlow<ContentDownloadState?> {
        return _downloadStates.map { it[chartId] }.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            _downloadStates.value[chartId]
        )
    }

    suspend fun getFolderUri(): Uri? {
        return settingsRepository.getFolderUri()?.toUri()
    }

    suspend fun setFolderUri(uri: Uri) {
        settingsRepository.setFolderUri(uri.toString())
    }
}