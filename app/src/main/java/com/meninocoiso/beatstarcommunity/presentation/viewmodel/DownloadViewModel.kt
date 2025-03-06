package com.meninocoiso.beatstarcommunity.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meninocoiso.beatstarcommunity.data.repository.ChartRepository
import com.meninocoiso.beatstarcommunity.domain.model.Chart
import com.meninocoiso.beatstarcommunity.util.DownloadUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class DownloadViewModel @Inject constructor(
    val downloadUtils: DownloadUtils,
    @Named("Local") private val localChartRepository: ChartRepository
) : ViewModel() {
    fun deleteChart(
        chart: Chart,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            runCatching {
                val updateResult = localChartRepository.updateChart(chart.id, false).first()
                updateResult.getOrThrow() // Throw if the result is a failure

                // Proceed with getting the folder name and deleting the chart
                val folderName = downloadUtils.getChartFolderName(chart.id, chart.track)

                Log.d("DownloadViewModel", "Deleting chart with folder name: $folderName")

                downloadUtils.deleteChart(folderName)
            }.onSuccess {
                onSuccess()
            }.onFailure { error ->
                Log.e("DownloadViewModel", "Failed to update or delete chart", error)
                onError("Failed to update or delete chart")
            }
        }
    }
}