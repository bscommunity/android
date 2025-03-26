package com.meninocoiso.beatstarcommunity.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meninocoiso.beatstarcommunity.data.manager.ChartManager
import com.meninocoiso.beatstarcommunity.data.manager.ChartState
import com.meninocoiso.beatstarcommunity.data.manager.FetchResult
import com.meninocoiso.beatstarcommunity.domain.model.Chart
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "UpdatesViewModel"

@HiltViewModel
class UpdatesViewModel @Inject constructor(
    private val chartManager: ChartManager
) : ViewModel() {
    val updatesAvailable: Flow<List<Chart>> = chartManager.chartsWithUpdates
    val localCharts: Flow<List<Chart>> = chartManager.installedCharts

    val workshopState = chartManager.workshopState

    private val _updateState = MutableStateFlow<ChartState>(ChartState.Loading)
    val updateState: StateFlow<ChartState> = _updateState.asStateFlow()

    init {
        viewModelScope.launch {
            checkForUpdates(false)
        }
    }

    /**
     * Check for updates to installed charts
     * @param showLoading Whether to show loading state or keep showing existing data
     */
    fun checkForUpdates(showLoading: Boolean = true) {
        viewModelScope.launch {
            // If we want to show loading state, update the UI
            if (showLoading) {
                _updateState.value = ChartState.Loading
            }

            // Check for updates
            chartManager.checkForUpdates().collect { result ->
                when (result) {
                    is FetchResult.Success -> {
                        _updateState.value = ChartState.Success
                    }
                    is FetchResult.Error -> {
                        _updateState.value = ChartState.Error
                    }
                    FetchResult.Loading -> {
                        // Already handled above if showLoading is true
                    }
                }
            }
        }
    }
}