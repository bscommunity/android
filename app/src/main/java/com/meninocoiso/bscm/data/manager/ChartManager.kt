package com.meninocoiso.bscm.data.manager

import android.net.Uri
import android.util.Log
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.data.mapper.ChartPlaceholderFactory
import com.meninocoiso.bscm.data.parser.ExternalContentConfig
import com.meninocoiso.bscm.data.parser.ExternalContentMetadata
import com.meninocoiso.bscm.data.service.ChartStorageScanner
import com.meninocoiso.bscm.di.ApplicationScope
import com.meninocoiso.bscm.domain.enums.OperationOption
import com.meninocoiso.bscm.domain.enums.SortOption
import com.meninocoiso.bscm.domain.model.Chart
import com.meninocoiso.bscm.domain.model.internal.InstalledContentEntry
import com.meninocoiso.bscm.domain.repository.ChartLocalRepository
import com.meninocoiso.bscm.domain.repository.ChartQuery
import com.meninocoiso.bscm.domain.repository.ChartRemoteRepository
import com.meninocoiso.bscm.domain.result.ContentResult
import com.meninocoiso.bscm.domain.result.ContentState
import com.meninocoiso.bscm.domain.result.UiText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ChartManager"

/**
 * Chart-specific manager that handles chart-specific operations.
 * Delegates generic content operations to ContentManager.
 */
