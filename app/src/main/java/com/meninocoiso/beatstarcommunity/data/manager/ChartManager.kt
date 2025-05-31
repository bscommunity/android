package com.meninocoiso.beatstarcommunity.data.manager

import android.content.Context
import android.net.Uri
import android.util.Log
import com.meninocoiso.beatstarcommunity.data.repository.CacheRepository
import com.meninocoiso.beatstarcommunity.data.repository.ChartRepository
import com.meninocoiso.beatstarcommunity.domain.enums.Difficulty
import com.meninocoiso.beatstarcommunity.domain.enums.Genre
import com.meninocoiso.beatstarcommunity.domain.enums.OperationType
import com.meninocoiso.beatstarcommunity.domain.enums.SortOption
import com.meninocoiso.beatstarcommunity.domain.model.Chart
import com.meninocoiso.beatstarcommunity.util.StorageUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

private const val TAG = "ChartManager"

/**
 * Result wrapper for chart operations
 */
sealed class FetchResult<out T> {
    data class Success<T>(val data: T) : FetchResult<T>()
    data class Error(val message: String, val cause: Throwable? = null) : FetchResult<Nothing>()
    data object Loading : FetchResult<Nothing>()
}

sealed class ChartState {
    data object Loading : ChartState()
    data object Success : ChartState()
    data object Error : ChartState()
}

// Event for one-time UI actions
sealed class FetchEvent {
    data class Error(val message: String) : FetchEvent()
}

private const val MAX_CACHED_CHARTS = 50

/**
 * Singleton manager for chart data across the application
 */
