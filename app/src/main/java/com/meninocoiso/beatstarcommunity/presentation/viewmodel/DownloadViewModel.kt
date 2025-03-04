package com.meninocoiso.beatstarcommunity.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meninocoiso.beatstarcommunity.data.repository.ChartRepository
import com.meninocoiso.beatstarcommunity.util.DownloadUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class DownloadViewModel @Inject constructor(
    @Named("Local") private val localChartRepository: ChartRepository,
    val downloadUtils: DownloadUtils
) : ViewModel() {
    fun markChartAsInstalled(chartId: String) {
        viewModelScope.launch {
            localChartRepository.updateChart(chartId, true)
        }
    }
}