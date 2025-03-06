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

private const val TAG = "ChartDetailsViewModel"

// Sealed class representing the state of chart data
sealed class DetailsState {
    data object Loading : DetailsState()
    data class Success(val chart: Chart) : DetailsState()
    data class Error(val message: String?) : DetailsState()
}

@HiltViewModel
class ChartDetailsViewModel @Inject constructor(
    @Named("Remote") private val remoteChartRepository: ChartRepository,
    @Named("Local") private val localChartRepository: ChartRepository
) : ViewModel() {
    private val _chart = MutableStateFlow<DetailsState>(DetailsState.Loading)
    val chart: StateFlow<DetailsState> = _chart.asStateFlow()

    fun fetchChartById(chartId: String?) {
        if (chartId.isNullOrEmpty()) {
            _chart.value = DetailsState.Error("Invalid chart ID")
            return
        }

        viewModelScope.launch {
            _chart.value = DetailsState.Loading

            try {
                // Fetch local data first
                val localResult = localChartRepository.getChart(chartId).first()

                if (localResult.isSuccess && localResult.getOrNull() != null) {
                    Log.d(TAG, "Local data used")

                    _chart.value = DetailsState.Success(localResult.getOrThrow())
                    return@launch
                }

                // If no local data, attempt to fetch from remote repository
                val remoteResult = remoteChartRepository.getChart(chartId).first()
                Log.d(TAG, "Remote data fetched")

                if (remoteResult.isSuccess) {
                    val remoteChart = remoteResult.getOrThrow()

                    _chart.value = DetailsState.Success(remoteChart)

                    // Save the remote data to local storage
                    localChartRepository.insertCharts(listOf(remoteChart))
                        .onEach {
                            Log.d(TAG, "Local data updated: $it")
                        }
                        .catch {
                            Log.e(TAG, "Error updating local data", it)
                        }
                        .collect()
                } else {
                    val error = remoteResult.exceptionOrNull()
                    _chart.value = DetailsState.Error(error?.message)

                    Log.e(TAG, "Error fetching remote data", error)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching data", e)
                _chart.value = DetailsState.Error(e.message)
            }
        }
    }
}