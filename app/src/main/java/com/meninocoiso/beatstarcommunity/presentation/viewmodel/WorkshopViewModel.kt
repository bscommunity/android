package com.meninocoiso.beatstarcommunity.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meninocoiso.beatstarcommunity.data.manager.ChartManager
import com.meninocoiso.beatstarcommunity.data.manager.ChartResult
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

sealed class ChartsState {
    data class Success(val charts: List<Chart>) : ChartsState()
    data class Loading(val charts: List<Chart> = emptyList()) : ChartsState()
    data class Error(val charts: List<Chart> = emptyList(), val message: String) : ChartsState()
}

@HiltViewModel
class WorkshopViewModel @Inject constructor(
    private val chartManager: ChartManager
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val charts = chartManager.workshopCharts

    private val _events = MutableSharedFlow<FetchEvent>()
    val events: SharedFlow<FetchEvent> = _events.asSharedFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                chartManager.loadCachedCharts()

                chartManager.refreshRemoteCharts(forceRefresh = true)
                    .collect { result ->
                        when (result) {
                            is ChartResult.Loading -> _isLoading.value = true
                            is ChartResult.Success -> {
                                _isLoading.value = false
                                _error.value = null
                            }
                            is ChartResult.Error -> {
                                _isLoading.value = false
                                _error.value = result.message
                                _events.emit(FetchEvent.Error(result.message, true))
                            }
                        }
                    }
            } catch (e: Exception) {
                val errorMessage = "Failed to refresh charts: ${e.message}"
                _isLoading.value = false
                _error.value = errorMessage
                _events.emit(FetchEvent.Error(errorMessage, true))
            }
        }
    }
}