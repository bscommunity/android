package com.meninocoiso.beatstarcommunity.data.manager

import android.util.Log
import com.meninocoiso.beatstarcommunity.data.repository.ChartRepository
import com.meninocoiso.beatstarcommunity.domain.enums.Difficulty
import com.meninocoiso.beatstarcommunity.domain.enums.Genre
import com.meninocoiso.beatstarcommunity.domain.enums.OperationType
import com.meninocoiso.beatstarcommunity.domain.enums.SortOption
import com.meninocoiso.beatstarcommunity.domain.model.Chart
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
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
    data object Idle : ChartState()
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
    @Named("Remote") private val remoteChartRepository: ChartRepository,
    @Named("Local") private val localChartRepository: ChartRepository,
) {
    private val _remoteCharts = MutableStateFlow<Map<String, Chart>>(emptyMap())
    private val remoteCharts : StateFlow<Map<String, Chart>> = _remoteCharts.asStateFlow()

    // TODO: Maybe will be necessary to filter and insert cached chart after some installation, check later
    val searchCharts = remoteCharts.map { charts ->
        charts.values.toList()
    }
    
    private val _cachedCharts = MutableStateFlow<Map<String, Chart>>(emptyMap())
    private val cachedCharts: StateFlow<Map<String, Chart>> = _cachedCharts.asStateFlow()

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

    /**
     * Loads charts from local cache
     * @return FetchResult containing the cached charts
     */
    suspend fun loadCachedCharts(): FetchResult<List<Chart>> {
        return try {
            val cachedCharts = localChartRepository.getCharts().first()

            cachedCharts.fold(
                onSuccess = { charts ->
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

            // Fetch from remote
            val remoteResult = remoteChartRepository.getChartsSortedBy(
                sortBy = sortBy,
                limit = limit,
                offset = offset
            ).first()

            remoteResult.fold(
                onSuccess = { remoteCharts ->
                    Log.d(TAG, "Fetched ${remoteCharts.size} feed charts from remote")

                    // Cache the results if this is the initial page
                    if (offset == 0) {
                        localChartRepository.insertCharts(remoteCharts).first()
                    }

                    // Update our in-memory collection with the new charts
                    if (offset == 0) {
                        // Replace charts for initial load
                        updateChartsWithLimit(remoteCharts)
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
                    
                    // We cannot update our in-memory collection here, 
                    // since the query can change after remote fetch
                    // We pass this responsibility to the caller
                    
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

    fun updateRemoteCharts(searchResults: List<Chart>, offset: Int = 0) {
        if (offset == 0) {
            // For first page, replace existing charts with search results
            _remoteCharts.value = searchResults.associateBy { it.id }
        } else {
            // For pagination, append to existing results
            val updatedMap = _cachedCharts.value.toMutableMap()
            searchResults.forEach { chart ->
                updatedMap[chart.id] = chart
            }
            _remoteCharts.value = updatedMap
        }
    }
    
    fun clearRemoteCharts() {
        _remoteCharts.value = emptyMap()
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

                    installedCharts.forEach { chart ->
                        val remoteVersion = latestVersions.find { it.chartId == chart.id }

                        if (remoteVersion != null &&
                            remoteVersion.index > chart.latestVersion.index) {

                            val updatedChart = chart.copy(availableVersion = remoteVersion)
                            updateChart(updatedChart)
                            chartsToUpdate.add(updatedChart)
                        }
                    }

                    // Update in local repository
                    if (chartsToUpdate.isNotEmpty()) {
                        localChartRepository.updateCharts(chartsToUpdate).first()
                    }

                    emit(FetchResult.Success(chartsToUpdate))
                    Log.d(TAG, "Updated ${chartsToUpdate.size} charts with new versions")
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
     * Update multiple charts in memory
     */
    private fun updateCharts(charts: List<Chart>) {
        val updatedMap = _cachedCharts.value.toMutableMap()

        charts.forEach { chart ->
            // Add to our map, preserving any existing data not in the update
            updatedMap[chart.id] = _cachedCharts.value[chart.id]?.let { existingChart ->
                // If we have this chart already, preserve certain fields
                // that might not be in the new data
                when {
                    // If chart is from remote, preserve installed status
                    chart.isInstalled == null -> chart.copy(
                        isInstalled = existingChart.isInstalled,
                        availableVersion = existingChart.availableVersion
                    )
                    // Otherwise take the new version completely
                    else -> chart
                }
            } ?: chart
        }

        _cachedCharts.value = updatedMap
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
     * Update charts while respecting the maximum cache size limit
     * Installed charts are preserved regardless of limit
     */
    private fun updateChartsWithLimit(newCharts: List<Chart>) {
        val updatedMap = _cachedCharts.value.toMutableMap()

        // Add all new charts to our map, preserving any existing data
        newCharts.forEach { chart ->
            updatedMap[chart.id] = _cachedCharts.value[chart.id]?.let { existingChart ->
                when {
                    chart.isInstalled == null -> chart.copy(
                        isInstalled = existingChart.isInstalled,
                        availableVersion = existingChart.availableVersion
                    )
                    else -> chart
                }
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
        } else {
            // We're within limits, just update the map
            _cachedCharts.value = updatedMap
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