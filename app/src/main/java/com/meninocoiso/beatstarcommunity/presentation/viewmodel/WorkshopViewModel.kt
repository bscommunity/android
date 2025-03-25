package com.meninocoiso.beatstarcommunity.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meninocoiso.beatstarcommunity.data.manager.ChartManager
import com.meninocoiso.beatstarcommunity.data.manager.ChartResult
import com.meninocoiso.beatstarcommunity.data.manager.ChartsState
import com.meninocoiso.beatstarcommunity.data.manager.FetchEvent
import com.meninocoiso.beatstarcommunity.domain.model.Chart
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "WorkshopViewModel"

@HiltViewModel
class WorkshopViewModel @Inject constructor(
    private val chartManager: ChartManager
) : ViewModel() {

    private val _charts = MutableStateFlow<ChartsState>(ChartsState.Loading())
    val charts: StateFlow<ChartsState> = _charts.asStateFlow()

    private val _events = MutableSharedFlow<FetchEvent>()
    val events: SharedFlow<FetchEvent> = _events.asSharedFlow()

    init {
        // Initialize by loading cached charts, then fetch fresh data
        viewModelScope.launch {
            // Start with cached charts
            val cachedResult = chartManager.loadCachedCharts()
            handleCachedChartsResult(cachedResult)

            // Then fetch fresh data
            refresh(false)

            // Subscribe to workshop charts flow for any updates
            chartManager.workshopCharts.collect { latestCharts ->
                // Only update if we're in success state to avoid overriding error/loading states

                // TODO: If we're in error state, it doesn't update
                if (_charts.value is ChartsState.Success) {
                    _charts.value = ChartsState.Success(latestCharts)
                }
            }
        }
    }

    /**
     * Refresh charts data from remote source
     * @param showLoading Whether to show loading state or keep showing existing data
     */
    fun refresh(showLoading: Boolean = true) {
        viewModelScope.launch {
            // If we want to show loading state, update the UI
            if (showLoading) {
                // Preserve existing charts while loading
                _charts.value = ChartsState.Loading(
                    when (val currentState = _charts.value) {
                        is ChartsState.Success -> currentState.charts
                        is ChartsState.Error -> currentState.charts
                        is ChartsState.Loading -> currentState.charts
                    }
                )
            }

            // Fetch new data
            chartManager.refreshRemoteCharts(forceRefresh = true).collect { result ->
                when (result) {
                    is ChartResult.Success -> {
                        _charts.value = ChartsState.Success(result.data)
                    }
                    is ChartResult.Error -> {
                        // Keep showing old data but mark as error
                        val charts = when (val currentState = _charts.value) {
                            is ChartsState.Success -> currentState.charts
                            is ChartsState.Error -> currentState.charts
                            is ChartsState.Loading -> currentState.charts
                        }

                        if (charts.isNotEmpty()) {
                            _events.emit(FetchEvent.Error(result.message))
                        } else {
                            _charts.value = ChartsState.Error(
                                charts = charts,
                                message = result.message
                            )
                        }
                    }
                    ChartResult.Loading -> {
                        // Already handled above
                    }
                }
            }
        }
    }

    private fun handleCachedChartsResult(result: ChartResult<List<Chart>>) {
        when (result) {
            is ChartResult.Success -> {
                if (result.data.isNotEmpty()) {
                    _charts.value = ChartsState.Success(result.data)
                }
            }
            is ChartResult.Error -> {
                _charts.value = ChartsState.Error(message = result.message)
            }
            ChartResult.Loading -> {
                // No-op, we're already in Loading state
            }
        }
    }
}