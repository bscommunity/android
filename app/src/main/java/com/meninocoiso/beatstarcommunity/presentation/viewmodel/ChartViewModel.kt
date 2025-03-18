package com.meninocoiso.beatstarcommunity.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meninocoiso.beatstarcommunity.data.repository.ChartRepository
import com.meninocoiso.beatstarcommunity.domain.model.Chart
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

private const val TAG = "ChartViewModel"

// Sealed class representing the state of chart data
sealed class ChartsState {
    data class Loading(val previousCharts: List<Chart>? = null) : ChartsState()
    data class Success(val charts: List<Chart>) : ChartsState()
    data class Error(val message: String?, val previousCharts: List<Chart>? = null) : ChartsState()
}

@HiltViewModel
class ChartViewModel @Inject constructor(
    @Named("Remote") private val remoteChartRepository: ChartRepository,
    @Named("Local") private val localChartRepository: ChartRepository
) : ViewModel() {
    private val _charts = MutableStateFlow<ChartsState>(ChartsState.Loading())
    val charts: StateFlow<ChartsState> = _charts.asStateFlow()

    // Automatically fetch charts on initialization
    init {
        fetchCharts()
    }

    fun refresh() = fetchCharts(true)

    private fun fetchCharts(refresh: Boolean? = false) {
        // Get previous charts if they exist
        val previousCharts = when (val currentState = _charts.value) {
            is ChartsState.Success -> currentState.charts
            is ChartsState.Loading -> currentState.previousCharts
            is ChartsState.Error -> currentState.previousCharts
        }

        // Set loading state but keep previous data
        _charts.value = ChartsState.Loading(previousCharts)

        Log.d(TAG, "Fetching data with refresh: $refresh and state ${_charts.value}")

        viewModelScope.launch {
            try {
                // Fetch local data first
                val localResult = localChartRepository.getCharts().first()

                if (refresh == false) {
                    if (localResult.isSuccess && localResult.getOrNull()?.isNotEmpty() == true) {
                        Log.d(TAG, "Local data fetched")

                        _charts.value = ChartsState.Success(localResult.getOrThrow())

                        return@launch
                    }
                }

                // If no local data, attempt to fetch from remote repository
                val remoteResult = remoteChartRepository.getCharts().first()

                val chartsList = remoteResult.getOrThrow()
                Log.d(TAG, "Remote data fetched")

                // Keep the isInstalled flag from local data
                localResult.getOrNull()?.forEach { localChart ->
                    chartsList.find {
                        it.id == localChart.id
                    }?.isInstalled = localChart.isInstalled
                }

                _charts.value = ChartsState.Success(chartsList)
                Log.d(TAG, "Remote data used: $chartsList")

                // Save the remote data to local storage
                localChartRepository.insertCharts(chartsList).first()
                    .onSuccess {
                        Log.d(TAG, "Local data updated: $chartsList")
                    }
                    .onFailure {
                        Log.e(TAG, "Error updating local data", it)
                        _charts.value = ChartsState.Error(it.message, previousCharts)
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching data", e)
                _charts.value = ChartsState.Error(e.message, previousCharts)
            }
        }
    }
}