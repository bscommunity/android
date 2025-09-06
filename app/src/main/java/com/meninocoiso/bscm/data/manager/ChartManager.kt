package com.meninocoiso.bscm.data.manager

import android.content.Context
import android.net.Uri
import android.util.Log
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.data.repository.CacheRepository
import com.meninocoiso.bscm.data.repository.ChartRepository
import com.meninocoiso.bscm.di.ApplicationScope
import com.meninocoiso.bscm.domain.enums.Difficulty
import com.meninocoiso.bscm.domain.enums.Genre
import com.meninocoiso.bscm.domain.enums.OperationType
import com.meninocoiso.bscm.domain.enums.SortOption
import com.meninocoiso.bscm.domain.model.Chart
import com.meninocoiso.bscm.util.StorageUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
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
private const val MAX_CACHED_CHARTS = 50

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
/**
 * Singleton manager for chart data across the application
 */
@Singleton
class ChartManager @Inject constructor(
    @ApplicationContext private val context: Context,
    @Named("Remote") private val remoteChartRepository: ChartRepository,
    @Named("Local") private val localChartRepository: ChartRepository,
    private val cacheRepository: CacheRepository,
    @ApplicationScope private val coroutineScope: CoroutineScope) 
{

    // Single source of truth for all charts in memory
    private val _charts = MutableStateFlow<Map<String, Chart>>(emptyMap())
    private val charts: StateFlow<Map<String, Chart>> = _charts.asStateFlow()

    // Search-specific state
    private val _searchResults = MutableStateFlow<List<String>>(emptyList()) // Chart IDs
    private val searchResults: StateFlow<List<String>> = _searchResults.asStateFlow()

    private val _cacheState = MutableStateFlow<ChartState>(ChartState.Loading)
    val cacheState: StateFlow<ChartState> = _cacheState.asStateFlow()

    var currentSearchQuery: String = ""
        private set

    // Derived flows for different chart collections
    val memoryCharts: Flow<List<Chart>> = charts.map { it.values.toList() }

    val installedCharts: Flow<List<Chart>> = charts.map { chartMap ->
        chartMap.values.filter { it.isInstalled == true }
    }

    val chartsWithUpdates: Flow<List<Chart>> = charts.map { chartMap ->
        chartMap.values.filter { it.isInstalled == true && it.availableVersion != null }
    }

    val searchCharts: Flow<List<Chart>> = combine(searchResults, charts) { searchIds, chartMap ->
        searchIds.mapNotNull { chartMap[it] }
    }

    private val feedChartsList = mutableListOf<Chart>()

    fun updateState(newState: ChartState) {
        // Log.d(TAG, "Updating cache state to $newState")
        _cacheState.value = newState
    }

    fun getChartsLength(): Int = _charts.value.size

    fun verifyInstalledCharts(chartsToVerify: List<Chart>, rootUri: Uri): List<Chart> {
        try {
            val destination = StorageUtils.getFolder(rootUri, listOf("songs"), context)
            
            return chartsToVerify.map { chart ->
                val folderName = StorageUtils.getChartFolderName(chart.id)
                val chartFolder = destination.findFile(folderName)
                val fileExists = chartFolder != null
    
                chart.copy(isInstalled = fileExists).also { updatedChart ->
                    // Update in-memory state if verification changed the status
                    if (chart.isInstalled != fileExists) {
                        updateChartInMemory(updatedChart)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error verifying installed charts", e)

            // If we can't access the root folder, clear the cache
            // This edge case is very specific but can happen if the user changes storage permissions
            // We could simply allow the error in background, but clearing the cache ensures it won't
            // error again while the user don't re-selects the folder
            coroutineScope.launch {
                cacheRepository.setFolderUri("")
            }
            
            return chartsToVerify
        }
    }

    /**
     * Loads charts from local cache into memory
     */
    suspend fun loadCachedCharts(sortBy: SortOption, rootUri: Uri? = null) {
        _cacheState.value = ChartState.Loading
        
        try {
            val cachedCharts = localChartRepository.getChartsSortedBy(sortBy).first()

            cachedCharts.fold(
                onSuccess = { allCharts ->
                    Log.d(TAG, "Loading ${allCharts.size} charts from cache")

                    val verifiedCharts = if (rootUri != null) {
                        verifyInstalledCharts(allCharts, rootUri)
                    } else {
                        allCharts
                    }

                    // Replace all charts in memory
                    replaceChartsInMemory(verifiedCharts)
                    _cacheState.value = ChartState.Success

                    Log.d(TAG, "Successfully loaded ${verifiedCharts.size} charts from cache")
                },
                onFailure = { error ->
                    Log.e(TAG, "Failed to load cached charts", error)
                    _cacheState.value = ChartState.Error
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error loading cached charts", e)
            _cacheState.value = ChartState.Error
        }
    }

    /**
     * Fetches charts in a feed-style format from remote source
     */
    fun fetchFeedCharts(
        sortBy: SortOption,
        forceRefresh: Boolean = false,
        limit: Int = 10,
        offset: Int = 0
    ): Flow<FetchResult<List<Chart>>> = flow {
        emit(FetchResult.Loading)
        Log.d(TAG, "Fetching feed charts: sortBy=$sortBy, limit=$limit, offset=$offset")

        // Reset lista acumulada se for uma nova busca/feed
        if (offset == 0 || forceRefresh) {
            feedChartsList.clear()
        }

        // Fetch from remote
        val remoteResult = remoteChartRepository.getChartsSortedBy(
            sortBy = sortBy,
            limit = limit,
            offset = offset
        ).first()

        remoteResult.fold(
            onSuccess = { remoteCharts ->
                // Acumula os charts
                feedChartsList.addAll(remoteCharts.filter { new ->
                    feedChartsList.none { it.id == new.id }
                })
                updateChartsInMemory(feedChartsList)
                emit(FetchResult.Success(feedChartsList))
            },
            onFailure = { error ->
                Log.e(TAG, "Failed to fetch feed charts", error)
                emit(FetchResult.Error(
                    error.message ?: context.getString(R.string.failed_to_fetch_feed_charts),
                    error
                ))
            }
        )
    }.catch { e ->
        Log.e(TAG, "Exception in fetchFeedCharts", e)
        emit(FetchResult.Error(context.getString(R.string.failed_to_fetch_feed_charts), e))
    }

    /**
     * Handle charts that have been deleted from remote
     */
    private suspend fun handleDeletedCharts(remoteCharts: List<Chart>) {
        val currentCharts = _charts.value
        val remoteIds = remoteCharts.map { it.id }.toSet()

        // Find non-installed charts that are no longer on remote
        val deletedCharts = currentCharts.values.filter { chart ->
            chart.isInstalled != true && chart.id !in remoteIds
        }

        if (deletedCharts.isNotEmpty()) {
            Log.d(TAG, "Removing ${deletedCharts.size} deleted charts from cache")

            // Remove from memory
            val updatedMap = currentCharts.toMutableMap()
            deletedCharts.forEach { updatedMap.remove(it.id) }
            _charts.value = updatedMap

            // Remove from local storage
            coroutineScope.launch {
                localChartRepository.deleteCharts(deletedCharts).first()
                Log.d(TAG, "Deleted charts removed from local storage")
            }
        }
    }

    /**
     * Search for charts with query and filters
     */
    fun searchCharts(
        query: String,
        difficulties: List<Difficulty>? = null,
        genres: List<Genre>? = null,
        limit: Int = 10,
        offset: Int = 0
    ): Flow<FetchResult<List<Chart>>> = flow {
        currentSearchQuery = query

        if (query.isEmpty()) {
            _searchResults.value = emptyList()
            emit(FetchResult.Success(emptyList()))
            return@flow
        }

        emit(FetchResult.Loading)

        val remoteResult = remoteChartRepository.getCharts(
            query = query,
            difficulties = difficulties,
            genres = genres,
            limit = limit,
            offset = offset
        ).first()

        remoteResult.fold(
            onSuccess = { searchResults ->
                Log.d(TAG, "Found ${searchResults.size} charts for query: $query")

                // Check if query is still current
                if (currentSearchQuery != query) {
                    Log.d(TAG, "Query changed, ignoring results")
                    return@flow
                }

                // Update charts in memory and search results
                updateChartsInMemory(searchResults)

                val currentSearchIds = if (offset == 0) {
                    searchResults.map { it.id }
                } else {
                    _searchResults.value + searchResults.map { it.id }
                }

                _searchResults.value = currentSearchIds
                emit(FetchResult.Success(searchResults))
            },
            onFailure = { error ->
                Log.e(TAG, "Search failed for query: $query", error)
                emit(FetchResult.Error(context.getString(R.string.search_failed), error))
            }
        )
    }.catch { e ->
        Log.e(TAG, "Exception in searchCharts", e)
        emit(FetchResult.Error(context.getString(R.string.search_failed), e))
    }

    /**
     * Check for updates to installed charts
     */
    fun checkForUpdates(): Flow<FetchResult<List<Chart>>> = flow {
        emit(FetchResult.Loading)

        val installedCharts = _charts.value.values.filter { it.isInstalled == true }

        if (installedCharts.isEmpty()) {
            emit(FetchResult.Success(emptyList<Chart>()))
            return@flow
        }

        val latestVersionsResult = remoteChartRepository
            .getLatestVersionsByChartIds(installedCharts.map { it.id })
            .first()

        latestVersionsResult.fold(
            onSuccess = { latestVersions ->
                val chartsWithUpdates = mutableListOf<Chart>()
                val versionMap = latestVersions.associateBy { it.chartId }

                installedCharts.forEach { chart ->
                    val remoteVersion = versionMap[chart.id]

                    if (remoteVersion != null && remoteVersion.index > chart.latestVersion.index) {
                        val updatedChart = chart.copy(availableVersion = remoteVersion)
                        updateChartInMemory(updatedChart)
                        chartsWithUpdates.add(updatedChart)
                    }
                }

                // Update local storage
                if (chartsWithUpdates.isNotEmpty()) {
                    coroutineScope.launch {
                        localChartRepository.updateCharts(chartsWithUpdates.toList()).first()
                    }
                }

                emit(FetchResult.Success(chartsWithUpdates.toList()))
                Log.d(TAG, "Found ${chartsWithUpdates.size} charts with updates")
            },
            onFailure = { error ->
                emit(FetchResult.Error(
                    context.getString(R.string.failed_to_check_for_updates),
                    error
                ))
            }
        )
    }.catch { e ->
        emit(FetchResult.Error(context.getString(R.string.failed_to_check_for_updates), e))
    }

    /**
     * Update a single chart with specific operation
     */
    fun updateChart(chartId: String, operation: OperationType): Flow<FetchResult<List<Chart>>> = flow {
        val existingChart = _charts.value[chartId] ?: run {
            emit(FetchResult.Error(context.getString(R.string.chart_not_found)))
            return@flow
        }

        // Update in local repository
        val result = localChartRepository.updateChart(chartId, operation).first()

        result.fold(
            onSuccess = { success ->
                if (!success) {
                    emit(FetchResult.Error(context.getString(R.string.failed_to_update)))
                    return@fold
                }

                // Update in-memory state
                val updatedChart = when (operation) {
                    OperationType.INSTALL -> existingChart.copy(isInstalled = true)
                    OperationType.UPDATE -> {
                        val availableVersion = existingChart.availableVersion
                        if (availableVersion == null) {
                            emit(FetchResult.Error(context.getString(R.string.no_available_version)))
                            return@fold
                        }
                        existingChart.copy(
                            latestVersion = availableVersion,
                            availableVersion = null
                        )
                    }
                    OperationType.DELETE -> existingChart.copy(isInstalled = false)
                }

                updateChartInMemory(updatedChart)
                emit(FetchResult.Success(getCurrentChartsList()))

                Log.d(TAG, "Successfully updated chart $chartId with operation $operation")
            },
            onFailure = { error ->
                emit(FetchResult.Error(context.getString(R.string.failed_to_update), error))
            }
        )
    }.catch { e ->
        emit(FetchResult.Error(context.getString(R.string.failed_to_update), e))
    }

    /**
     * Get search suggestions
     */
    fun getSuggestions(query: String): Flow<List<String>> = flow {
        if (query.isBlank()) {
            emit(emptyList())
            return@flow
        }

        val result = remoteChartRepository.getSuggestions(query).first()

        result.fold(
            onSuccess = { suggestions -> emit(suggestions) },
            onFailure = { emit(emptyList()) }
        )
    }.catch {
        emit(emptyList())
    }

    /**
     * Post analytics for chart operations
     */
    fun postAnalytics(chartId: String, operation: OperationType) {
        coroutineScope.launch {
            try {
                remoteChartRepository.postAnalytics(chartId, operation).collect { result ->
                    if (result.isSuccess) {
                        Log.i(TAG, "Analytics posted for $chartId: $operation")
                    } else {
                        Log.w(TAG, "Failed to post analytics for $chartId", result.exceptionOrNull())
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception posting analytics for $chartId", e)
            }
        }
    }

    // Private helper methods
    private fun updateChartInMemory(chart: Chart) {
        _charts.value = _charts.value.toMutableMap().apply {
            put(chart.id, chart)
        }
    }

    private fun updateChartsInMemory(charts: List<Chart>) {
        if (charts.isEmpty()) return

        _charts.value = _charts.value.toMutableMap().apply {
            charts.forEach { chart ->
                // Preserve installed status and available version if chart already exists
                val existing = this[chart.id]
                val updatedChart = if (existing?.isInstalled == true) {
                    chart.copy(
                        isInstalled = true,
                        availableVersion = existing.availableVersion
                    )
                } else {
                    chart
                }
                put(chart.id, updatedChart)
            }
        }

        // Apply cache limit for non-installed charts
        applyCacheLimit()

        Log.d(TAG, "Updated ${charts.size} charts in memory")
    }

    private fun replaceChartsInMemory(charts: List<Chart>) {
        _charts.value = charts.associateBy { it.id }
        Log.d(TAG, "Replaced all charts in memory with ${charts.size} charts")
    }

    private fun applyCacheLimit() {
        val currentCharts = _charts.value
        val installed = currentCharts.values.filter { it.isInstalled == true }
        val nonInstalled = currentCharts.values.filter { it.isInstalled != true }

        if (nonInstalled.size > MAX_CACHED_CHARTS) {
            val limitedNonInstalled = nonInstalled.takeLast(MAX_CACHED_CHARTS)
            val finalCharts = (installed + limitedNonInstalled).associateBy { it.id }
            _charts.value = finalCharts

            Log.d(TAG, "Applied cache limit: ${installed.size} installed + ${limitedNonInstalled.size} non-installed")
        }
    }

    private fun getCurrentChartsList(): List<Chart> = _charts.value.values.toList()
}