@Singleton
class ChartManager @Inject constructor(
    @ApplicationContext private val context: Context,
    @Named("Remote") private val remoteChartRepository: ChartRepository,
    @Named("Local") private val localChartRepository: ChartRepository,
    private val cacheRepository: CacheRepository
) {
    private val _remoteCharts = MutableStateFlow<List<Chart>>(emptyList())
    private val remoteCharts: StateFlow<List<Chart>> = _remoteCharts.asStateFlow()

    var searchQuery: String = ""
        set(value) {
            field = value
            _remoteCharts.value = emptyList()
        }

    private val _cachedCharts = MutableStateFlow<List<Chart>>(emptyList())
    val cachedCharts: StateFlow<List<Chart>> = _cachedCharts.asStateFlow()

    val searchCharts = remoteCharts.combine(cachedCharts) { remote, cached ->
        val result = mutableListOf<Chart>()

        // Add all remote charts from the search
        remote.forEach { remoteChart ->
            // If there's a cached version installed, use it
            val cachedChart = cached.find { it.id == remoteChart.id }
            if (cachedChart != null) {
                result.add(cachedChart)
            } else {
                // Otherwise, just add the remote chart
                result.add(remoteChart)
            }
        }

        result
    }

    // Filtered chart collections
    val workshopCharts: Flow<List<Chart>> = cachedCharts

    val installedCharts: Flow<List<Chart>> = cachedCharts.map { charts ->
        charts.filter { it.isInstalled == true }
    }

    val chartsWithUpdates: Flow<List<Chart>> = cachedCharts.map { charts ->
        charts.filter {
            it.isInstalled == true && it.availableVersion != null
        }
    }

    private val _cacheState = MutableStateFlow<ChartState>(ChartState.Loading)
    val cacheState: StateFlow<ChartState> = _cacheState.asStateFlow()

    fun updateState(newState: ChartState) {
        _cacheState.value = newState
    }

    /**
     * Get the number of charts synchronously
     * This method uses the current value of the StateFlow to provide an immediate count
     */
    fun getChartsLength(): Int {
        return _cachedCharts.value.size
    }

    fun verifyInstalledCharts(cachedCharts: List<Chart>, rootUri: Uri): List<Chart> {
        val installedCharts = cachedCharts.filter { it.isInstalled == true }
        val chartsToUpdate = mutableListOf<Chart>()

        for (chart in installedCharts) {
            val destination = StorageUtils.getFolder(rootUri, listOf("songs"), context)
            val folderName = StorageUtils.getChartFolderName(chart.id)

            // Verify if the chart file exists physically
            val chartFolder = destination.findFile(folderName)

            if (chartFolder == null && chart.isInstalled == true) {
                // The file doesn't exist anymore, but is marked as installed
                chart.isInstalled = false
                chartsToUpdate.add(chart)
            }
        }

        return chartsToUpdate
    }

    /**
     * Loads charts from local cache
     * @return FetchResult containing the cached charts
     */
    suspend fun loadCachedCharts(sortBy: SortOption, rootUri: Uri? = null) {
        try {
            val cachedCharts = localChartRepository.getChartsSortedBy(sortBy).first()

            cachedCharts.fold(
                onSuccess = { allCharts ->
                    var charts = allCharts
                    Log.d(TAG, "Charts order: ${charts.joinToString { it.track }}")

                    // Verify if the charts are still installed
                    val chartsToUpdate = if (rootUri != null) verifyInstalledCharts(
                        allCharts,
                        rootUri
                    ) else emptyList()

                    // If some charts were deleted externally, we update them
                    if (chartsToUpdate.isNotEmpty()) {
                        // Update the charts in local storage
                        localChartRepository.updateCharts(chartsToUpdate)

                        // Update the charts in memory (bellow)
                        charts = allCharts.filter {
                            // Filter out charts on chartsToUpdate
                            !chartsToUpdate.any { chart -> chart.id == it.id }
                        }
                    }

                    Log.d(TAG, "Loaded ${charts.size} charts from cache with sortBy: $sortBy")
                    updateCharts(charts)
                    updateState(ChartState.Success)
                },
                onFailure = { error ->
                    Log.e(TAG, "Failed to load cached charts", error)
                    FetchResult.Error("Failed to load cached charts", error)
                    updateState(ChartState.Error)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error loading cached charts", e)
            FetchResult.Error("Unexpected error loading cached charts", e)
        }
    }

    /**
     * Fetches charts in a feed-style format from remote source with specified sorting
     * Results are cached for future use
     *
     * @param sortBy The sorting order for the feed (weekly_rank, last_updated, etc.)
     * @param forceRefresh Whether to force a refresh or use cached results if available
     * @param limit Maximum number of charts to fetch
     * @param offset The offset from which to start fetching
     */
    fun fetchFeedCharts(
        sortBy: SortOption,
        forceRefresh: Boolean = false,
        limit: Int = 10,
        offset: Int = 0
    ): Flow<FetchResult<List<Chart>>> = flow {
        emit(FetchResult.Loading)

        Log.d(TAG, "Fetching feed charts with sortBy: $sortBy, limit: $limit, offset: $offset")

        try {
            // Only check cache for initial load (offset=0) and when not forcing refresh
            if (offset == 0 && !forceRefresh) {
                val cachedCharts = localChartRepository.getChartsSortedBy(sortBy, limit).first()

                cachedCharts.fold(
                    onSuccess = { charts ->
                        if (charts.isNotEmpty()) {
                            Log.d(TAG, "Using ${charts.size} cached charts for feed")
                            updateCharts(charts)
                            emit(FetchResult.Success(charts))
                            return@flow
                        }
                    },
                    onFailure = { /* Continue to remote fetch if cache fails */ }
                )
            }

            Log.d(TAG, "Cache miss or forced refresh, fetching from remote")

            // Fetch from remote
            val remoteResult = remoteChartRepository.getChartsSortedBy(
                sortBy = sortBy,
                limit = limit,
                offset = offset
            ).first()

            remoteResult.fold(
                onSuccess = { remoteCharts ->
                    Log.d(TAG, "Fetched ${remoteCharts.size} feed charts from remote")

                    // For initial load
                    if (offset == 0) {
                        // Check and remove charts that are no longer on remote
                        val deletedCharts = handleDeletedCharts(remoteCharts)
                        if (deletedCharts.isNotEmpty()) {
                            Log.d(TAG, "Removing ${deletedCharts.size} deleted charts from cache")
                            CoroutineScope(Dispatchers.IO).launch {
                                localChartRepository.deleteCharts(deletedCharts)
                            }
                        }

                        // Insert or update the new charts in local repository
                        // Log.d(TAG, "Inserting/updating ${remoteCharts.size} charts into local storage")
                        CoroutineScope(Dispatchers.IO).launch {
                            localChartRepository.updateCharts(remoteCharts)
                        
                        }
                    }

                    // Update the in-memory cache with the new charts
                    updateCharts(remoteCharts)

                    emit(FetchResult.Success(remoteCharts))
                },
                onFailure = { error ->
                    Log.e(TAG, "Failed to fetch feed charts", error)
                    emit(FetchResult.Error("Failed to fetch feed charts", error))
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error fetching feed charts", e)
            emit(FetchResult.Error("Unexpected error fetching feed charts", e))
        }
    }.catch { e ->
        Log.e(TAG, "Exception in fetchFeedCharts flow", e)
        emit(FetchResult.Error("Exception in feed charts flow", e))
    }

    /**
     * Handle charts that have been deleted from the remote server
     * @param remoteCharts The charts fetched from remote
     * @return List of charts to be cached
     */
    private fun handleDeletedCharts(remoteCharts: List<Chart>): List<Chart> {
        val cachedMap = _cachedCharts.value.toMutableList()
        val remoteIds = remoteCharts.map { it.id }.toSet()

        // Find charts of the same type in cache that aren't in the remote result
        // This suggests they've been deleted or made private
        val potentiallyDeleted = cachedMap.filter { chart ->
            // Only consider non-installed charts for deletion
            chart.isInstalled != true && chart.id !in remoteIds
        }

        if (potentiallyDeleted.isNotEmpty()) {
            Log.d(
                TAG,
                "Found ${potentiallyDeleted.size} charts that may have been deleted/unpublished"
            )

            // Remove these charts from cache
            potentiallyDeleted.forEach { chart ->
                cachedMap.remove(chart)
                Log.d(
                    TAG,
                    "Removed chart with id: ${chart.id} from cache as it no longer exists on remote"
                )
            }

            // Update the in-memory cache
            _cachedCharts.value = cachedMap

            // Also remove from local repository
            return potentiallyDeleted
        }

        return emptyList()
    }

    /**
     * Searches for charts based on query and filters
     * Results are NOT cached
     *
     * @param query The search term to look for
     * @param difficulties Optional list of difficulties to filter by
     * @param genres Optional list of genres to filter by
     * @param limit Maximum number of charts to fetch
     * @param offset The offset from which to start fetching
     */
    fun searchCharts(
        query: String,
        difficulties: List<Difficulty>? = null,
        genres: List<Genre>? = null,
        limit: Int = 10,
        offset: Int = 0
    ): Flow<FetchResult<List<Chart>>> = flow {
        if (query.isEmpty()) {
            _remoteCharts.value = emptyList()
            emit(FetchResult.Success(emptyList()))
            return@flow
        }

        emit(FetchResult.Loading)

        try {
            val remoteResult = remoteChartRepository.getCharts(
                query = query,
                difficulties = difficulties,
                genres = genres,
                limit = limit,
                offset = offset
            ).first()

            remoteResult.fold(
                onSuccess = { searchResults ->
                    Log.d(TAG, "Search found ${searchResults.size} charts for query: $query")

                    // Prevent emitting results if the query has changed
                    if (searchQuery != query) {
                        Log.d(TAG, "Skipping search result: query has changed")
                        return@flow
                    }

                    // Update in-memory state
                    if (offset == 0) {
                        // For first page, replace existing charts with search results
                        _remoteCharts.value = searchResults
                    } else {
                        // For pagination, append to existing results
                        val updatedList = _remoteCharts.value.toMutableList()
                        updatedList.addAll(searchResults)
                        _remoteCharts.value = updatedList
                    }

                    emit(FetchResult.Success(searchResults))
                },
                onFailure = { error ->
                    Log.e(TAG, "Search failed for query: $query", error)
                    emit(FetchResult.Error("Search failed", error))
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error searching charts", e)
            emit(FetchResult.Error("Unexpected error searching charts", e))
        }
    }.catch { e ->
        Log.e(TAG, "Exception in remoteCharts flow", e)
        emit(FetchResult.Error("Exception in search charts flow", e))
    }

    /**
     * Check for updates to installed charts
     */
    fun checkForUpdates(): Flow<FetchResult<List<Chart>>> = flow {
        emit(FetchResult.Loading)

        val installedCharts = _cachedCharts.value
            .filter { it.isInstalled == true }

        if (installedCharts.isEmpty()) {
            emit(FetchResult.Success(emptyList<Chart>()))
            return@flow
        }

        try {
            val latestVersionsResult = remoteChartRepository
                .getLatestVersionsByChartIds(installedCharts.map { it.id })
                .first()

            latestVersionsResult.fold(
                onSuccess = { latestVersions ->
                    // Update charts that have newer versions available
                    val chartsToUpdate = mutableListOf<Chart>()
                    val chartsToDelete = mutableListOf<Chart>()
                    val existingRemoteIds = latestVersions.map { it.chartId }.toSet()

                    installedCharts.forEach { chart ->
                        val remoteVersion = latestVersions.find { it.chartId == chart.id }

                        if (remoteVersion != null) {
                            // Chart exists on remote, check for updates
                            if (remoteVersion.index > chart.latestVersion.index) {
                                val updatedChart = chart.copy(availableVersion = remoteVersion)
                                updateChart(updatedChart)
                                chartsToUpdate.add(updatedChart)
                            }
                        } else if (chart.id !in existingRemoteIds) {
                            // Chart doesn't exist on remote anymore
                            // Note: We don't delete the installed chart, just mark it for potential UI warning
                            Log.d(TAG, "Chart ${chart.id} no longer exists on remote server")
                            chartsToDelete.add(chart)
                        }
                    }

                    // Update in local repository
                    if (chartsToUpdate.isNotEmpty()) {
                        localChartRepository.updateCharts(chartsToUpdate).first()
                    }

                    emit(FetchResult.Success(chartsToUpdate))
                    Log.d(TAG, "Updated ${chartsToUpdate.size} charts with new versions")

                    if (chartsToDelete.isNotEmpty()) {
                        Log.d(
                            TAG,
                            "Detected ${chartsToDelete.size} installed charts that are no longer on remote"
                        )
                    }
                },
                onFailure = { error ->
                    emit(FetchResult.Error("Failed to check for updates", error))
                }
            )
        } catch (e: Exception) {
            emit(FetchResult.Error("Error checking for updates", e))
        }
    }.catch { e ->
        emit(FetchResult.Error("Unexpected error checking for updates", e))
    }

    /*
    * Add a single chart to cache
    */
    fun addChart(chart: Chart) {
        _cachedCharts.value = _cachedCharts.value.toMutableList().apply {
            add(chart)
        }
    }

    /**
     * Update a single chart in memory
     */
    private fun updateChart(chart: Chart) {
        _cachedCharts.value = _cachedCharts.value.toMutableList().apply {
            val index = indexOfFirst { it.id == chart.id }
            if (index != -1) {
                set(index, chart)
            } else {
                add(chart)
            }
        }
    }

    /**
     * Update chart in memory with a specific operation
     */
    fun updateChart(chartId: String, operation: OperationType): Flow<FetchResult<List<Chart>>> =
        flow {
            // Find the chart in our in-memory cache
            val existingChart = _cachedCharts.value.find { it.id == chartId } ?: run {
                // If chart was not found on cache, check in remote and cache it
                val remoteChart = remoteCharts.value.find { it.id == chartId }
                if (remoteChart != null) {
                    Log.d(TAG, "Chart with id: $chartId not found in cache, added from remote")
                    addChart(
                        remoteChart.copy(
                            isInstalled = true
                        )
                    )
                    return@run remoteChart
                } else {
                    Log.d(TAG, "Chart with id: $chartId not found")
                    emit(FetchResult.Error("Chart not found"))
                    return@flow
                }
            }

            // Update in local repository
            val result = localChartRepository.updateChart(chartId, operation).first()

            result.fold(
                onSuccess = { updated ->
                    if (!updated) {
                        emit(FetchResult.Error("Failed to update chart in local storage"))
                        return@flow
                    }
                },
                onFailure = { error ->
                    emit(FetchResult.Error("Error updating chart in local storage", error))
                    return@flow
                }
            )

            Log.d(TAG, "Updated chart with id: $chartId in local storage")

            // Update in-memory cache
            when (operation) {
                OperationType.INSTALL -> {
                    updateChart(existingChart.copy(isInstalled = true))
                }

                OperationType.UPDATE -> {
                    if (existingChart.availableVersion == null) {
                        emit(FetchResult.Error("No available version to update"))
                        return@flow
                    }
                    updateChart(
                        existingChart.copy(
                            latestVersion = existingChart.availableVersion!!,
                            availableVersion = null,
                        )
                    )
                }

                OperationType.DELETE -> {
                    updateChart(existingChart.copy(isInstalled = false))
                }
            }

            Log.d(TAG, "Updated chart with id: $chartId in memory")

            // Only emit a single success result
            emit(FetchResult.Success(_cachedCharts.value))
        }.catch { e ->
            emit(FetchResult.Error("Unexpected error updating chart", e))
        }


    /**
     * Update multiple charts in memory
     */
    private fun updateCharts(charts: List<Chart>) {
        // Log.d("SERVER", charts.joinToString { it.track })

        val currentCache = _cachedCharts.value
        val updatedList = charts.map { chart ->
            val existingChart = currentCache.find { it.id == chart.id }
            if (existingChart != null) {
                chart.copy(
                    isInstalled = existingChart.isInstalled,
                    availableVersion = existingChart.availableVersion
                )
            } else {
                chart
            }
        }.toMutableList()

        // Log.d("UPDATED", updatedList.joinToString { it.track })

        _cachedCharts.value = updatedList
    }

    /**
     * Update charts while respecting the maximum cache size limit
     * Installed charts are preserved regardless of limit
     */
    private fun updateChartsWithLimit(newCharts: List<Chart>): List<Chart> {
        // Updates the cached charts while respecting the maximum cache size limit
        val currentCache = _cachedCharts.value
        val installedCharts = currentCache.filter { it.isInstalled == true }
            .associateBy { it.id }

        // Updates the new charts with installed status and available version
        val updatedNewCharts = newCharts.map { chart ->
            val installed = installedCharts[chart.id]
            if (installed != null) {
                chart.copy(
                    isInstalled = installed.isInstalled,
                    availableVersion = installed.availableVersion
                )
            } else {
                chart
            }
        }

        val nonInstalled = updatedNewCharts.filter { it.isInstalled != true }

        // Limit the number of non-installed charts to MAX_CACHED_CHARTS
        val limitedNonInstalled = if (nonInstalled.size > MAX_CACHED_CHARTS) {
            nonInstalled.takeLast(MAX_CACHED_CHARTS)
        } else {
            nonInstalled
        }

        // Keeps the original order, but only with the limited non-installed charts
        val finalList =
            updatedNewCharts.filter { it.isInstalled == true || it in limitedNonInstalled }

        _cachedCharts.value = finalList
        return finalList
    }

    fun getSuggestions(query: String): Flow<List<String>> = flow {
        val result = remoteChartRepository.getSuggestions(query).first()

        result.fold(
            onSuccess = { suggestions ->
                // emit(FetchResult.Success(suggestions))
                emit(suggestions)
            },
            onFailure = { error ->
                // emit(FetchResult.Error("Failed to fetch suggestions", error))
                emit(emptyList<String>())
            }
        )
    }.catch { e ->
        // emit(FetchResult.Error("Unexpected error fetching suggestions", e))
        emit(emptyList<String>())
    }

    /**
     * Notify the server about a chart download operation
     */
    fun postAnalytics(
        chartId: String,
        operation: OperationType
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                remoteChartRepository.postAnalytics(chartId, operation).collect { result ->
                    if (result.isSuccess) {
                        Log.i(
                            TAG,
                            "Analytics posted for chartId: $chartId with operation: $operation"
                        )
                    } else {
                        Log.e(
                            TAG,
                            "Failed to post analytics for chartId: $chartId",
                            result.exceptionOrNull()
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception posting analytics for chartId: $chartId", e)
            }
        }
    }
}



