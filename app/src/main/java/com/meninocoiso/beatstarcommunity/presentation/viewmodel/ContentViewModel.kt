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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

sealed class ContentDownloadState {
    data object Idle : ContentDownloadState()
    data class Downloading(val progress: Float) : ContentDownloadState()
    data class Extracting(val progress: Float) : ContentDownloadState()
    data class Error(val message: String) : ContentDownloadState()
    data object Installed : ContentDownloadState()
}

private const val TAG = "ContentViewModel"

@HiltViewModel
class ContentViewModel @Inject constructor(
    private val contentDownloadRepository: ContentDownloadRepository,
    private val settingsRepository: SettingsRepository,
    @Named("Local") private val localChartRepository: ChartRepository
) : ViewModel() {
    val downloadState: Flow<ContentDownloadState> = _downloadState.asStateFlow()

    suspend fun getFolderUri(): Uri? {
        return settingsRepository.getFolderUri()?.toUri()
    }

    suspend fun setFolderUri(uri: Uri) {
        settingsRepository.setFolderUri(uri.toString())
    }

    fun checkInstallationStatus(chart: Chart) {
        viewModelScope.launch {
            val isInstalled = chart.isInstalled == true

            _downloadState.value = if (isInstalled) ContentDownloadState.Installed else ContentDownloadState.Idle
        }
    }

    fun downloadChart(
        chart: Chart,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            runCatching {
                // Download the chart to the user's device
                contentDownloadRepository.downloadChart(
                    chart.latestVersion.chartUrl,
                    chart.id
                ) {
                    _downloadState.value = it
                }

                // Update the chart in the local database
                val updateResult = localChartRepository.updateChart(chart.id, true).first()
                updateResult.getOrThrow() // Throw if the result is a failure
            }.onSuccess {
                onSuccess()
            }.onFailure { error ->
                Log.e("DownloadViewModel", "Failed to download chart", error)
                onError("Failed to download chart: ${error.message}")
            }
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
                _downloadState.value = ContentDownloadState.Idle

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

    fun markAsInstalled() {
        _downloadState.value = ContentDownloadState.Installed
    }
}