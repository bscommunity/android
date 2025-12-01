package com.meninocoiso.bscm.data.manager

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.data.repository.CacheRepository
import com.meninocoiso.bscm.di.ApplicationScope
import com.meninocoiso.bscm.domain.enums.Difficulty
import com.meninocoiso.bscm.domain.enums.Genre
import com.meninocoiso.bscm.domain.enums.OperationOption
import com.meninocoiso.bscm.domain.enums.SortOption
import com.meninocoiso.bscm.domain.model.Chart
import com.meninocoiso.bscm.domain.model.Contributor
import com.meninocoiso.bscm.domain.model.StreamingLink
import com.meninocoiso.bscm.domain.model.Version
import com.meninocoiso.bscm.domain.repository.ChartRepository
import com.meninocoiso.bscm.util.StorageUtils
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
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDateTime
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

@Serializable
private data class ExternalChartInfo(
    val title: String,
    val artist: String,
    val id: Long,
    val difficulty: Int? = null,
    val bpm: Int? = null,
    val sections: Int? = null,
    val maxScore: Int? = null,
    val numLanes: Int? = null,
    val type: String? = null,
    val version: Int? = null
)

private data class InstalledChartEntry(
    val chartId: String,
    val info: ExternalChartInfo?,
    val folder: DocumentFile
)

private data class InstalledSyncResult(
    val orderedCharts: List<Chart>,
    val orphanCharts: List<Chart> = emptyList()
)
/**
 * Singleton manager for chart data across the application
 */
