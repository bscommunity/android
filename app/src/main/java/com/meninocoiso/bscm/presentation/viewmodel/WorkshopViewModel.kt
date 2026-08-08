package com.meninocoiso.bscm.presentation.viewmodel

import android.content.Context
import android.util.Log
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meninocoiso.bscm.data.manager.ChartManager
import com.meninocoiso.bscm.data.manager.ThemeManager
import com.meninocoiso.bscm.data.manager.TourPassManager
import com.meninocoiso.bscm.data.repository.CacheRepository
import com.meninocoiso.bscm.data.repository.SettingsRepository
import com.meninocoiso.bscm.domain.enums.Difficulty
import com.meninocoiso.bscm.domain.enums.Genre
import com.meninocoiso.bscm.domain.enums.SortOption
import com.meninocoiso.bscm.domain.model.Chart
import com.meninocoiso.bscm.domain.model.Theme
import com.meninocoiso.bscm.domain.model.TourPass
import com.meninocoiso.bscm.domain.repository.ChartQuery
import com.meninocoiso.bscm.domain.result.ContentEvent
import com.meninocoiso.bscm.domain.result.ContentResult
import com.meninocoiso.bscm.domain.result.ContentState
import com.meninocoiso.bscm.util.StorageUtils
import com.meninocoiso.bscm.util.StorageUtils.BEATSTAR_URI
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
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
    private val tourPassManager: TourPassManager,
    private val themeManager: ThemeManager,
    private val cacheRepository: CacheRepository,
    private val settingsRepository: SettingsRepository,
    @param:ApplicationContext private val context: Context
) : ViewModel() {
    val isExplicitAllowed: Flow<Boolean> = settingsRepository.settingsFlow
        .map { it.allowExplicitContent }

    // Updated to use the new ChartManager flows
    val feedCharts: Flow<List<Chart>> = chartManager.feedCharts
    val searchCharts: Flow<List<Chart>> = chartManager.searchCharts

    // Tour pass flows (install status merged from the manager's live signal).
    val feedTourPasses: StateFlow<List<TourPass>> = tourPassManager.tourPassesUiState
    val searchTourPasses: StateFlow<List<TourPass>?> = tourPassManager.searchTourPassesUiState
    val tourPassState: StateFlow<ContentState> = tourPassManager.feedState

    // Theme flows
    val feedThemes: StateFlow<List<Theme>> = themeManager.themes
    val searchThemes: StateFlow<List<Theme>?> = themeManager.searchThemes
    val themeState: StateFlow<ContentState> = themeManager.feedState

    private val _workshopState = MutableStateFlow<ContentState>(ContentState.Loading)
    val workshopState: SharedFlow<ContentState> = _workshopState.asStateFlow()

    private val _events = MutableSharedFlow<ContentEvent>()
    val events: SharedFlow<ContentEvent> = _events.asSharedFlow()

    // Pagination
    val listState = LazyListState()
    val tourPassListState = LazyListState()
    val themeListState: LazyGridState = LazyGridState()

    private var currentFeedPage = 0
    private var currentSearchPage = 0
    private var hasMoreData = true

    var isLoadingMore by mutableStateOf(false)
        private set

    // Tour pass pagination
    private var currentTourPassFeedPage = 0
    private var currentTourPassSearchPage = 0
    private var hasMoreTourPasses = true

    var isLoadingMoreTourPasses by mutableStateOf(false)
        private set

    // Theme pagination
    private var currentThemeFeedPage = 0
    private var currentThemeSearchPage = 0
    private var hasMoreThemes = true

    var isLoadingMoreThemes by mutableStateOf(false)
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
    // Track previous auth state to detect transitions
    private var wasAuthenticated: Boolean? = null // null = not yet observed

    init {
        // Observe ChartManager feed state (separate from cache state used by updates)
        viewModelScope.launch {
            chartManager.feedState.collect { state ->
                _workshopState.value = state
            }
        }

        // Initialize by loading cached charts and local charts independently, then fetch fresh data
        val cacheLoadedJob = viewModelScope.launch {
            currentSortOption = cacheRepository.getLatestWorkshopSort() ?: SortOption.LAST_UPDATED

            // Set initial feed state to loading
            chartManager.updateFeedState(ContentState.Loading)

            // Load cached charts first (without searching for external charts yet)
            chartManager.loadCachedCharts(currentSortOption)
        }

        // Then fetch fresh data (waiting for the cache load so the feed can
        // short-circuit on cached items when possible)
        viewModelScope.launch {
            cacheLoadedJob.join()
            fetchFeedCharts(false) // Don't show loading again, we already set it above

            // Observe scroll state for pagination
            observeScrollState()
        }

        // Initialize tour pass feed from cache, then fetch fresh data
        viewModelScope.launch {
            tourPassManager.updateFeedState(ContentState.Loading)
            tourPassManager.loadCachedTourPasses()
            fetchTourPasses(false)
        }

        // Observe tour pass scroll state for pagination
        viewModelScope.launch {
            observeTourPassScrollState()
        }

        // Initialize theme feed from cache, then fetch fresh data
        viewModelScope.launch {
            themeManager.updateFeedState(ContentState.Loading)
            themeManager.loadCachedThemes()
            fetchThemes(false)
        }

        // Observe theme scroll state for pagination
        viewModelScope.launch {
            observeThemeScrollState()
        }

        // Load local/external charts independently if permission is available
        viewModelScope.launch {
            cacheLoadedJob.join()
            val rootUri = StorageUtils.getFolderUri(context, BEATSTAR_URI)
            if (rootUri != null) {
                chartManager.scanLocalCharts(rootUri)
            }
        }

        // Load search history
        getSearchHistory()

        // Observe suggestions
        viewModelScope.launch {
            observeSuggestions()
        }

        // Observe auth state changes to invalidate data on login
        viewModelScope.launch {
            cacheRepository.cacheFlow
                .map { it?.user != null }
                .distinctUntilChanged() // Only emit when auth state actually changes
                .collect { isNowAuthenticated ->
                    val previousState = wasAuthenticated
                    wasAuthenticated = isNowAuthenticated

                    // Only invalidate on LOGIN (false -> true), not on initial load or logout
                    if (previousState == false && isNowAuthenticated) {
                        Log.d(TAG, "User logged in, invalidating workshop data")
                        invalidateAndRefresh()
                    }

                    // On logout, we want to refresh to strip personal data
                    if (previousState == true && !isNowAuthenticated) {
                        Log.d(TAG, "User logged out, invalidating workshop data")
                        invalidateAndRefresh()
                    }
                }
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
                chartManager.updateFeedState(ContentState.Loading)
            }

            chartManager.fetchFeedCharts(
                sortBy = currentSortOption,
                forceRefresh = true,
                limit = BATCH_SIZE,
                offset = 0
            ).collect { result ->
                when (result) {
                    is ContentResult.Success -> {
                        hasMoreData = result.data.size >= BATCH_SIZE
                        chartManager.updateFeedState(ContentState.Success)
                        Log.d(TAG, "Fetched ${result.data.size} feed charts: ${result.data}")
                    }
                    is ContentResult.Error -> {
                        if (showLoading && chartManager.getChartsLength() > 0) {
                            _events.emit(ContentEvent.Error(result.message))
                        }
                        chartManager.updateFeedState(ContentState.Error)
                        Log.e(TAG, "Error fetching feed charts: ${result.message}")
                    }
                    ContentResult.Loading -> {
                        // Already handled above
                    }
                }
            }
        }
    }

    /**
     * Fetches the feed of tour passes from the remote source.
     */
    fun fetchTourPasses(showLoading: Boolean = true) {
        viewModelScope.launch {
            // Reset pagination
            currentTourPassFeedPage = 0
            isLoadingMoreTourPasses = false
            hasMoreTourPasses = true

            if (showLoading) {
                tourPassManager.updateFeedState(ContentState.Loading)
            }

            tourPassManager.fetchFeedTourPasses(
                forceRefresh = true,
                limit = BATCH_SIZE,
                offset = 0
            ).collect { result ->
                when (result) {
                    is ContentResult.Success -> {
                        hasMoreTourPasses = result.data.size >= BATCH_SIZE
                        tourPassManager.updateFeedState(ContentState.Success)
                        Log.d(TAG, "Fetched ${result.data.size} tour passes")
                    }
                    is ContentResult.Error -> {
                        tourPassManager.updateFeedState(ContentState.Error)
                        Log.e(TAG, "Error fetching tour passes: ${result.message}")
                    }
                    ContentResult.Loading -> {
                        // Already handled above
                    }
                }
            }
        }
    }

    /**
     * Searches for tour passes based on the provided query.
     */
    fun searchTourPasses(query: String) {
        viewModelScope.launch {
            // If the query is empty, clear the search and show feed
            if (query.isEmpty()) {
                clearTourPassSearch()
                return@launch
            }

            Log.d(TAG, "Searching for tour passes with query: $query")

            tourPassManager.updateFeedState(ContentState.Loading)

            // Reset pagination
            currentTourPassSearchPage = 0
            isLoadingMoreTourPasses = false
            hasMoreTourPasses = true

            tourPassManager.searchTourPasses(
                query = query,
                limit = BATCH_SIZE,
                offset = 0
            ).collect { result ->
                when (result) {
                    is ContentResult.Success -> {
                        hasMoreTourPasses = result.data.size >= BATCH_SIZE
                        tourPassManager.updateFeedState(ContentState.Success)
                    }
                    is ContentResult.Error -> {
                        tourPassManager.updateFeedState(ContentState.Error)
                        _events.emit(ContentEvent.Error(result.message))
                    }
                    ContentResult.Loading -> {
                        // No-op
                    }
                }
            }
        }
    }

    fun clearTourPassSearch() {
        currentTourPassSearchPage = 0
        isLoadingMoreTourPasses = false
        hasMoreTourPasses = true
        tourPassManager.clearSearch()
    }

    /**
     * Loads the next batch of tour passes when the user scrolls to the bottom.
     */
    fun loadMoreTourPasses() {
        if (isLoadingMoreTourPasses || !hasMoreTourPasses) {
            Log.d(TAG, "Skipping load more tour passes: isLoadingMore=$isLoadingMoreTourPasses, hasMoreData=$hasMoreTourPasses")
            return
        }

        viewModelScope.launch {
            Log.d(TAG, "Loading more tour passes...")
            isLoadingMoreTourPasses = true

            val activeQuery = searchFieldState.text.toString()
            val flowToCollect = if (activeQuery.isEmpty()) {
                currentTourPassFeedPage++

                tourPassManager.fetchFeedTourPasses(
                    forceRefresh = false,
                    limit = BATCH_SIZE,
                    offset = currentTourPassFeedPage * BATCH_SIZE
                )
            } else {
                currentTourPassSearchPage++

                tourPassManager.searchTourPasses(
                    query = activeQuery,
                    limit = BATCH_SIZE,
                    offset = currentTourPassSearchPage * BATCH_SIZE
                )
            }

            flowToCollect.collect { result ->
                when (result) {
                    is ContentResult.Success -> {
                        if (result.data.isEmpty() || result.data.size < BATCH_SIZE) {
                            hasMoreTourPasses = false
                        }
                        isLoadingMoreTourPasses = false
                    }
                    is ContentResult.Error -> {
                        _events.emit(ContentEvent.Error(result.message))
                        isLoadingMoreTourPasses = false
                    }
                    ContentResult.Loading -> {
                        // No-op
                    }
                }
            }
        }
    }

    /**
     * Fetches the feed of themes from the remote source.
     */
    fun fetchThemes(showLoading: Boolean = true) {
        viewModelScope.launch {
            // Reset pagination
            currentThemeFeedPage = 0
            isLoadingMoreThemes = false
            hasMoreThemes = true

            if (showLoading) {
                themeManager.updateFeedState(ContentState.Loading)
            }

            themeManager.fetchFeedThemes(
                forceRefresh = true,
                limit = BATCH_SIZE,
                offset = 0
            ).collect { result ->
                when (result) {
                    is ContentResult.Success -> {
                        hasMoreThemes = result.data.size >= BATCH_SIZE
                        themeManager.updateFeedState(ContentState.Success)
                        Log.d(TAG, "Fetched ${result.data.size} themes")
                    }
                    is ContentResult.Error -> {
                        themeManager.updateFeedState(ContentState.Error)
                        Log.e(TAG, "Error fetching themes: ${result.message}")
                    }
                    ContentResult.Loading -> {
                        // Already handled above
                    }
                }
            }
        }
    }

    /**
     * Searches for themes based on the provided query.
     */
    fun searchThemes(query: String) {
        viewModelScope.launch {
            // If the query is empty, clear the search and show feed
            if (query.isEmpty()) {
                clearThemeSearch()
                return@launch
            }

            Log.d(TAG, "Searching for themes with query: $query")

            themeManager.updateFeedState(ContentState.Loading)

            // Reset pagination
            currentThemeSearchPage = 0
            isLoadingMoreThemes = false
            hasMoreThemes = true

            themeManager.searchThemes(
                query = query,
                limit = BATCH_SIZE,
                offset = 0
            ).collect { result ->
                when (result) {
                    is ContentResult.Success -> {
                        hasMoreThemes = result.data.size >= BATCH_SIZE
                        themeManager.updateFeedState(ContentState.Success)
                    }
                    is ContentResult.Error -> {
                        themeManager.updateFeedState(ContentState.Error)
                        _events.emit(ContentEvent.Error(result.message))
                    }
                    ContentResult.Loading -> {
                        // No-op
                    }
                }
            }
        }
    }

    fun clearThemeSearch() {
        currentThemeSearchPage = 0
        isLoadingMoreThemes = false
        hasMoreThemes = true
        themeManager.clearSearch()
    }

    /**
     * Loads the next batch of themes when the user scrolls to the bottom.
     */
    fun loadMoreThemes() {
        if (isLoadingMoreThemes || !hasMoreThemes) {
            Log.d(TAG, "Skipping load more themes: isLoadingMore=$isLoadingMoreThemes, hasMoreData=$hasMoreThemes")
            return
        }

        viewModelScope.launch {
            Log.d(TAG, "Loading more themes...")
            isLoadingMoreThemes = true

            val activeQuery = searchFieldState.text.toString()
            val flowToCollect = if (activeQuery.isEmpty()) {
                currentThemeFeedPage++

                themeManager.fetchFeedThemes(
                    forceRefresh = false,
                    limit = BATCH_SIZE,
                    offset = currentThemeFeedPage * BATCH_SIZE
                )
            } else {
                currentThemeSearchPage++

                themeManager.searchThemes(
                    query = activeQuery,
                    limit = BATCH_SIZE,
                    offset = currentThemeSearchPage * BATCH_SIZE
                )
            }

            flowToCollect.collect { result ->
                when (result) {
                    is ContentResult.Success -> {
                        if (result.data.isEmpty() || result.data.size < BATCH_SIZE) {
                            hasMoreThemes = false
                        }
                        isLoadingMoreThemes = false
                    }
                    is ContentResult.Error -> {
                        _events.emit(ContentEvent.Error(result.message))
                        isLoadingMoreThemes = false
                    }
                    ContentResult.Loading -> {
                        // No-op
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
            chartManager.updateFeedState(ContentState.Loading)

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
                sortBy = currentSortOption,
                limit = BATCH_SIZE,
                offset = 0,
                filters = ChartQuery(
                    difficulties = difficulties,
                    genres = genres
                )
            ).collect { result ->
                when (result) {
                    is ContentResult.Success -> {
                        hasMoreData = result.data.size >= BATCH_SIZE
                        chartManager.updateFeedState(ContentState.Success)
                    }
                    is ContentResult.Error -> {
                        chartManager.updateFeedState(ContentState.Error)
                        _events.emit(ContentEvent.Error(result.message))
                    }
                    ContentResult.Loading -> {
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

    suspend fun observeTourPassScrollState() {
        Log.d(TAG, "Observing scroll state for tour passes")

        snapshotFlow {
            val layoutInfo = tourPassListState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItem =
                layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: return@snapshotFlow false

            lastVisibleItem >= totalItems - 3 // Load more when 3 items from end
        }
            .distinctUntilChanged()
            .collect { isAtEnd ->
                if (isAtEnd && tourPassManager.getTourPassesLength() > 0) {
                    Log.d(TAG, "User scrolled to bottom, loading more tour passes")
                    loadMoreTourPasses()
                }
            }
    }

    suspend fun observeThemeScrollState() {
        Log.d(TAG, "Observing scroll state for themes")

        snapshotFlow {
            val layoutInfo = themeListState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItem =
                layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: return@snapshotFlow false

            lastVisibleItem >= totalItems - 3 // Load more when 3 items from end
        }
            .distinctUntilChanged()
            .collect { isAtEnd ->
                if (isAtEnd && themeManager.getThemesLength() > 0) {
                    Log.d(TAG, "User scrolled to bottom, loading more themes")
                    loadMoreThemes()
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
                    sortBy = currentSortOption,
                    limit = BATCH_SIZE,
                    offset = currentSearchPage * BATCH_SIZE,
                    filters = ChartQuery(
                        difficulties = difficulties,
                        genres = genres
                    )
                )
            }

            flowToCollect.collect { result ->
                when (result) {
                    is ContentResult.Success -> {
                        if (result.data.isEmpty() || result.data.size < BATCH_SIZE) {
                            hasMoreData = false
                        }
                        isLoadingMore = false
                    }
                    is ContentResult.Error -> {
                        _events.emit(ContentEvent.Error(result.message))
                        isLoadingMore = false
                    }
                    ContentResult.Loading -> {
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

    private fun invalidateAndRefresh() {
        viewModelScope.launch {
            // 1. Clear the cached charts so stale data isn't shown
            chartManager.clearCache()
            tourPassManager.clearCache()
            themeManager.clearCache()

            // 2. Reset all pagination state
            currentFeedPage = 0
            currentSearchPage = 0
            isLoadingMore = false
            hasMoreData = true
            currentTourPassFeedPage = 0
            currentTourPassSearchPage = 0
            isLoadingMoreTourPasses = false
            hasMoreTourPasses = true
            currentThemeFeedPage = 0
            currentThemeSearchPage = 0
            isLoadingMoreThemes = false
            hasMoreThemes = true

            // 3. Clear any active search so we go back to the feed
            if (currentSearchQuery.isNotEmpty()) {
                clearSearch()
            }
            if (searchFieldState.text.isNotEmpty()) {
                clearTourPassSearch()
                clearThemeSearch()
            }

            // 4. Re-fetch fresh data (which will now include auth headers)
            fetchFeedCharts(true)
            fetchTourPasses(true)
            fetchThemes(true)
        }
    }
}