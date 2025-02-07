package com.meninocoiso.beatstarcommunity.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meninocoiso.beatstarcommunity.data.repository.ChartRepository
import com.meninocoiso.beatstarcommunity.domain.model.Chart
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChartViewModel @Inject constructor(
    private val chartRepository: ChartRepository
) : ViewModel() {
    private val _charts = MutableStateFlow<Result<List<Chart>>?>(null)
    val charts: StateFlow<Result<List<Chart>>?> = _charts.asStateFlow()

    fun fetchCharts() {
        _charts.value = null
        viewModelScope.launch {
            chartRepository.getCharts().collect { result ->
                _charts.value = result
            }
        }
    }
}