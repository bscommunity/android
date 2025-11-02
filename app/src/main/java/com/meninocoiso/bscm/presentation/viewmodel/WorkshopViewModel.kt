package com.meninocoiso.bscm.presentation.viewmodel

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
import com.meninocoiso.bscm.data.manager.ChartManager
import com.meninocoiso.bscm.data.manager.ChartState
import com.meninocoiso.bscm.data.manager.FetchEvent
import com.meninocoiso.bscm.data.manager.FetchResult
import com.meninocoiso.bscm.data.repository.CacheRepository
import com.meninocoiso.bscm.data.repository.SettingsRepository
import com.meninocoiso.bscm.domain.enums.Difficulty
import com.meninocoiso.bscm.domain.enums.Genre
import com.meninocoiso.bscm.domain.enums.SortOption
import com.meninocoiso.bscm.domain.model.Chart
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
import kotlin.coroutines.cancellation.CancellationException

private const val TAG = "WorkshopViewModel"
private const val BATCH_SIZE = 10
private const val MAX_HISTORY_SIZE = 5
private const val SUGGESTION_DEBOUNCE_MILLIS = 600L

@HiltViewModel
class WorkshopViewModel @Inject constructor(
    private val chartManager: ChartManager,
    private val cacheRepository: CacheRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {
    val isExplicitAllowed: Flow<Boolean> = settingsRepository.settingsFlow
        .map { it.allowExplicitContent }

    // Updated to use the new ChartManager flows
    val feedCharts: Flow<List<Chart>> = chartManager.memoryCharts
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
    var currentSortOption by mutableStateOf<SortOption>(SortOption.LAST_UPDATED)
        private set

    var difficulties by mutableStateOf<List<Difficulty>?>(null)
        private set

    var genres by mutableStateOf<List<Genre>?>(null)
        private set

    // Track current search query to know when we're in search mode
    private var currentSearchQuery by mutableStateOf("")

    init {
        // Load search history
        getSearchHistory()

        // Observe ChartManager state
        viewModelScope.launch {
            chartManager.cacheState.collect { state ->
                _workshopState.value = state
            }
        }

        // Initialize by loading cached charts, then fetch fresh data
        viewModelScope.launch {
            currentSortOption = cacheRepository.getLatestWorkshopSort() ?: SortOption.LAST_UPDATED

            // Load cached charts first
            val rootUri = cacheRepository.getFolderUri()
            chartManager.loadCachedCharts(currentSortOption, rootUri)

            // Then fetch fresh data
            fetchFeedCharts(true)

            // Observe scroll state for pagination
            observeScrollState()
        }

        // Observe suggestions
        viewModelScope.launch {
            observeSuggestions()
        }
    }

    /**
     * Fetches the feed charts from the remote source.
     */
    fun fetchFeedCharts(showLoading: Boolean = true) {
        viewModelScope.launch {
            // Reset pagination
            currentFeedPage = 0
            isLoadingMore = false
            hasMoreData = true

            if (showLoading) {
                chartManager.updateState(ChartState.Loading)
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
     * Searches for charts based on the provided query.
     */
    fun searchCharts(query: String) {
        viewModelScope.launch {
            currentSearchQuery = query

            // If the query is empty, clear the search and show feed
            if (query.isEmpty()) {
                Log.d(TAG, "Clearing search results")
                clearSearch()
                return@launch
            }

            Log.d(TAG, "Searching for charts with query: $query")

            // Show loading indicator
            chartManager.updateState(ChartState.Loading)

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
                        chartManager.updateState(ChartState.Success)
                    }
                    is FetchResult.Error -> {
                        chartManager.updateState(ChartState.Error)
                        _events.emit(FetchEvent.Error(result.message))
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
     */
    fun changeSortOption(sortOption: SortOption) {
        if (currentSortOption != sortOption) {
            currentSortOption = sortOption
            fetchFeedCharts(true)
            viewModelScope.launch {
                cacheRepository.setLatestWorkshopSort(sortOption.name)
            }
        }
    }

    @OptIn(FlowPreview::class)
    suspend fun observeSuggestions() {
        Log.d(TAG, "Observing suggestions for search field")

        snapshotFlow { searchFieldState.text.toString() }
            .distinctUntilChanged()
            .debounce(SUGGESTION_DEBOUNCE_MILLIS)
            .collectLatest { query ->
                suggestions = when {
                    query.length > 1 -> {
                        Log.d(TAG, "Fetching suggestions for query: $query")
                        try {
                            chartManager.getSuggestions(query).first()
                        } catch (e: CancellationException) {
                            Log.d(TAG, "Suggestion fetching cancelled for query: $query")
                            throw e // Re-throw to cancel the flow
                        } catch (e: Exception) {
                            Log.e(TAG, "Error fetching suggestions for query: $query", e)
                            null // Clean suggestions on error
                        }
                    }
                    else -> {
                        // Clean suggestions for very short or empty queries
                        null
                    }
                }
            }
    }

    suspend fun observeScrollState() {
        Log.d(TAG, "Observing scroll state for list")

        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItem =
                layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: return@snapshotFlow false

            // Ignore if no items are visible

            lastVisibleItem >= totalItems - 3 // Load more when 3 items from end
        }
            .distinctUntilChanged()
            .collect { isAtEnd ->
                // Check if we are at the end of the list 
                // and if there's data already loaded
                if (isAtEnd && chartManager.getChartsLength() > 0) {
                    Log.d(TAG, "User scrolled to bottom, loading more charts")
                    loadMoreCharts()
                }
            }
    }

    /**
     * Load the next batch of charts when user scrolls to the bottom
     */
    fun loadMoreCharts() {
        if (isLoadingMore || !hasMoreData) {
            Log.d(TAG, "Skipping load more: isLoadingMore=$isLoadingMore, hasMoreData=$hasMoreData")
            return
        }
        
        // If this is the first load and there's no data, skip loading more
        // This verification is already done in observeScrollState

        viewModelScope.launch {
            Log.d(TAG, "Loading more charts...")
            isLoadingMore = true

            // Determine which loading function to use based on current search state
            val flowToCollect = if (currentSearchQuery.isEmpty()) {
                currentFeedPage++

                // We're in feed mode
                chartManager.fetchFeedCharts(
                    sortBy = currentSortOption,
                    forceRefresh = false, // Don't force refresh for pagination
                    limit = BATCH_SIZE,
                    offset = currentFeedPage * BATCH_SIZE
                )
            } else {
                currentSearchPage++

                // We're in search mode
                chartManager.searchCharts(
                    query = currentSearchQuery,
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

    fun clearSearch() {
        // Clear search state
        searchFieldState.setTextAndPlaceCursorAtEnd("")
        currentSearchQuery = ""

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

        // Note: No need to manually clear charts since the UI will switch
        // to showing feedCharts flow instead of searchCharts flow
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
            val updatedHistory = if (searchHistory.size < MAX_HISTORY_SIZE) {
                searchHistory + search
            } else {
                searchHistory.drop(1) + search
            }

            searchHistory = updatedHistory
            cacheRepository.setSearchHistory(searchHistory)
        }
    }

    fun removeSearchHistory(search: String) {
        viewModelScope.launch {
            searchHistory = searchHistory.filter { it != search }
            cacheRepository.setSearchHistory(searchHistory)
        }
    }
}