@Singleton
class ChartManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:Named("Remote") private val remoteChartRepository: ChartRepository,
    @param:Named("Local") private val localChartRepository: ChartRepository,
    private val cacheRepository: CacheRepository,
    @param:ApplicationScope private val coroutineScope: CoroutineScope)
{
    private val json = Json { ignoreUnknownKeys = true }
    private val ioDispatcher = Dispatchers.IO

    // Single source of truth for all charts in memory
    private val _charts = MutableStateFlow<Map<String, Chart>>(emptyMap())
    private val charts: StateFlow<Map<String, Chart>> = _charts.asStateFlow()

    private val _feedOrder = MutableStateFlow<List<String>>(emptyList())
    private val feedOrder: StateFlow<List<String>> = _feedOrder.asStateFlow()

    // Search-specific state
    private val _searchResults = MutableStateFlow<List<String>>(emptyList()) // Chart IDs
    private val searchResults: StateFlow<List<String>> = _searchResults.asStateFlow()

    private val _cacheState = MutableStateFlow<ChartState>(ChartState.Loading)
    val cacheState: StateFlow<ChartState> = _cacheState.asStateFlow()

    var currentSearchQuery: String = ""
        private set

    // Derived flows for different chart collections
    val memoryCharts: Flow<List<Chart>> = combine(feedOrder, charts) { order, chartMap ->
        if (order.isEmpty()) {
            chartMap.values.toList()
        } else {
            val feedCharts = order.mapNotNull { chartMap[it] }
            val installedOnly = chartMap.values.filter { it.isInstalled == true && it.id !in order }
            feedCharts + installedOnly
        }
    }

    val installedCharts: Flow<List<Chart>> = charts.map { chartMap ->
        chartMap.values.filter { it.isInstalled == true }
    }

    val chartsWithUpdates: Flow<List<Chart>> = charts.map { chartMap ->
        chartMap.values.filter { it.isInstalled == true && it.availableVersion != null }
    }

    val searchCharts: Flow<List<Chart>> = combine(searchResults, charts) { searchIds, chartMap ->
        searchIds.mapNotNull { chartMap[it] }
    }

    fun updateState(newState: ChartState) {
        // Log.d(TAG, "Updating cache state to $newState")
        _cacheState.value = newState
    }

    fun getChartsLength(): Int = _charts.value.size

    private suspend fun syncInstalledCharts(
        chartsToVerify: List<Chart>,
        rootUri: Uri
    ): InstalledSyncResult {
        return try {
            val installedEntries = scanInstalledChartEntries(rootUri)
            if (installedEntries.isEmpty()) {
                val clearedCharts = chartsToVerify.map { it.copy(isInstalled = false) }
                persistInstalledChanges(chartsToVerify, clearedCharts)
                InstalledSyncResult(clearedCharts)
            } else {
                val installedIds = installedEntries.keys
                val updatedCharts = chartsToVerify.map { chart ->
                    val isInstalled = chart.id in installedIds
                    if (chart.isInstalled == isInstalled) chart else chart.copy(isInstalled = isInstalled)
                }

                persistInstalledChanges(chartsToVerify, updatedCharts)

                val existingIds = chartsToVerify.map { it.id }.toSet()
                val missingEntries = installedEntries.filterKeys { id ->
                    id !in existingIds
                }

                val orphanCharts = hydrateMissingInstalledCharts(missingEntries)
                if (orphanCharts.isNotEmpty()) {
                    val insertResult = localChartRepository.insertCharts(orphanCharts).first()
                    if (insertResult.isFailure) {
                        Log.e(TAG, "Failed to insert orphan charts", insertResult.exceptionOrNull())
                    }
                }

                InstalledSyncResult(updatedCharts, orphanCharts)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error verifying installed charts", e)
            coroutineScope.launch {
                cacheRepository.setFolderUri("")
            }
            InstalledSyncResult(chartsToVerify)
        }
    }

    /**
     * Loads charts from local cache into memory
     */
    suspend fun loadCachedCharts(sortBy: SortOption, rootUri: Uri? = null) {
        _cacheState.value = ChartState.Loading

        try {
            val cachedCharts = localChartRepository.getChartsSortedBy(sortBy, limit = null).first()

            cachedCharts.fold(
                onSuccess = { allCharts ->
                    Log.d(TAG, "Loading ${allCharts.size} charts from cache")

                    val syncResult = if (rootUri != null) {
                        syncInstalledCharts(allCharts, rootUri)
                    } else {
                        InstalledSyncResult(allCharts)
                    }

                    // Replace all charts in memory
                    replaceFeedCharts(syncResult.orderedCharts)

                    if (syncResult.orphanCharts.isNotEmpty()) {
                        addChartsWithoutAffectingFeed(syncResult.orphanCharts)
                    }
                    _cacheState.value = ChartState.Success

                    Log.d(TAG, "Successfully loaded ${syncResult.orderedCharts.size} charts from cache")
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

        /*// Check cache for initial load
        if (offset == 0 && !forceRefresh) {
            val cachedResult = localChartRepository.getChartsSortedBy(sortBy, limit).first()

            cachedResult.fold(
                onSuccess = { cached ->
                    if (cached.isNotEmpty()) {
                        Log.d(TAG, "Using ${cached.size} cached charts for feed")
                        updateChartsInMemory(cached)
                        emit(FetchResult.Success(cached))
                        return@flow
                    }
                },
                onFailure = { *//* Continue to remote fetch *//* }
            )
        }*/

        // Fetch from remote
        val remoteResult = remoteChartRepository.getChartsSortedBy(
            sortBy = sortBy,
            limit = limit,
            offset = offset
        ).first()

        remoteResult.fold(
            onSuccess = { remoteCharts ->
                Log.d(TAG, "Fetched ${remoteCharts.size} charts from remote")
                // Log.d(TAG, "Charts: $remoteCharts")

                if (offset == 0) {
                    // Initial load - update cache
                    replaceFeedCharts(remoteCharts)
                } else {
                    // Pagination - merge with existing while preserving order
                    appendFeedCharts(remoteCharts)
                }

                // Update local storage in background with the sanitized charts currently in memory
                if (remoteCharts.isNotEmpty()) {
                    val chartsToPersist = remoteCharts.mapNotNull { _charts.value[it.id] }
                    coroutineScope.launch {
                        localChartRepository.updateCharts(chartsToPersist).first()
                        Log.d(TAG, "Persisted ${chartsToPersist.size} charts in local storage")
                    }
                }

                emit(FetchResult.Success(remoteCharts))
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
                addChartsWithoutAffectingFeed(searchResults)

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
    fun updateChart(chartId: String, operation: OperationOption): Flow<FetchResult<List<Chart>>> = flow {
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
                    OperationOption.INSTALL -> existingChart.copy(isInstalled = true)
                    OperationOption.UPDATE -> {
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
                    OperationOption.DELETE -> existingChart.copy(isInstalled = false)
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
    fun postAnalytics(chartId: String, operation: OperationOption) {
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
    private suspend fun scanInstalledChartEntries(rootUri: Uri): Map<String, InstalledChartEntry> {
        return withContext(ioDispatcher) {
            val entries = mutableMapOf<String, InstalledChartEntry>()
            try {
                val destination = StorageUtils.getFolder(rootUri, listOf("songs"), context)
                destination.listFiles()?.forEach { folder ->
                    if (!folder.isDirectory) return@forEach
                    val infoFile = folder.findFile("info.json") ?: return@forEach
                    val info = readExternalChartInfo(infoFile)
                    val chartId = info?.id?.toString()
                        ?: folder.name?.substringAfterLast('_')
                    if (!chartId.isNullOrBlank()) {
                        entries[chartId] = InstalledChartEntry(chartId, info, folder)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Unable to scan installed charts", e)
                entries.clear()
            }
            entries.toMap()
        }
    }

    private fun readExternalChartInfo(file: DocumentFile): ExternalChartInfo? {
        return try {
            context.contentResolver.openInputStream(file.uri)?.bufferedReader()?.use { reader ->
                json.decodeFromString<ExternalChartInfo>(reader.readText())
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse info.json for ${file.name}", e)
            null
        }
    }

    private suspend fun hydrateMissingInstalledCharts(
        entries: Map<String, InstalledChartEntry>
    ): List<Chart> {
        if (entries.isEmpty()) return emptyList()

        val hydrated = mutableListOf<Chart>()
        for ((chartId, entry) in entries) {
            val remoteChart = fetchRemoteChart(chartId)
            val chart = when {
                remoteChart != null -> remoteChart.copy(isInstalled = true)
                entry.info != null -> createPlaceholderChart(entry.info)
                else -> null
            }

            if (chart != null) {
                hydrated += chart
            }
        }
        return hydrated
    }

    private suspend fun fetchRemoteChart(chartId: String): Chart? {
        return try {
            remoteChartRepository.getChart(chartId).first().getOrNull()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch remote chart $chartId", e)
            null
        }
    }

    private suspend fun persistInstalledChanges(
        originalCharts: List<Chart>,
        updatedCharts: List<Chart>
    ) {
        if (originalCharts.size != updatedCharts.size) return

        val changedCharts = updatedCharts.mapIndexedNotNull { index, updated ->
            val original = originalCharts[index]
            if (original.isInstalled != updated.isInstalled) updated else null
        }

        if (changedCharts.isNotEmpty()) {
            val result = localChartRepository.updateCharts(changedCharts).first()
            if (result.isFailure) {
                Log.e(TAG, "Failed to persist installed changes", result.exceptionOrNull())
            }
        }
    }

    private fun createPlaceholderChart(info: ExternalChartInfo): Chart {
        val chartId = info.id.toString()
        val now = LocalDateTime.now()
        return Chart(
            artist = info.artist,
            track = info.title,
            album = null,
            genre = null,
            trackUrls = emptyList<StreamingLink>(),
            trackPreviewUrl = "",
            id = chartId,
            contentId = chartId,
            coverUrl = "",
            isFeatured = false,
            downloadsSum = 0,
            latestPublishedAt = now,
            isLiked = false,
            isFavorited = false,
            isInstalled = true,
            latestVersion = createPlaceholderVersion(chartId, info),
            availableVersion = null,
            contributors = emptyList<Contributor>()
        )
    }

    private fun createPlaceholderVersion(
        chartId: String,
        info: ExternalChartInfo
    ): Version {
        val now = LocalDateTime.now()
        return Version(
            id = -chartId.hashCode().toLong(),
            chartId = chartId,
            index = info.version ?: 1,
            duration = 0f,
            notesAmount = info.sections ?: 0,
            effectsAmount = 0,
            bpm = info.bpm ?: 0,
            difficulty = mapDifficulty(info.difficulty),
            isDeluxe = false,
            isExplicit = false,
            bundleUrl = "",
            previewUrl = null,
            downloadsAmount = 0,
            knownIssues = emptyList(),
            publishedAt = now
        )
    }

    private fun mapDifficulty(value: Int?): Difficulty {
        if (value == null) return Difficulty.NORMAL
        val difficulties = Difficulty.entries.toTypedArray()
        val index = value.coerceIn(0, difficulties.size - 1)
        return difficulties.getOrNull(index) ?: Difficulty.NORMAL
    }

    private fun updateChartInMemory(chart: Chart) {
        upsertCharts(listOf(chart))
    }

    private fun upsertCharts(charts: List<Chart>) {
        if (charts.isEmpty()) return

        val updated = _charts.value.toMutableMap().apply {
            charts.forEach { chart ->
                val existing = this[chart.id]
                val chartToStore = if (existing?.isInstalled == true && chart.isInstalled != true) {
                    chart.copy(
                        isInstalled = true,
                        availableVersion = existing.availableVersion
                    )
                } else {
                    chart
                }
                put(chart.id, chartToStore)
            }
        }

        _charts.value = updated
    }

    private fun replaceFeedCharts(charts: List<Chart>) {
        upsertCharts(charts)
        _feedOrder.value = charts.map { it.id }
        applyCacheLimit()
    }

    private fun appendFeedCharts(charts: List<Chart>) {
        if (charts.isEmpty()) return

        upsertCharts(charts)
        val appendIds = charts.map { it.id }
        if (appendIds.isNotEmpty()) {
            _feedOrder.value = (_feedOrder.value + appendIds).distinct()
            applyCacheLimit()
        }
    }

    private fun addChartsWithoutAffectingFeed(charts: List<Chart>) {
        upsertCharts(charts)
    }

    private fun applyCacheLimit() {
        val chartMap = _charts.value
        if (chartMap.isEmpty()) return

        val nonInstalledFeedIds = _feedOrder.value.filter { id ->
            chartMap[id]?.isInstalled != true
        }

        if (nonInstalledFeedIds.size <= MAX_CACHED_CHARTS) return

        val idsToKeep = nonInstalledFeedIds.take(MAX_CACHED_CHARTS)
        val keepSet = idsToKeep.toSet()
        val idsToDrop = nonInstalledFeedIds.filterNot { it in keepSet }

        if (idsToDrop.isEmpty()) return

        val updatedOrder = _feedOrder.value.filterNot { it in idsToDrop }
        val updatedMap = chartMap.toMutableMap().apply {
            idsToDrop.forEach { id ->
                val chart = this[id]
                if (chart?.isInstalled != true) {
                    remove(id)
                }
            }
        }

        _feedOrder.value = updatedOrder
        _charts.value = updatedMap

        Log.d(TAG, "Applied cache limit: keeping ${idsToKeep.size} non-installed charts")
    }

    private fun getCurrentChartsList(): List<Chart> {
        val chartMap = _charts.value
        return if (_feedOrder.value.isEmpty()) {
            chartMap.values.toList()
        } else {
            val ordered = _feedOrder.value.mapNotNull { chartMap[it] }
            val leftovers = chartMap.values.filter { it.isInstalled == true && it.id !in _feedOrder.value }
            ordered + leftovers
        }
    }
}