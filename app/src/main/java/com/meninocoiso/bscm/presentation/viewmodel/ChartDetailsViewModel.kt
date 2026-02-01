package com.meninocoiso.bscm.presentation.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.data.manager.ChartManager
import com.meninocoiso.bscm.domain.model.Chart
import com.meninocoiso.bscm.domain.result.ContentResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "ChartDetailsViewModel"

// Sealed class representing the state of chart data
sealed class DetailsState {
    data object Loading : DetailsState()
    data class Success(val chart: Chart) : DetailsState()
    data class Error(val message: String?) : DetailsState()
}

@HiltViewModel
class ChartDetailsViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val chartManager: ChartManager
) : ViewModel() {
    private val _chart = MutableStateFlow<DetailsState>(DetailsState.Loading)
    val chart: StateFlow<DetailsState> = _chart.asStateFlow()

    fun fetchChartById(chartId: String?) {
        if (chartId.isNullOrEmpty()) {
            _chart.value = DetailsState.Error(context.getString(R.string.invalid_chart_id))
            return
        }

        viewModelScope.launch {
            _chart.value = DetailsState.Loading

            try {
                val result = chartManager.getChart(chartId).first()
                when (result) {
                    is ContentResult.Success -> {
                        Log.d(TAG, "Chart data loaded successfully")
                        _chart.value = DetailsState.Success(result.data)
                    }
                    is ContentResult.Error -> {
                        Log.e(TAG, "Error fetching chart: ${result.message}", result.cause)
                        _chart.value = DetailsState.Error(result.message)
                    }
                    is ContentResult.Loading -> {
                        // Keep loading state
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching data", e)
                _chart.value = DetailsState.Error(e.message)
            }
        }
    }
}