package com.meninocoiso.bscm.presentation.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.data.manager.ChartManager
import com.meninocoiso.bscm.domain.model.Chart
import com.meninocoiso.bscm.domain.result.ContentResult
import com.meninocoiso.bscm.domain.result.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "ChartDetailsViewModel"

@HiltViewModel
class ChartDetailsViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val chartManager: ChartManager
) : ViewModel() {
    private val _chart = MutableStateFlow<ContentResult<Chart>>(ContentResult.Loading)
    val chart: StateFlow<ContentResult<Chart>> = _chart.asStateFlow()

    fun fetchChartById(contentId: String?) {
        if (contentId.isNullOrEmpty()) {
            _chart.value = ContentResult.Error(UiText.Res(R.string.invalid_chart_id))
            return
        }

        viewModelScope.launch {
            _chart.value = ContentResult.Loading

            try {
                chartManager.getChartByContentId(contentId).collect { result ->
                    when (result) {
                        is ContentResult.Success -> {
                            Log.d(TAG, "Chart data loaded successfully")
                            _chart.value = ContentResult.Success(result.data)
                        }

                        is ContentResult.Error -> {
                            val msg = result.message
                            Log.e(TAG, "Error fetching chart: $msg", result.cause)
                            _chart.value = ContentResult.Error(msg)
                        }

                        is ContentResult.Loading -> {
                            // Keep loading state
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching data", e)
                _chart.value = ContentResult.Error(
                    UiText.Plain(e.message ?: context.getString(R.string.unknown_error)),
                    e
                )
            }
        }
    }
}