package com.meninocoiso.beatstarcommunity.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.meninocoiso.beatstarcommunity.data.repository.ChartRepository
import com.meninocoiso.beatstarcommunity.util.DownloadUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class DownloadViewModel @Inject constructor(
    @Named("Local") private val localChartRepository: ChartRepository,
    val downloadUtils: DownloadUtils
) : ViewModel() {
    suspend fun markChartAsInstalled(chartId: String) = localChartRepository.updateChart(chartId, true)
}