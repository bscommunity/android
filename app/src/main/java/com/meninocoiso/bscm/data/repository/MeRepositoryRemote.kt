package com.meninocoiso.bscm.data.repository

import android.util.Log
import com.meninocoiso.bscm.data.local.dao.ChartDao
import com.meninocoiso.bscm.data.local.dao.CollectionDao
import com.meninocoiso.bscm.data.manager.ChartStateMerger
import com.meninocoiso.bscm.data.manager.InteractionQueueManager
import com.meninocoiso.bscm.data.remote.ApiClient
import com.meninocoiso.bscm.data.remote.dto.activity.ActivityItemResponse
import com.meninocoiso.bscm.di.ApplicationScope
import com.meninocoiso.bscm.domain.enums.CollectionKind
import com.meninocoiso.bscm.domain.enums.CatalogItemType
import com.meninocoiso.bscm.domain.model.CatalogItem
import com.meninocoiso.bscm.domain.model.Chart
import com.meninocoiso.bscm.domain.model.Collection
import com.meninocoiso.bscm.domain.model.CollectionItemCrossRef
import com.meninocoiso.bscm.domain.repository.MeRepository
import com.meninocoiso.bscm.presentation.viewmodel.profile.PagedResult
import jakarta.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

private const val TAG = "MeRepositoryRemote"

