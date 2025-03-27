package com.meninocoiso.beatstarcommunity.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meninocoiso.beatstarcommunity.data.manager.ChartManager
import com.meninocoiso.beatstarcommunity.data.manager.ChartState
import com.meninocoiso.beatstarcommunity.data.manager.FetchEvent
import com.meninocoiso.beatstarcommunity.data.manager.FetchResult
import com.meninocoiso.beatstarcommunity.data.repository.CacheRepository
import com.meninocoiso.beatstarcommunity.domain.model.Chart
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "WorkshopViewModel"
private const val BATCH_SIZE = 10
private const val MAX_HISTORY_SIZE = 10

@HiltViewModel
class WorkshopViewModel @Inject constructor(
    private val chartManager: ChartManager,
    private val cacheRepository: CacheRepository
) : ViewModel() {

    val charts: Flow<List<Chart>> = chartManager.workshopCharts
    val state = chartManager.workshopState

    private val _events = MutableSharedFlow<FetchEvent>()
    val events: SharedFlow<FetchEvent> = _events.asSharedFlow()

    private var currentPage = 0
    private var hasMoreData = true

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _searchHistory = MutableStateFlow<List<String>>(emptyList())
    val searchHistory: StateFlow<List<String>> = _searchHistory.asStateFlow()
    
    init {
        viewModelScope.launch {
            // Load search history
            getSearchHistory()
            
            // Initialize by loading cached charts, then fetch fresh data
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
            // Reset pagination state
            Log.d(TAG, "Refreshing charts...")
            
            currentPage = 0
            _isLoadingMore.value = false
            hasMoreData = true
            
            chartManager.updateState(ChartState.Loading)

            // Fetch first batch of data
            chartManager.refreshRemoteCharts(
                forceRefresh = true,
                limit = BATCH_SIZE,
                offset = 0
            ).collect { result ->
                when (result) {
                    is FetchResult.Success -> {
                        // Check if we received fewer items than requested (end of data)
                        hasMoreData = result.data.size >= BATCH_SIZE
                        chartManager.updateState(ChartState.Success)
                    }

                    is FetchResult.Error -> {
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

    /**
     * Load the next batch of charts when user scrolls to the bottom
     */
    fun loadMoreCharts() {
        if (_isLoadingMore.value || !hasMoreData) {
            Log.d(TAG, "Skipping load more: already loading or no more data")
            return
        }

        viewModelScope.launch {
            Log.d(TAG, "Loading more charts...")
            _isLoadingMore.value = true
            currentPage++

            chartManager.loadMoreCharts(
                limit = BATCH_SIZE,
                offset = currentPage * BATCH_SIZE
            ).collect { result ->
                when (result) {
                    is FetchResult.Success -> {
                        // Check if we're at the end of available data
                        if (result.data.isEmpty() || result.data.size < BATCH_SIZE) {
                            hasMoreData = false
                        }
                        _isLoadingMore.value = false
                    }
                    is FetchResult.Error -> {
                        _events.emit(FetchEvent.Error(result.message))
                        _isLoadingMore.value = false
                    }
                    FetchResult.Loading -> {
                        // No-op
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

    private fun getSearchHistory() {
        viewModelScope.launch {
            _searchHistory.value = cacheRepository.getSearchHistory()
        }
    }

    fun addSearchHistory(search: String) {
        viewModelScope.launch {
            if (_searchHistory.value.contains(search)) {
                return@launch
            }

            // Add search to history if below max size, otherwise replace oldest item
            if (_searchHistory.value.size < MAX_HISTORY_SIZE) {
                _searchHistory.value = _searchHistory.value.toMutableList().apply { add(search) }
                cacheRepository.setSearchHistory(_searchHistory.value)
            } else {
                _searchHistory.value = _searchHistory.value.toMutableList().apply {
                    removeAt(0)
                    add(search)
                }
                cacheRepository.setSearchHistory(_searchHistory.value)
            }
        }
    }
    
    fun removeSearchHistory(search: String) {
        viewModelScope.launch {
            _searchHistory.value = _searchHistory.value.toMutableList().apply { remove(search) }
            cacheRepository.setSearchHistory(_searchHistory.value)
        }
    }
}