@Singleton
class ChartManager @Inject constructor(
    @param:ApplicationScope private val coroutineScope: CoroutineScope,
    private val remoteChartRepository: ChartRemoteRepository,
    private val localChartRepository: ChartLocalRepository,
    private val chartStorageScanner: ChartStorageScanner,
    private val chartPlaceholderFactory: ChartPlaceholderFactory,
    private val memoryStore: ContentMemoryStore<Chart>,
    private val contentManager: ContentManager<Chart, SortOption, ChartQuery>
) {
    // Expose ContentManager states
    val cacheState: StateFlow<ContentState> = contentManager.cacheState
    val feedState: StateFlow<ContentState> = contentManager.feedState

    // Expose ContentManager flows
    val feedCharts: Flow<List<Chart>> = contentManager.feedContent
    val installedCharts: Flow<List<Chart>> = contentManager.installedContent
    val pendingUpdateCharts: Flow<List<Chart>> = installedCharts.map { chartList ->
        chartList.filter { it.availableVersion != null }
    }
    val searchCharts: Flow<List<Chart>> = contentManager.searchContent

    fun getChartsLength(): Int = memoryStore.contentById.value.size

    /**
     * Synchronous lookup of a chart in the in-memory store. Lets install state
     * be resolved on the very first frame of a details screen without waiting
     * for a database read.
     */
    fun getChartFromStore(id: String): Chart? = memoryStore.contentById.value[id]

    fun updateCacheState(newState: ContentState) = contentManager.updateCacheState(newState)
    fun updateFeedState(newState: ContentState) = contentManager.updateFeedState(newState)

    // Delegate generic operations to ContentManager
    suspend fun loadCachedCharts(sortBy: SortOption) = contentManager.loadCachedContent(sortBy)

    fun fetchFeedCharts(
        sortBy: SortOption,
        forceRefresh: Boolean = false,
        limit: Int = 10,
        offset: Int = 0,
        filters: ChartQuery? = null
    ): Flow<ContentResult<List<Chart>>> =
        contentManager.fetchFeed(sortBy, forceRefresh, limit, offset, filters)

    fun searchCharts(
        query: String,
        sortBy: SortOption? = null,
        limit: Int = 10,
        offset: Int = 0,
        filters: ChartQuery? = null
    ): Flow<ContentResult<List<Chart>>> =
        contentManager.search(query, sortBy, limit, offset, filters)

    fun getChartsById(ids: List<String>): Flow<ContentResult<List<Chart>>> = contentManager.getItemsById(ids)

    suspend fun persistCharts(charts: List<Chart>) {
        if (charts.isEmpty()) return
        memoryStore.addWithoutAffectingFeed(charts, getId = { it.id })
        val result = localChartRepository.insert(charts).first()
        if (result.isFailure) {
            Log.e(TAG, "Failed to persist charts", result.exceptionOrNull())
        }
    }

    suspend fun updateContentById(
        internalId: String,
        operation: OperationOption
    ): ContentResult<Chart> = contentManager.updateContent(internalId, operation)

    fun getChart(id: String): Flow<ContentResult<Chart>> =
        contentManager.getItem(id)

    fun getSuggestions(query: String): Flow<List<String>> = contentManager.getSuggestions(query)

    fun postAnalytics(chartId: String, operation: OperationOption) {
        coroutineScope.launch { contentManager.postAnalytics(chartId, operation).first() }
    }

    // Chart-specific operations that require ChartRepository methods
    fun checkForUpdates(): Flow<ContentResult<List<Chart>>> = flow {
        emit(ContentResult.Loading)
        val installed = memoryStore.contentById.value.values.filter { it.isInstalled == true && !isLocalOnlyChart(it) }
        if (installed.isEmpty()) {
            emit(ContentResult.Success(emptyList()))
            return@flow
        }
        val latestVersionsResult = remoteChartRepository.getLatestVersionsByChartIds(installed.map { it.id }).first()
        latestVersionsResult.fold(
            onSuccess = { versions ->
                Log.d(TAG, "Fetched latest versions for ${versions.size} charts from remote")
                val versionMap = versions.associateBy { it.catalogItemId }
                val updated = installed.mapNotNull { chart ->
                    val remoteVersion = versionMap[chart.id]
                    if (remoteVersion != null && chart.latestVersion?.let { remoteVersion.createdAt > it.createdAt } == true) {
                        chart.copy(availableVersion = remoteVersion)
                    } else null
                }
                if (updated.isNotEmpty()) {
                    memoryStore.upsertContent(updated) { it.id }
                    coroutineScope.launch { localChartRepository.update(updated).first() }
                }
                emit(ContentResult.Success(updated))
            },
            onFailure = { err ->
                emit(
                    ContentResult.Error(
                        err.message?.let { UiText.Plain(it) }
                            ?: UiText.Res(R.string.failed_to_check_for_updates),
                        err
                    )
                )
            }
        )
    }

    suspend fun scanLocalCharts(rootUri: Uri) {
        try {
            // Scan local storage for installed charts
            val installedEntries = chartStorageScanner.scanInstalledContent(rootUri)
            Log.d(TAG, "Scanned local storage: found ${installedEntries.size} folders in songs")

            // Update existing charts with installed status, and persist any changes to the local repository
            val installedIds = installedEntries.values.mapNotNull { it.id }.toSet()
            val current = memoryStore.contentById.value.values.toList()

            val updatedCharts = current.map { chart ->
                val isInstalled = shouldMarkInstalled(chart, installedIds)
                Log.d(
                    TAG,
                    "Chart ${chart.id} installed status: ${chart.isInstalled} -> $isInstalled"
                )
                if (chart.isInstalled == isInstalled) chart else chart.copy(isInstalled = isInstalled)
            }
            persistInstalledChanges(current, updatedCharts)
            memoryStore.upsertContent(updatedCharts) { it.id }

            // Identify any installed charts that are missing from memory and attempt to hydrate them from storage metadata
            val idsToHydrate = findMissingIdsToHydrate(installedIds, current)
            Log.d(
                TAG,
                "Sync installed charts: ${installedIds.size} ids to match, ${idsToHydrate.size} canonical charts to hydrate"
            )

            // Hydrate any missing canonical charts
            val hydratedCharts = if (idsToHydrate.isNotEmpty()) {
                hydrateMissingInstalledCharts(idsToHydrate)
            } else {
                emptyList()
            }
            if (hydratedCharts.isNotEmpty()) {
                memoryStore.addWithoutAffectingFeed(hydratedCharts, getId = { it.id })

                val persistResult = localChartRepository.insert(hydratedCharts).first()
                if (persistResult.isFailure) {
                    Log.e(TAG, "Failed to persist hydrated charts", persistResult.exceptionOrNull())
                } else {
                    Log.d(TAG, "Successfully persisted ${hydratedCharts.size} hydrated charts")
                }
            }

            // Add local placeholders for any installed entries that couldn't be matched
            // to existing or hydrated charts, so they still show up offline/unknown.
            val hydratedIds = hydratedCharts.mapTo(mutableSetOf()) { it.id }
            val placeholders = createLocalPlaceholders(installedEntries, current, hydratedIds)
            if (placeholders.isNotEmpty()) {
                memoryStore.addWithoutAffectingFeed(placeholders, getId = { it.id })

                // Persist placeholders so they survive navigation and process death.
                val persistResult = localChartRepository.insert(placeholders).first()
                if (persistResult.isFailure) {
                    Log.e(TAG, "Failed to persist local placeholders", persistResult.exceptionOrNull())
                } else {
                    Log.d(TAG, "Persisted ${placeholders.size} local placeholders")
                }
            }

            val totalInstalled = memoryStore.contentById.value.values.count { it.isInstalled == true }
            Log.d(TAG, "Scan completed! Total installed charts in memory: $totalInstalled")
            updateCacheState(ContentState.Success)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing installed charts", e)
            updateCacheState(ContentState.Error)
        }
    }

    suspend fun clearCache() {
        memoryStore.clearAll()
        updateFeedState(ContentState.Loading)
    }

    private suspend fun hydrateMissingInstalledCharts(ids: Collection<String>): List<Chart> {
        if (ids.isEmpty()) return emptyList()

        val hydrated = mutableListOf<Chart>()
        // The batch endpoint ignores the ids (it is the feed endpoint), so the
        // per-id endpoint is the only one that resolves the actual chart.
        for (id in ids) {
            val result = runCatching { remoteChartRepository.getItem(id).first() }.getOrNull()
            result?.fold(
                onSuccess = { chart ->
                    // These charts were found on disk, so their install state is local
                    // device state that the server cannot know about. Mark them installed
                    // so they surface in the installed charts and installed tour passes.
                    hydrated.add(chart.copy(isInstalled = true))
                },
                onFailure = { err ->
                    Log.d(TAG, "Failed to hydrate chart $id", err)
                }
            )
        }
        if (hydrated.isNotEmpty()) {
            Log.d(TAG, "Hydrated ${hydrated.size} charts from remote for missing ids")
        }

        return hydrated
    }

    private fun createLocalPlaceholders(
        entries: Map<String, InstalledContentEntry<ExternalContentMetadata>>,
        currentCharts: List<Chart>,
        hydratedIds: Set<String>
    ): List<Chart> {
        if (entries.isEmpty()) return emptyList()

        val existingById = currentCharts.associateBy { it.id }
        return entries.values.mapNotNull { entry ->
            val metadata = entry.metadata ?: return@mapNotNull null
            val metaId = metadata.id.takeIf { it.isNotBlank() } ?: return@mapNotNull null

            // Skip entries that were matched to a real chart (existing in memory
            // or successfully hydrated from the server).
            if (!entry.id.isNullOrBlank() && entry.id in hydratedIds) return@mapNotNull null

            // The placeholder is keyed by the resolved id (manifest/folder based),
            // which is the canonical chart id, so it can be rebound into a real
            // chart later without leaving a ghost duplicate behind.
            val localId = entry.id ?: metaId
            if (localId in existingById) return@mapNotNull null
            val placeholderMetadata =
                if (entry.id == null || entry.id == metaId) metadata else metadata.copy(id = entry.id)

            try {
                val config = entry.config as? ExternalContentConfig
                chartPlaceholderFactory.createPlaceholderChart(placeholderMetadata, config)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to create local placeholder for ${entry.folder.name}", e)
                null
            }
        }
    }

    private fun persistInstalledChanges(original: List<Chart>, updated: List<Chart>) {
        if (original.size != updated.size) return
        val changed = updated.mapIndexedNotNull { index, newChart ->
            val oldChart = original[index]
            if (oldChart.isInstalled != newChart.isInstalled && !isLocalOnlyChart(newChart)) newChart else null
        }
        if (changed.isNotEmpty()) {
            coroutineScope.launch {
                val result = localChartRepository.update(changed).first()
                if (result.isFailure) {
                    Log.e(TAG, "Failed to persist installed changes", result.exceptionOrNull())
                }
            }
        }
    }

    private fun isLocalOnlyChart(chart: Chart): Boolean = false

    companion object {
        internal fun shouldMarkInstalled(
            chart: Chart,
            installedIds: Set<String>
        ): Boolean = chart.id in installedIds

        internal fun findMissingIdsToHydrate(
            installedIds: Set<String>,
            currentCharts: List<Chart>
        ): Set<String> {
            val existingIds = currentCharts.map { it.id }.toSet()
            return installedIds.filterNot { it in existingIds }.toSet()
        }
    }
}
