package com.meninocoiso.beatstarcommunity.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meninocoiso.beatstarcommunity.data.manager.ChartManager
import com.meninocoiso.beatstarcommunity.data.manager.ChartState
import com.meninocoiso.beatstarcommunity.data.manager.FetchEvent
import com.meninocoiso.beatstarcommunity.data.manager.FetchResult
import com.meninocoiso.beatstarcommunity.domain.model.Chart
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "WorkshopViewModel"

@HiltViewModel
class WorkshopViewModel @Inject constructor(
    private val chartManager: ChartManager
) : ViewModel() {

    val charts: Flow<List<Chart>> = chartManager.workshopCharts
    val state = chartManager.workshopState

    private val _events = MutableSharedFlow<FetchEvent>()
    val events: SharedFlow<FetchEvent> = _events.asSharedFlow()

    init {
        // Initialize by loading cached charts, then fetch fresh data
        viewModelScope.launch {
            val cachedResult = chartManager.loadCachedCharts()
            handleCachedChartsResult(cachedResult)

            refresh(false)
        }
    }

    /**
     * Refresh charts data from remote source
     * @param showLoading Whether to show loading state or keep showing existing data
     */
    fun refresh(showLoading: Boolean = true) {
        viewModelScope.launch {
            // If we want to show loading state, update the UI
            /*if (showLoading) {
                chartManager.updateState(ChartState.Loading)
            }*/
            chartManager.updateState(ChartState.Loading)

            // Fetch new data
            chartManager.refreshRemoteCharts(forceRefresh = true).collect { result ->
                when (result) {
                    is FetchResult.Success -> {
                        chartManager.updateState(ChartState.Success)
                    }
                    is FetchResult.Error -> {
                        // Emit an event for the UI to show an error message,
                        // only if we already have charts to display
                        if (showLoading && chartManager.getChartsLength() > 0) {
                            _events.emit(FetchEvent.Error(result.message))
                        }
                        chartManager.updateState(ChartState.Error)
                    }
                    FetchResult.Loading -> {
                        // Already handled above
                    }
                }
            }
        }
    }

    private fun handleCachedChartsResult(result: FetchResult<List<Chart>>) {
        when (result) {
            is FetchResult.Success -> {
                if (result.data.isNotEmpty()) {
                    chartManager.updateState(ChartState.Success)
                }
            }
            is FetchResult.Error -> {
                chartManager.updateState(ChartState.Error)
            }
            FetchResult.Loading -> {
                // No-op, we're already in Loading state
            }
        }
    }
}