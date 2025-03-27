package com.meninocoiso.beatstarcommunity.presentation.viewmodel

import android.util.Log
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meninocoiso.beatstarcommunity.data.manager.ChartManager
import com.meninocoiso.beatstarcommunity.data.manager.ChartState
import com.meninocoiso.beatstarcommunity.data.manager.FetchEvent
import com.meninocoiso.beatstarcommunity.data.manager.FetchResult
import com.meninocoiso.beatstarcommunity.data.repository.CacheRepository
import com.meninocoiso.beatstarcommunity.domain.model.Chart
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
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
    
    var isLoadingMore by mutableStateOf(false)
        private set
    
    var searchHistory: List<String> by mutableStateOf(emptyList())
        private set
    
    var suggestions by mutableStateOf<List<String>?>(emptyList<String>())         
        private set

    val searchFieldState = TextFieldState()
    var searchResults: List<String> by mutableStateOf(emptyList())         
        private set
    
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
    
    @OptIn(FlowPreview::class)
    suspend fun observeSuggestions() {
        snapshotFlow { searchFieldState.text }
            // Let fast typers get multiple keystrokes in before kicking off a search.
            .debounce(500)
            // collectLatest cancels the previous search if it's still running 
            // when there's a new change.             
            .collectLatest { query ->
                /*TODO: Currently, if users type something and then delete it fast,
                *  when the remote call returns, it will overwrite the suggestions,
                *  even though the query is empty. This is a minor issue, but could
                *  be improved in the future.
                *  Some ideas:
                * - Keep track of the last query and only update suggestions if the
                *  new query is not empty and different from the last one.
                * - Cancel the remote call if the query is empty.
                 */
                if (query.length > 2) {
                    getSuggestions(query.toString())
                } else {
                    suggestions = null
                }
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
            isLoadingMore = false
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
        if (isLoadingMore || !hasMoreData) {
            Log.d(TAG, "Skipping load more: already loading or no more data")
            return
        }

        viewModelScope.launch {
            Log.d(TAG, "Loading more charts...")
            isLoadingMore = true
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
                        isLoadingMore = false
                    }
                    is FetchResult.Error -> {
                        _events.emit(FetchEvent.Error(result.message))
                        isLoadingMore = false
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
    
    fun getSuggestions(query: String) {
        viewModelScope.launch {
            chartManager.getSuggestions(query).collect { result ->
                when (result) {
                    is FetchResult.Success -> {
                        Log.d(TAG, "Got suggestions: ${result.data} for the query $query")
                        suggestions = result.data
                    }
                    is FetchResult.Error -> {
                        // No-op
                    }
                    FetchResult.Loading -> {
                        // No-op
                    }
                }
            }
        }
    }

    /*fun searchCharts(query: String) {
        viewModelScope.launch {
            chartManager.searchCharts(query).collect { result ->
                when (result) {
                    is FetchResult.Success -> {
                        chartManager.updateState(ChartState.Success)
                    }
                    is FetchResult.Error -> {
                        chartManager.updateState(ChartState.Error)
                    }
                    FetchResult.Loading -> {
                        // No-op
                    }
                }
            }
        }
    }*/
    
    private fun getSearchHistory() {
        viewModelScope.launch {
            searchHistory = cacheRepository.getSearchHistory()
        }
    }

    fun addSearchHistory(search: String) {
        viewModelScope.launch {
            if (searchHistory.contains(search)) {
                return@launch
            }

            // Add search to history if below max size, otherwise replace oldest item
            if (searchHistory.size < MAX_HISTORY_SIZE) {
                searchHistory = searchHistory.toMutableList().apply { add(search) }
                cacheRepository.setSearchHistory(searchHistory)
            } else {
                searchHistory = searchHistory.toMutableList().apply {
                    removeAt(0)
                    add(search)
                }
                cacheRepository.setSearchHistory(searchHistory)
            }
        }
    }
    
    fun removeSearchHistory(search: String) {
        viewModelScope.launch {
            searchHistory = searchHistory.toMutableList().apply { remove(search) }
            cacheRepository.setSearchHistory(searchHistory)
        }
    }
}