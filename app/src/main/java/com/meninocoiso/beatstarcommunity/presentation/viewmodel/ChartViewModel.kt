package com.meninocoiso.beatstarcommunity.presentation.viewmodel

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

// Sealed class representing the state of chart data
sealed class ChartsState {
    data object Loading : ChartsState()
    data class Success(val charts: List<Chart>) : ChartsState()
    data class Error(val message: String?) : ChartsState()
}

@HiltViewModel
class ChartViewModel @Inject constructor(
    @Named("Remote") private val remoteChartRepository: ChartRepository,
    @Named("Local") private val localChartRepository: ChartRepository
) : ViewModel() {

    private val _charts = MutableStateFlow<ChartsState>(ChartsState.Loading)
    val charts: StateFlow<ChartsState> = _charts.asStateFlow()

    // Automatically fetch charts on initialization
    init {
        fetchCharts()
    }

    // Public function to refresh data so external components can trigger a refetch.
    fun refresh() = fetchCharts()

    private fun fetchCharts() {
        viewModelScope.launch {
            _charts.value = ChartsState.Loading
            try {
                // Fetch local data first
                val localResult = localChartRepository.getCharts().first()

                if (localResult.isSuccess && localResult.getOrNull()?.isNotEmpty() == true) {
                    println("Local data found: ${localResult.getOrThrow()}")
                    _charts.value = ChartsState.Success(localResult.getOrThrow())
                } else {
                    // If no local data, attempt to fetch from remote repository
                    println("No local data found")
                    val remoteResult = remoteChartRepository.getCharts().first()
                    if (remoteResult.isSuccess) {
                        val chartsList = remoteResult.getOrThrow()
                        _charts.value = ChartsState.Success(chartsList)
                        // Save the remote data to local storage
                        localChartRepository.insertCharts(chartsList)
                    } else {
                        val error = remoteResult.exceptionOrNull()

                        _charts.value = ChartsState.Error(error?.message)
                        /*if (error is IOException) {
                            _charts.value = ChartsState.Error("No internet connection")
                        } else {
                            _charts.value = ChartsState.Error(error?.message)
                        }*/
                    }
                }
            } catch (e: Exception) {
                _charts.value = ChartsState.Error(e.message)
            }
        }
    }
}