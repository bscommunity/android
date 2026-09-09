package com.meninocoiso.bscm.data.repository

import android.util.Log
import androidx.room.withTransaction
import com.meninocoiso.bscm.data.local.AppDatabase
import com.meninocoiso.bscm.data.local.dao.CollectionDao
import com.meninocoiso.bscm.data.local.dao.TourPassDao
import com.meninocoiso.bscm.data.manager.ChartStateMerger
import com.meninocoiso.bscm.data.manager.InteractionQueueManager
import com.meninocoiso.bscm.data.remote.ApiClient
import com.meninocoiso.bscm.data.remote.dto.user.SectionCounts
import com.meninocoiso.bscm.domain.enums.CollectionKind
import com.meninocoiso.bscm.domain.enums.CatalogItemType
import com.meninocoiso.bscm.domain.model.CatalogItem
import com.meninocoiso.bscm.domain.model.Chart
import com.meninocoiso.bscm.domain.model.Collection
import com.meninocoiso.bscm.domain.model.CollectionItemCrossRef
import com.meninocoiso.bscm.domain.model.TourPass
import com.meninocoiso.bscm.domain.repository.CollectionRepository
import com.meninocoiso.bscm.presentation.viewmodel.profile.PagedResult
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

private const val TAG = "CollectionRepositoryRemote"