class MeRepositoryRemote @Inject constructor(
    @param:ApplicationScope private val coroutineScope: CoroutineScope,
    private val apiClient: ApiClient,
    private val profileCacheRepository: ProfileCacheRepository,
    private val chartDao: ChartDao,
    private val collectionDao: CollectionDao,
    private val queueManager: InteractionQueueManager,
    private val chartStateMerger: ChartStateMerger,
) : MeRepository {
    override suspend fun getActivity(
        limit: Int,
        offset: Int,
        useCache: Boolean
    ): Result<List<ActivityItemResponse>> = runCatching {
        if (useCache && offset == 0) {
            profileCacheRepository.getActivity()?.let { cached ->
                Log.d(TAG, "Returning cached activity")
                return@runCatching cached
            }
        }

        val activity = apiClient.getMyActivity(limit, offset)

        if (offset == 0) {
            profileCacheRepository.cacheActivity(activity)
        }

        activity
    }

    override suspend fun getLikes(
        limit: Int,
        offset: Int,
        useCache: Boolean
    ): Result<PagedResult<CatalogItem>> =
        runCatching {
            Log.d(TAG, "getLikes called with limit=$limit, offset=$offset, useCache=$useCache")
            if (useCache && offset == 0) {
                val localLikes =
                    withContext(Dispatchers.IO) { chartDao.getLikedCharts(limit, offset) }
                if (localLikes.isNotEmpty()) {
                    Log.d(TAG, "Returning likes from Room (${localLikes.size} items)")
                    // Restore cached total count so the UI can show it without a network hit
                    val cachedTotal = profileCacheRepository.getLikesCount()?.toInt()
                    return@runCatching PagedResult(
                        items = localLikes,
                        total = cachedTotal
                    )
                }
            }

            if (offset == 0) {
                queueManager.syncPendingInteractionsBeforeRefresh()
            }

            // First page: fetch all content types to receive ContentCounts
            // Subsequent pages: filter to CHART only (the only type we persist / display here)
            val types = if (offset == 0) null else listOf(CatalogItemType.CHART)
            val page = apiClient.getMyLikes(limit, offset, types)
            val remoteLikes = chartStateMerger.mergeRemoteCharts(page.items)
            Log.d(TAG, "Fetched ${remoteLikes.size} likes from API (offset=$offset)")

            // Persist total count from the first-page response
            if (offset == 0) {
                page.counts?.charts?.let { count ->
                    profileCacheRepository.cacheLikesCount(count.toLong())
                }
            }

            // Persist before returning so observeLikedCharts cannot race with stale rows.
            withContext(Dispatchers.IO) { chartDao.insert(remoteLikes) }

            PagedResult(
                items = remoteLikes,
                total = page.counts?.charts,
            )
        }

    override suspend fun getBookmarks(
        limit: Int,
        offset: Int,
        useCache: Boolean
    ): Result<PagedResult<CatalogItem>> = runCatching {
        Log.d(TAG, "getBookmarks called with limit=$limit, offset=$offset, useCache=$useCache")

        if (useCache && offset == 0) {
            val localBookmarks =
                withContext(Dispatchers.IO) { chartDao.getBookmarkedCharts(limit, offset) }
            Log.d(TAG, "Found ${localBookmarks.size} bookmarked charts in Room for offset=$offset")

            if (localBookmarks.isNotEmpty()) {
                Log.d(TAG, "Returning bookmarks from Room (${localBookmarks.size} items)")
                val cachedTotal = profileCacheRepository.getBookmarksCount()?.toInt()
                return@runCatching PagedResult(
                    items = localBookmarks,
                    total = cachedTotal
                )
            }
        }

        if (offset == 0) {
            queueManager.syncPendingInteractionsBeforeRefresh()
        }

        // First page: fetch all content types to receive ContentCounts
        // Subsequent pages: filter to CHART only
        val types = if (offset == 0) null else listOf(CatalogItemType.CHART)
        val page = apiClient.getMyBookmarks(limit, offset, types)
        val overlay = if (offset == 0) {
            queueManager.getCollectionMembershipOverlay(CollectionKind.BOOKMARKS)
        } else {
            null
        }
        val remoteBookmarks = chartStateMerger.mergeRemoteCharts(page.items)
        val filteredBookmarks = if (overlay != null) {
            remoteBookmarks.filterNot { it.id in overlay.forceExcludeContentIds }
        } else {
            remoteBookmarks
        }
        val missingPendingBookmarks = if (overlay != null && offset == 0) {
            chartStateMerger.getChartsByIds(
                overlay.forceIncludeContentIds - filteredBookmarks.map { it.id }.toSet()
            )
        } else {
            emptyList()
        }
        val effectiveBookmarks = (filteredBookmarks + missingPendingBookmarks)
            .distinctBy { it.id }

        // Persist total count from the first-page response
        if (offset == 0) {
            page.counts?.charts?.let { count ->
                profileCacheRepository.cacheBookmarksCount(count.toLong())
            }
        }

        // Do the stale eviction synchronously before returning — this is what
        // prevents the observer from seeing an intermediate state with ghost items.
        // The writes are cheap (single-page DB ops) and must happen before the
        // caller considers this fetch complete.
        withContext(Dispatchers.IO) {
            collectionDao.upsertCollection(
                Collection(
                    id = "bookmarks",
                    userId = "user",
                    kind = CollectionKind.BOOKMARKS,
                    name = "Bookmarks",
                    isPublic = false,
                    createdAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now(),
                )
            )

            val crossRefs = page.items.mapNotNull { chart ->
                chart.id.takeUnless { overlay?.forceExcludeContentIds?.contains(it) == true }?.let { contentId ->
                    CollectionItemCrossRef(
                        collectionId = "bookmarks",
                        contentId = contentId,
                        contentType = CatalogItemType.CHART
                    )
                }
            } + missingPendingBookmarks.mapNotNull { chart ->
                chart.id.let { contentId ->
                    CollectionItemCrossRef(
                        collectionId = "bookmarks",
                        contentId = contentId,
                        contentType = CatalogItemType.CHART
                    )
                }
            }
            val retainedContentIds = crossRefs.map { it.contentId }

            if (offset == 0) {
                if (retainedContentIds.isNotEmpty()) {
                    collectionDao.deleteStaleCrossRefs("bookmarks", retainedContentIds)
                } else {
                    collectionDao.deleteAllCrossRefsForCollection("bookmarks")
                }
            }

            if (crossRefs.isNotEmpty()) {
                collectionDao.upsertCrossRefs(crossRefs)
            }
        }

        // Chart rows are idempotent — safe to persist in background even if caller is cancelled
        coroutineScope.launch { chartDao.insert(effectiveBookmarks) }

        PagedResult(
            items = effectiveBookmarks,
            total = page.counts?.charts,
        )
    }

    // -----------------------------------------------------------------
    // Reactive streams — thin pass-through to ChartDao's Flow queries.
    //
    // Room handles all the magic here: whenever any coroutine writes
    // liked_at or bookmarked_at (including InteractionViewModel via
    // InteractionRepositoryImpl), Room invalidates these flows and
    // re-emits the full updated list automatically.
    // -----------------------------------------------------------------

    override fun observeLikes(): Flow<List<Chart>> =
        chartDao.observeLikedCharts()

    override fun observeBookmarks(): Flow<List<Chart>> =
        chartDao.observeBookmarkedCharts()
}