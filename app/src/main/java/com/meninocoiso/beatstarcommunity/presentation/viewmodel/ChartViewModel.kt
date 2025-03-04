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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

private const val TAG = "ChartViewModel"

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

    // Public function to refresh data so external components can trigger a re-fetch.
    fun refresh() = fetchCharts()

    private fun fetchCharts() {
        viewModelScope.launch {
            _charts.value = ChartsState.Loading
            try {
                // Fetch local data first
                val localResult = localChartRepository.getCharts().first()
                Log.d(TAG, "Local data found on cache")

                if (localResult.isSuccess && localResult.getOrNull()?.isNotEmpty() == true) {
                    _charts.value = ChartsState.Success(localResult.getOrThrow())
                } else {
                    // If no local data, attempt to fetch from remote repository
                    val remoteResult = remoteChartRepository.getCharts().first()

                    Log.d(TAG, "No local data found on cache")
                    Log.d(TAG, "Remote data fetched")

                    if (remoteResult.isSuccess) {
                        val chartsList = remoteResult.getOrThrow()
                        _charts.value = ChartsState.Success(chartsList)

                        // Save the remote data to local storage
                        localChartRepository.insertCharts(chartsList)
                            .onEach {
                                Log.d(TAG, "Local data updated")
                            }
                            .catch {
                                Log.e(TAG, "Error updating local data", it)
                            }
                            .collect()
                    } else {
                        val error = remoteResult.exceptionOrNull()
                        Log.e(TAG, "Error fetching remote data", error)

                        _charts.value = ChartsState.Error(error?.message)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching data", e)
                _charts.value = ChartsState.Error(e.message)
            }
        }
    }
}