class CollectionRepositoryRemote @Inject constructor(
    private val apiClient: ApiClient,
    private val appDatabase: AppDatabase,
    private val collectionDao: CollectionDao,
    private val tourPassDao: TourPassDao,
    private val profileCacheRepository: ProfileCacheRepository,
    private val queueManager: InteractionQueueManager,
    private val chartStateMerger: ChartStateMerger,
) : CollectionRepository {
    override suspend fun getUserCollections(
        userId: String,
        limit: Int,
        offset: Int,
        useCache: Boolean
    ): Result<PagedResult<Collection>> = runCatching {
        Log.d(TAG, "Getting collections for user $userId (limit=$limit, offset=$offset, useCache=$useCache)")

        if (userId == "user" && useCache && offset == 0) {
            val localCollections = collectionDao.getUserCollections(limit, offset)
            if (localCollections.isNotEmpty()) {
                Log.d(TAG, "Returning owner collections from Room (${localCollections.size} items)")
                // Fall back to the Room list size when no quick-cache total was ever
                // stored (e.g. first run after creating a collection), so the profile
                // never reports a null/0 count while only Room data is available.
                val cachedTotal = profileCacheRepository.getCollections(userId).total
                    ?: localCollections.size
                return@runCatching PagedResult(
                    mergeLocalItemCounts(localCollections),
                    cachedTotal
                )
            }
        }

        // Other profiles: quick cache by IDs + Room hydration
        if (userId != "user" && useCache && offset == 0) {
            val cachedIds = profileCacheRepository.getCollections(userId)
            if (cachedIds.items.isNotEmpty()) {
                val cachedCollections = collectionDao.getCollectionsByIds(cachedIds.items)
                    .sortedBy { cachedIds.items.indexOf(it.id) }
                if (cachedCollections.isNotEmpty()) {
                    Log.d(TAG, "Returning cached collections for user $userId (${cachedCollections.size} items)")
                    return@runCatching PagedResult(
                        mergeLocalItemCounts(cachedCollections),
                        cachedIds.total
                    )
                }
            }
        }

        if (userId == "user" && offset == 0) {
            queueManager.syncPendingInteractionsBeforeRefresh()
        }

        // Fetch from API
        val page = if (userId == "user") {
            apiClient.getMyCollections(limit, offset)
        } else {
            apiClient.getUserCollections(userId, limit, offset)
        }
        val collections = page.items
        Log.d(TAG, "Fetched collections for user $userId from API (${collections.size} items)")

        val userCollections = collections.filter { it.kind == CollectionKind.USER }
        if (userCollections.isNotEmpty()) {
            collectionDao.upsertCollections(userCollections)
        }

        // Cache only first page
        val total = if (offset == 0) {
            page.counts?.collections?.toLong().also { t ->
                profileCacheRepository.cacheCollectionIds(userId, userCollections.map { it.id }, t)
            }
        } else null

        PagedResult(mergeLocalItemCounts(userCollections), total?.toInt())
    }.onFailure { error ->
        Log.e(TAG, "Failed to get collections for user $userId: ${error.message}", error)
    }

    override suspend fun getCollectionById(collectionId: String): Result<Collection> = runCatching {
        val cached = collectionDao.getCollectionsByIds(listOf(collectionId)).firstOrNull()
        if (cached != null) return@runCatching cached

        val collection = apiClient.getCollection(collectionId)
        collectionDao.upsertCollection(collection)
        collection
    }

    override suspend fun getCollectionBySlug(username: String, slug: String): Result<Collection> = runCatching {
        val collection = apiClient.getCollectionBySlug(username, slug)
        collectionDao.upsertCollection(collection)
        collection
    }

    override suspend fun createCollection(name: String, isPublic: Boolean): Result<Collection> =
        runCatching {
            val collection = apiClient.createCollection(name, isPublic)

            collectionDao.upsertCollection(collection)

            // Keep the quick-cache total in sync so a profile opened after a
            // local creation does not report a stale count (e.g. 0). Room is
            // the source of truth for local mutations; the cached total is only
            // adjusted when it is already known, never guessed from scratch.
            val cached = profileCacheRepository.getCollections("user")
            if (cached.total != null) {
                profileCacheRepository.cacheCollectionIds(
                    "user",
                    (cached.items + collection.id).distinct(),
                    cached.total.toLong() + 1,
                )
            }

            collection
        }

    override suspend fun updateCollection(
        collectionId: String,
        name: String?,
        isPublic: Boolean?
    ): Result<String?> =
        runCatching {
            val slug = apiClient.updateCollection(collectionId, name, isPublic)

            collectionDao.updateCollectionMetadata(
                collectionId = collectionId,
                name = name,
                isPublic = isPublic,
                slug = slug,
                updatedAt = LocalDateTime.now()
            )

            slug
        }

    override suspend fun deleteCollection(collectionId: String): Result<Unit> = runCatching {
        apiClient.deleteCollection(collectionId)

        collectionDao.deleteCollectionById(collectionId)

        // Mirror the create path: keep the quick-cache total in sync with the
        // local deletion when the total is already known.
        val cached = profileCacheRepository.getCollections("user")
        if (cached.total != null) {
            profileCacheRepository.cacheCollectionIds(
                "user",
                cached.items - collectionId,
                (cached.total - 1).coerceAtLeast(0).toLong(),
            )
        }
    }

    override suspend fun getCollectionItems(
        collectionId: String,
        limit: Int,
        offset: Int,
        types: List<CatalogItemType>?,
        useCache: Boolean
    ): Result<PagedResult<CatalogItem>> = runCatching {
        Log.d(TAG, "Getting items for collection $collectionId (limit=$limit, offset=$offset, types=$types, useCache=$useCache)")

        val filterType = types?.firstOrNull()

        // Return Room-cached items on the first page when cache is allowed
        if (useCache && offset == 0) {
            val cachedCharts = if (filterType == null || filterType == CatalogItemType.CHART) {
                collectionDao.getChartItems(collectionId, limit, offset)
            } else {
                emptyList()
            }
            val cachedTourPasses = if (filterType == null || filterType == CatalogItemType.TOUR_PASS) {
                collectionDao.getTourPassItems(collectionId, limit, offset)
            } else {
                emptyList()
            }
            val cached = cachedCharts + cachedTourPasses
            if (cached.isNotEmpty()) {
                Log.d(TAG, "Returning cached items for collection $collectionId (${cached.size} items)")
                return@runCatching PagedResult(cached)
            }
        }

        if (offset == 0) {
            queueManager.syncPendingInteractionsBeforeRefresh()
        }

        // Fetch from API
        val page = apiClient.getCollectionItems(collectionId, types = types, limit = limit, offset = offset)

        if (offset == 0 && page.counts != null) {
            profileCacheRepository.cacheCollectionItemCounts(
                collectionId = collectionId,
                counts = SectionCounts(
                    charts = page.counts.charts,
                    tourPasses = page.counts.tourPasses,
                    themes = page.counts.themes,
                )
            )
        }

        val overlay = if (offset == 0) {
            queueManager.getCollectionMembershipOverlay(
                collectionKind = if (collectionId == "bookmarks") CollectionKind.BOOKMARKS else CollectionKind.USER,
                collectionId = collectionId.takeUnless { it == "bookmarks" },
            )
        } else {
            null
        }
        val mergedCharts = chartStateMerger.mergeRemoteCharts(page.items.filterIsInstance<Chart>())
        val remoteTourPasses = page.items.filterIsInstance<TourPass>()
        val filteredCharts = if (overlay != null) {
            mergedCharts.filterNot { it.id in overlay.forceExcludeIds }
        } else {
            mergedCharts
        }
        val filteredTourPasses = if (overlay != null) {
            remoteTourPasses.filterNot { it.id in overlay.forceExcludeIds }
        } else {
            remoteTourPasses
        }
        val missingPendingCharts = if (overlay != null) {
            chartStateMerger.getChartsByIds(
                overlay.forceIncludeIds - filteredCharts.map { it.id }.toSet()
            )
        } else {
            emptyList()
        }
        val missingPendingTourPasses = if (overlay != null) {
            withContext(Dispatchers.IO) {
                tourPassDao.getTourPassesByIds(
                    (overlay.forceIncludeIds - filteredTourPasses.map { it.id }.toSet()).toList()
                )
            }
        } else {
            emptyList()
        }
        val items = (filteredCharts + missingPendingCharts + filteredTourPasses + missingPendingTourPasses)
            .distinctBy { it.id }
        Log.d(TAG, "Fetched ${items.size} items for collection $collectionId from API")

        // Persist charts and tour passes to Room and update cross-refs after the
        // fresh response arrives, so observers never see a transient empty
        // collection during pull-to-refresh.
        if (offset == 0) {
            appDatabase.withTransaction {
                if (items.isNotEmpty()) {
                    collectionDao.upsertCharts(items.filterIsInstance<Chart>())
                    collectionDao.upsertTourPasses(items.filterIsInstance<TourPass>())
                }

                val crossRefs = items.map { item ->
                    CollectionItemCrossRef(
                        collectionId = collectionId,
                        id = item.id,
                        contentType = item.type,
                    )
                }
                val retainedIds = crossRefs.map { it.id }

                if (filterType == null) {
                    // Unfiltered sync: rebuild the whole cross-ref set for the collection.
                    if (retainedIds.isNotEmpty()) {
                        collectionDao.deleteStaleCrossRefs(collectionId, retainedIds)
                    } else {
                        collectionDao.deleteAllCrossRefsForCollection(collectionId)
                    }
                } else {
                    // Type-filtered sync: only evict stale cross-refs of that type,
                    // so other types' cross-refs are preserved.
                    if (crossRefs.isNotEmpty()) {
                        collectionDao.deleteStaleCrossRefs(collectionId, filterType, retainedIds)
                    } else {
                        collectionDao.deleteAllCrossRefsForCollection(collectionId, filterType)
                    }
                }
                collectionDao.upsertCrossRefs(crossRefs)
            }
        }

        PagedResult(items, page.counts?.collections)
    }

    override suspend fun addItemToCollection(
        collectionId: String,
        id: String
    ): Result<Unit> = runCatching {
        apiClient.addItemToCollection(collectionId, id)
    }

    override suspend fun removeItemFromCollection(
        collectionId: String,
        id: String
    ): Result<Unit> = runCatching {
        apiClient.removeItemFromCollection(collectionId, id)
    }

    override fun observeCollectionChartIds(collectionId: String): Flow<List<String>> =
        collectionDao.observeChartIdsForCollection(collectionId)

    override fun observeCollectionItemIds(collectionId: String): Flow<List<String>> =
        collectionDao.observeItemIdsForCollection(collectionId)

    override fun observeUserCollections(): Flow<List<Collection>> =
        collectionDao.observeUserCollections()

    /**
     * Overrides the server-side item counts with counts computed from the local
     * cross-ref table when the app has local data for a collection, so mutations
     * done offline (or not yet re-fetched) are reflected immediately. Collections
     * without local cross-refs keep their server-provided counts.
     */
    private suspend fun mergeLocalItemCounts(collections: List<Collection>): List<Collection> {
        if (collections.isEmpty()) return collections

        val localCounts = collectionDao.getItemCounts(collections.map { it.id })
            .associate { it.collectionId to Triple(it.chartCount, it.tourPassCount, it.themeCount) }

        return collections.map { collection ->
            val counts = localCounts[collection.id]
            if (counts != null && (counts.first > 0 || counts.second > 0 || counts.third > 0)) {
                collection.copy(
                    chartCount = counts.first,
                    tourPassCount = counts.second,
                    themeCount = counts.third,
                )
            } else {
                collection
            }
        }
    }
}
