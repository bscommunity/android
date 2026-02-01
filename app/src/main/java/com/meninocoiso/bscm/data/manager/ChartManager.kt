package com.meninocoiso.bscm.data.manager

import android.content.Context
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
import com.meninocoiso.bscm.domain.repository.ChartRemoteRepository
import com.meninocoiso.bscm.domain.repository.ChartQuery
import com.meninocoiso.bscm.domain.result.ContentResult
import com.meninocoiso.bscm.domain.result.ContentState
import dagger.hilt.android.qualifiers.ApplicationContext
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
    @param:ApplicationContext private val context: Context,
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
    val feedCharts: Flow<List<Chart>> = contentManager.feedContent.map { charts ->
        charts.filterNot { chart -> chart.isInstalled == true }
    }
    val installedCharts: Flow<List<Chart>> = contentManager.installedContent
    val pendingUpdateCharts: Flow<List<Chart>> = installedCharts.map { chartList ->
        chartList.filter { it.availableVersion != null }
    }
    val searchCharts: Flow<List<Chart>> = contentManager.searchContent

    fun getChartsLength(): Int = memoryStore.contentById.value.size

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
                val versionMap = versions.associateBy { it.chartId }
                val updated = installed.mapNotNull { chart ->
                    val remoteVersion = versionMap[chart.id]
                    if (remoteVersion != null && remoteVersion.index > chart.latestVersion.index) {
                        chart.copy(availableVersion = remoteVersion)
                    } else null
                }
                if (updated.isNotEmpty()) {
                    memoryStore.upsertContent(updated) { it.id }
                    coroutineScope.launch { localChartRepository.update(updated).first() }
                }
                emit(ContentResult.Success(updated))
            },
            onFailure = { err -> emit(ContentResult.Error(context.getString(R.string.failed_to_check_for_updates), err)) }
        )
    }

    fun updateChart(chartId: String, operation: OperationOption): Flow<ContentResult<List<Chart>>> =
        contentManager.updateContent(chartId, operation) { existing, op ->
            when (op) {
                OperationOption.INSTALL -> Result.success(existing.copy(isInstalled = true))
                OperationOption.UPDATE -> existing.availableVersion?.let {
                    Result.success(existing.copy(latestVersion = it, availableVersion = null))
                } ?: Result.failure(IllegalStateException(context.getString(R.string.no_available_version)))
                OperationOption.DELETE -> Result.success(existing.copy(isInstalled = false))
            }
        }

    fun getChart(chartId: String): Flow<ContentResult<Chart>> = contentManager.getItem(chartId)

    fun getSuggestions(query: String): Flow<List<String>> = contentManager.getSuggestions(query)

    fun postAnalytics(chartId: String, operation: OperationOption) {
        coroutineScope.launch { contentManager.postAnalytics(chartId, operation).first() }
    }

    suspend fun scanLocalCharts(rootUri: Uri) {
        syncInstalledCharts(rootUri)
    }

    private suspend fun syncInstalledCharts(rootUri: Uri) {
        try {
            val installedEntries = chartStorageScanner.scanInstalledContent(rootUri)
            if (installedEntries.isEmpty()) return

            val current = memoryStore.contentById.value.values.toList()
            val updatedCharts = current.map { chart ->
                val isInstalled = chart.id in installedEntries.keys
                Log.d(TAG, "Chart ${chart.id} installed status: ${chart.isInstalled} -> $isInstalled")
                if (chart.isInstalled == isInstalled) chart else chart.copy(isInstalled = isInstalled)
            }
            persistInstalledChanges(current, updatedCharts)
            memoryStore.upsertContent(updatedCharts) { it.id }

            val existingIds = current.map { it.id }.toSet()
            Log.d(TAG, "Sync installed charts: found ${installedEntries.size} installed, ${existingIds.size} existing in memory")
            val missingEntries = installedEntries.filterKeys { it !in existingIds }
            Log.d(TAG, "Found ${missingEntries.size} missing installed charts to hydrate")
            if (missingEntries.isNotEmpty()) {
                val orphan = hydrateMissingInstalledCharts(missingEntries)
                if (orphan.isNotEmpty()) memoryStore.addWithoutAffectingFeed(orphan, getId = { it.id })
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing installed charts", e)
        }
    }

    private fun hydrateMissingInstalledCharts(
        entries: Map<String, InstalledContentEntry<ExternalContentMetadata>>
    ): List<Chart> {
        if (entries.isEmpty()) return emptyList()
        val hydrated = mutableListOf<Chart>()
        for ((chartId, entry) in entries) {
            try {
                val metadata = entry.metadata
                val config = entry.config
                if (metadata != null && config is ExternalContentConfig) {
                    hydrated.add(chartPlaceholderFactory.createPlaceholderChart(metadata, config))
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to hydrate chart $chartId", e)
            }
        }
        return hydrated
    }

    private suspend fun persistInstalledChanges(original: List<Chart>, updated: List<Chart>) {
        if (original.size != updated.size) return
        val changed = updated.mapIndexedNotNull { index, newChart ->
            val oldChart = original[index]
            if (oldChart.isInstalled != newChart.isInstalled && !isLocalOnlyChart(newChart)) newChart else null
        }
        if (changed.isNotEmpty()) {
            val result = localChartRepository.update(changed).first()
            if (result.isFailure) {
                Log.e(TAG, "Failed to persist installed changes", result.exceptionOrNull())
            }
        }
    }

    private fun isLocalOnlyChart(chart: Chart): Boolean = chart.contentId == null
}
