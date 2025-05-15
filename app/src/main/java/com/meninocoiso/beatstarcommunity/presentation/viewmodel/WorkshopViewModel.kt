package com.meninocoiso.beatstarcommunity.presentation.viewmodel

import android.util.Log
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
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
import com.meninocoiso.beatstarcommunity.data.repository.SettingsRepository
import com.meninocoiso.beatstarcommunity.domain.enums.Difficulty
import com.meninocoiso.beatstarcommunity.domain.enums.Genre
import com.meninocoiso.beatstarcommunity.domain.enums.SortOption
import com.meninocoiso.beatstarcommunity.domain.model.Chart
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "WorkshopViewModel"
private const val BATCH_SIZE = 10
private const val MAX_HISTORY_SIZE = 5

@HiltViewModel
class WorkshopViewModel @Inject constructor(
    private val chartManager: ChartManager,
    private val cacheRepository: CacheRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    val isExplicitAllowed: Flow<Boolean> = settingsRepository.settingsFlow
        .map { it.allowExplicitContent }
    
    val feedCharts: Flow<List<Chart>> = chartManager.workshopCharts
    val searchCharts: Flow<List<Chart>> = chartManager.searchCharts
    
    private val _workshopState = MutableStateFlow<ChartState>(ChartState.Loading)
    val workshopState: SharedFlow<ChartState> = _workshopState.asStateFlow()
    
    private val _events = MutableSharedFlow<FetchEvent>()
    val events: SharedFlow<FetchEvent> = _events.asSharedFlow()
    
    // Pagination
    val listState = LazyListState()
    
    private var currentFeedPage = 0
    private var currentSearchPage = 0
    private var hasMoreData = true
    
    var isLoadingMore by mutableStateOf(false)
        private set
    
    // Search bar history and suggestions
    val searchFieldState = TextFieldState()

    var searchHistory: List<String> by mutableStateOf(emptyList())
        private set
    
    var suggestions by mutableStateOf<List<String>?>(null)         
        private set
    
    // Sorting and filtering
    var currentSortOption by mutableStateOf(SortOption.WEEKLY_RANK)
        private set

    var difficulties by mutableStateOf<List<Difficulty>?>(null)
        private set

    var genres by mutableStateOf<List<Genre>?>(null)
        private set
    
    init {
        // Load search history
        getSearchHistory()
        
        // Initialize by loading cached charts, then fetch fresh data
        viewModelScope.launch {
            val cachedResult = chartManager.loadCachedCharts()
            handleCachedChartsResult(cachedResult)

            fetchFeedCharts(false)

            // Observe scroll state for pagination
            // Must be done sequentially after loading cached charts
            observeScrollState()
        }
        
        // Observe suggestions
        viewModelScope.launch {
            observeSuggestions()
        }
    }

    /**
     * Fetches the feed charts from the remote source.
     *
     * @param showLoading Whether to show loading indicators (either full screen or in PullToRefresh).
     */
    fun fetchFeedCharts(showLoading: Boolean = true) {
        viewModelScope.launch {
            // Reset pagination
            currentFeedPage = 0
            isLoadingMore = false
            hasMoreData = true
            
            if (showLoading) {
                _workshopState.value = ChartState.Loading
            }

            chartManager.fetchFeedCharts(
                sortBy = currentSortOption,
                forceRefresh = true,
                limit = BATCH_SIZE,
                offset = 0
            ).collect { result ->
                when (result) {
                    is FetchResult.Success -> {
                        hasMoreData = result.data.size >= BATCH_SIZE
                        _workshopState.value = ChartState.Success
                    }
                    is FetchResult.Error -> {
                        if (showLoading && chartManager.getChartsLength() > 0) {
                            _events.emit(FetchEvent.Error(result.message))
                        }
                        _workshopState.value = ChartState.Error
                    }
                    FetchResult.Loading -> {
                        // Already handled above
                    }
                }
            }
        }
    }

    /**
     * Searches for charts based on the provided query.
     *
     * @param query The search query string.
     */
    fun searchCharts(query: String) {
        // val query = searchFieldState.text.toString()
        viewModelScope.launch {
            // If the query is empty, clear the charts and reset state to show the feed
            if (query.isEmpty()) {
                Log.d(TAG, "Clearing search results")
                clearSearch()
                return@launch
            }

            Log.d(TAG, "Searching for charts with query: $query")
            
            // Update the current search query to prevent data racing
            chartManager.searchQuery = query
            
            // Show loading indicator
            _workshopState.value = ChartState.Loading

            // Reset pagination
            currentSearchPage = 0
            isLoadingMore = false
            hasMoreData = true
            
            // Scroll to top
            viewModelScope.launch {
                listState.scrollToItem(0)
            }

            // Add search to history
            addSearchHistory(query)

            chartManager.searchCharts(
                query = query,
                difficulties = difficulties,
                genres = genres,
                limit = BATCH_SIZE,
                offset = 0
            ).collect { result ->
                when (result) {
                    is FetchResult.Success -> {
                        hasMoreData = result.data.size >= BATCH_SIZE
                        _workshopState.value = ChartState.Success
                    }
                    is FetchResult.Error -> {
                        _workshopState.value = ChartState.Error
                    }
                    FetchResult.Loading -> {
                        // No-op
                    }
                }
            }
        }
    }

    /**
     * Changes the current sort option for the charts.
     *
     * @param sortOption The new sort option to be applied.
     */
    fun changeSortOption(sortOption: SortOption) {
        if (currentSortOption != sortOption) {
            currentSortOption = sortOption
            fetchFeedCharts(true)
        }
    }

    /**
     * Sets the list of difficulties to be used for filtering charts.
     *
     * @param newDifficulties The new list of difficulties to be set.
     */
    /*fun setDifficulties(newDifficulties: List<Difficulty>?) {
        difficulties = newDifficulties
    }*/

    /*fun setGenres(newGenres: List<String>?) {
        genres = newGenres
    }*/

    @OptIn(FlowPreview::class)
    suspend fun observeSuggestions() {
        var lastQuery = ""
        Log.d(TAG, "Observing suggestions for search field")
        
        snapshotFlow { searchFieldState.text }
            // Let fast typers get multiple keystrokes in before kicking off a search
            .debounce(300)
            // collectLatest cancels the previous search if it's still running 
            // when there's a new change.             
            .collectLatest { currentQuery ->
                // Only process if query has meaningfully changed
                if (currentQuery.toString() != lastQuery) {
                    lastQuery = currentQuery.toString()

                    suggestions = when {
                        lastQuery.length > 1 -> {
                            chartManager.getSuggestions(currentQuery.toString()).first()
                        }
                        else -> {
                            // Clear suggestions for very short queries
                            null
                        }
                    }
                }
            }
    }
    
    suspend fun observeScrollState() {
        Log.d(TAG, "Observing scroll state for list")
        
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index
            
            // Ignore if no items are visible
            if (lastVisibleItem == null) {
                return@snapshotFlow false
            }

            lastVisibleItem >= totalItems - 3 // Load more when 3 items from end
        }
            .distinctUntilChanged()
            .collect { isAtEnd ->
                // Check if we are at the end of the list 
                // and if there's data already loaded
                if (isAtEnd && _workshopState.value is ChartState.Success) {
                    loadMoreCharts()
                }
            }
    }
    
    /**
     * Load the next batch of charts when user scrolls to the bottom
     */
    fun loadMoreCharts() {
        if (isLoadingMore || !hasMoreData) {
            Log.d(TAG, "Skipping load more: already loading isLoadingMore=$isLoadingMore or no more data hasMoreData=$hasMoreData")
            return
        }

        viewModelScope.launch {
            Log.d(TAG, "Loading more charts...")
            isLoadingMore = true

            // Determine which loading function to use based on context
            val flowToCollect = if (searchFieldState.text.isEmpty()) {
                currentFeedPage++
                
                // We're in feed mode
                chartManager.fetchFeedCharts(
                    sortBy = currentSortOption,
                    limit = BATCH_SIZE,
                    offset = currentFeedPage * BATCH_SIZE
                )
            } else {
                currentSearchPage++
                
                // We're on query mode
                chartManager.searchCharts(
                    query = searchFieldState.text.toString(),
                    difficulties = difficulties,
                    genres = genres,
                    limit = BATCH_SIZE,
                    offset = currentSearchPage * BATCH_SIZE
                )
            }

            flowToCollect.collect { result ->
                when (result) {
                    is FetchResult.Success -> {
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
                chartManager.updateState(ChartState.Success)
            }
            is FetchResult.Error -> {
                chartManager.updateState(ChartState.Error)
            }
            FetchResult.Loading -> {
                // No-op, we're already in Loading cacheState
            }
        }
    }
    
    fun clearSearch() {
        // Clear search
        searchFieldState.setTextAndPlaceCursorAtEnd("")
        chartManager.searchQuery = ""
        
        // Reset pagination
        currentSearchPage = 0
        isLoadingMore = false
        hasMoreData = true
        
        // Reset suggestions
        suggestions = null
        
        // Scroll to top
        viewModelScope.launch {
            listState.scrollToItem(0)
        }
    }
    
    // Search history management
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