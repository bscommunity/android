package com.meninocoiso.beatstarcommunity.data.manager

import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import com.meninocoiso.beatstarcommunity.data.repository.ChartRepository
import com.meninocoiso.beatstarcommunity.data.repository.SettingsRepository
import com.meninocoiso.beatstarcommunity.domain.enums.Difficulty
import com.meninocoiso.beatstarcommunity.domain.enums.Genre
import com.meninocoiso.beatstarcommunity.domain.enums.OperationType
import com.meninocoiso.beatstarcommunity.domain.enums.SortOption
import com.meninocoiso.beatstarcommunity.domain.model.Chart
import com.meninocoiso.beatstarcommunity.util.StorageUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
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
    private val settingsRepository: SettingsRepository
) {
    private val _remoteCharts = MutableStateFlow<Map<String, Chart>>(emptyMap())
    private val remoteCharts : StateFlow<Map<String, Chart>> = _remoteCharts.asStateFlow()

    var searchQuery: String = ""
        set(value) {
            field = value
            _remoteCharts.value = emptyMap()
        }

    private val _cachedCharts = MutableStateFlow<Map<String, Chart>>(emptyMap())
    private val cachedCharts: StateFlow<Map<String, Chart>> = _cachedCharts.asStateFlow()

    val searchCharts = remoteCharts.combine(cachedCharts) { remote, cached ->
        val result = mutableListOf<Chart>()

        // Add all remote charts from the search
        remote.values.forEach { remoteChart ->
            // If there's a cached version installed, use it
            if (cached.containsKey(remoteChart.id)) {
                result.add(cached[remoteChart.id]!!)
            } else {
                // Otherwise, just add the remote chart
                result.add(remoteChart)
            }
        }

        result
    }

    // Filtered chart collections
    val workshopCharts: Flow<List<Chart>> = cachedCharts.map { charts ->
        charts.values.toList()
    }

    val installedCharts: Flow<List<Chart>> = cachedCharts.map { charts ->
        charts.values.filter { it.isInstalled == true }.toList()
    }

    val chartsWithUpdates: Flow<List<Chart>> = cachedCharts.map { charts ->
        charts.values.filter {
            it.isInstalled == true && it.availableVersion != null
        }.toList()
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

    suspend fun verifyInstalledCharts(cachedCharts: List<Chart>): List<Chart> {
        val installedCharts = cachedCharts.filter { it.isInstalled == true }
        val chartsToUpdate = mutableListOf<Chart>()

        for (chart in installedCharts) {
            val rootUri = settingsRepository.getFolderUri()?.toUri()
                ?: throw IllegalStateException("Could not access or create beatstar folder")

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
    suspend fun loadCachedCharts(): FetchResult<List<Chart>> {
        return try {
            val cachedCharts = localChartRepository.getCharts().first()

            cachedCharts.fold(
                onSuccess = { allCharts ->
                    var charts = allCharts

                    // Verify if the charts are still installed
                    val chartsToUpdate = verifyInstalledCharts(allCharts)

                    if (chartsToUpdate.isNotEmpty()) {
                        // Update the charts in local storage
                        localChartRepository.updateCharts(chartsToUpdate)

                        // Update the charts in memory
                        charts = allCharts.filter {
                            // Filter out charts on chartsToUpdate
                            !chartsToUpdate.any { chart -> chart.id == it.id }
                        }
                    }

                    Log.d(TAG, "Loaded ${charts.size} charts from cache")
                    updateCharts(charts)
                    FetchResult.Success(charts)
                },
                onFailure = { error ->
                    Log.e(TAG, "Failed to load cached charts", error)
                    FetchResult.Error("Failed to load cached charts", error)
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

                    // Update our in-memory collection with the new charts
                    if (offset == 0) {
                        // For initial load, check and remove charts that are no longer on remote
                        val updatedCache = handleDeletedCharts(remoteCharts, sortBy)

                        // Since we're on initial page, cache them in local storage
                        localChartRepository.updateCharts(updatedCache).first()
                    } else {
                        // Append charts for pagination
                        updateCharts(remoteCharts)
                    }

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
     * @param sortBy The sorting option used for fetching
     * @return List of charts to be cached
     */
    private suspend fun handleDeletedCharts(remoteCharts: List<Chart>, sortBy: SortOption): List<Chart> {
        val cachedMap = _cachedCharts.value.toMutableMap()
        val remoteIds = remoteCharts.map { it.id }.toSet()

        // Find charts of the same type in cache that aren't in the remote result
        // This suggests they've been deleted or made private
        val potentiallyDeleted = cachedMap.filter { (id, chart) ->
            // Only consider non-installed charts for deletion
            chart.isInstalled != true && id !in remoteIds
        }

        if (potentiallyDeleted.isNotEmpty()) {
            Log.d(TAG, "Found ${potentiallyDeleted.size} charts that may have been deleted/unpublished")

            // Remove these charts from cache
            potentiallyDeleted.keys.forEach { id ->
                cachedMap.remove(id)
                Log.d(TAG, "Removed chart with id: $id from cache as it no longer exists on remote")
            }

            // Update the in-memory cache
            _cachedCharts.value = cachedMap

            // Also remove from local repository
            localChartRepository.deleteCharts(potentiallyDeleted.values.toList()).first()
        }

        // Now add/update the remote charts
        return updateChartsWithLimit(remoteCharts)
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
            _remoteCharts.value = emptyMap()
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
                        _remoteCharts.value = searchResults.associateBy { it.id }
                    } else {
                        // For pagination, append to existing results
                        val updatedMap = _remoteCharts.value.toMutableMap()
                        searchResults.forEach { chart ->
                            updatedMap[chart.id] = chart
                        }
                        _remoteCharts.value = updatedMap
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

        val installedCharts = _cachedCharts.value.values
            .filter { it.isInstalled == true }
            .toList()

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
                        Log.d(TAG, "Detected ${chartsToDelete.size} installed charts that are no longer on remote")
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
        _cachedCharts.value = _cachedCharts.value.toMutableMap().apply {
            this[chart.id] = chart
        }
    }

    /**
     * Update a single chart in memory
     */
    private fun updateChart(chart: Chart) {
        _cachedCharts.value = _cachedCharts.value.toMutableMap().apply {
            this[chart.id] = chart
        }
    }

    /**
     * Update chart in memory with a specific operation
     */
    fun updateChart(chartId: String, operation: OperationType): Flow<FetchResult<List<Chart>>> = flow {
        // Find the chart in our in-memory cache
        val existingChart = _cachedCharts.value[chartId] ?: run {
            // If chart was not found on cache, check in remote and cache it
            val remoteChart = remoteCharts.value[chartId]
            if (remoteChart != null) {
                Log.d(TAG, "Chart with id: $chartId not found in cache, added from remote")
                addChart(remoteChart.copy(
                    isInstalled = true
                ))
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
                updateChart(existingChart.copy(
                    latestVersion = existingChart.availableVersion!!,
                    availableVersion = null,
                ))
            }
            OperationType.DELETE -> {
                updateChart(existingChart.copy(isInstalled = false))
            }
        }

        Log.d(TAG, "Updated chart with id: $chartId in memory")

        // Only emit a single success result
        emit(FetchResult.Success(_cachedCharts.value.values.toList()))
    }.catch { e ->
        emit(FetchResult.Error("Unexpected error updating chart", e))
    }


    /**
     * Update multiple charts in memory
     */
    private fun updateCharts(charts: List<Chart>) {
        val updatedMap = _cachedCharts.value.toMutableMap()

        charts.forEach { chart ->
            // Add to our map, preserving any existing data not in the update
            updatedMap[chart.id] = _cachedCharts.value[chart.id]?.let { existingChart ->
                chart.copy(
                    isInstalled = existingChart.isInstalled,
                    availableVersion = existingChart.availableVersion
                )
            } ?: chart
        }

        _cachedCharts.value = updatedMap
    }

    /**
     * Update charts while respecting the maximum cache size limit
     * Installed charts are preserved regardless of limit
     */
    private fun updateChartsWithLimit(newCharts: List<Chart>): List<Chart> {
        val updatedMap = _cachedCharts.value.toMutableMap()

        // Add all new charts to our map, preserving any existing data
        newCharts.forEach { chart ->
            updatedMap[chart.id] = _cachedCharts.value[chart.id]?.let { existingChart ->
                chart.copy(
                    isInstalled = existingChart.isInstalled,
                    availableVersion = existingChart.availableVersion
                )
            } ?: chart
        }

        // Enforce the maximum cache size for non-installed charts
        val installedCharts = updatedMap.values.filter { it.isInstalled == true }
        val nonInstalledCharts = updatedMap.values.filter { it.isInstalled != true }

        // If we have too many non-installed charts, keep only the most recent ones
        if (nonInstalledCharts.size > MAX_CACHED_CHARTS) {
            val chartsToKeep = nonInstalledCharts
                .sortedByDescending { it.latestVersion.publishedAt } // Keep newest charts
                .take(MAX_CACHED_CHARTS)

            // Create new map with installed charts and the charts to keep
            val finalMap = mutableMapOf<String, Chart>()
            installedCharts.forEach { finalMap[it.id] = it }
            chartsToKeep.forEach { finalMap[it.id] = it }

            _cachedCharts.value = finalMap

            return finalMap.values.toList()
        } else {
            // We're within limits, just update the map
            _cachedCharts.value = updatedMap

            return updatedMap.values.toList()
        }
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
}