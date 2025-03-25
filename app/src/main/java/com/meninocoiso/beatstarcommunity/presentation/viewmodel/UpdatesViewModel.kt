package com.meninocoiso.beatstarcommunity.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meninocoiso.beatstarcommunity.data.manager.ChartManager
import com.meninocoiso.beatstarcommunity.data.manager.ChartResult
import com.meninocoiso.beatstarcommunity.data.manager.FetchEvent
import com.meninocoiso.beatstarcommunity.domain.model.Chart
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "UpdatesViewModel"

@HiltViewModel
class UpdatesViewModel @Inject constructor(
    private val chartManager: ChartManager
) : ViewModel() {

    // Directly expose the flows from ChartManager
    val updatesAvailable: Flow<List<Chart>> = chartManager.chartsWithUpdates
    val localCharts: Flow<List<Chart>> = chartManager.installedCharts

    private val _events = MutableSharedFlow<FetchEvent>()
    val events: SharedFlow<FetchEvent> = _events.asSharedFlow()

    init {
        // Initialize by checking for updates when the ViewModel is created
        checkForUpdates(false)
    }

    /**
     * Check for updates to installed charts
     * @param showLoading Whether to emit loading events (not used in this implementation)
     */
    fun checkForUpdates(showLoading: Boolean = true) {
        viewModelScope.launch {
            // Check for updates
            chartManager.checkForUpdates().collect { result ->
                when (result) {
                    is ChartResult.Success -> {
                        // No need to update a flow - ChartManager will update its internal flow
                        // which we're directly exposing
                    }
                    is ChartResult.Error -> {
                        // Emit an event for the UI to show an error message
                        _events.emit(FetchEvent.Error(result.message))
                    }
                    ChartResult.Loading -> {
                        // No specific action needed
                    }
                }
            }
        }
    }

    /**
     * Refresh all data
     */
    fun refresh() {
        viewModelScope.launch {
            try {
                // This will update the internal state of the manager
                chartManager.loadCachedCharts()

                // Then check for updates
                checkForUpdates(true)
            } catch (e: Exception) {
                _events.emit(FetchEvent.Error("Failed to refresh data: ${e.message}"))
            }
        }
    }
}