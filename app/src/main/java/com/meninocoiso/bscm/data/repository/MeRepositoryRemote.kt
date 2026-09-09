package com.meninocoiso.bscm.data.repository

import android.util.Log
import com.meninocoiso.bscm.data.local.dao.ChartDao
import com.meninocoiso.bscm.data.local.dao.CollectionDao
import com.meninocoiso.bscm.data.local.dao.ThemeDao
import com.meninocoiso.bscm.data.local.dao.TourPassDao
import com.meninocoiso.bscm.data.manager.ChartStateMerger
import com.meninocoiso.bscm.data.manager.InteractionQueueManager
import com.meninocoiso.bscm.data.remote.ApiClient
import com.meninocoiso.bscm.data.remote.dto.activity.ActivityItemResponse
import com.meninocoiso.bscm.data.remote.dto.user.SectionCounts
import com.meninocoiso.bscm.di.ApplicationScope
import com.meninocoiso.bscm.domain.enums.CollectionKind
import com.meninocoiso.bscm.domain.enums.CatalogItemType
import com.meninocoiso.bscm.domain.model.CatalogItem
import com.meninocoiso.bscm.domain.model.Chart
import com.meninocoiso.bscm.domain.model.Collection
import com.meninocoiso.bscm.domain.model.CollectionItemCrossRef
import com.meninocoiso.bscm.domain.model.Theme
import com.meninocoiso.bscm.domain.model.TourPass
import com.meninocoiso.bscm.domain.repository.MeRepository
import com.meninocoiso.bscm.presentation.viewmodel.profile.PagedResult
import jakarta.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

private const val TAG = "MeRepositoryRemote"

private typealias ContentCountsTriple = Triple<Int, Int, Int>

class MeRepositoryRemote @Inject constructor(
    @param:ApplicationScope private val coroutineScope: CoroutineScope,
    private val apiClient: ApiClient,
    private val profileCacheRepository: ProfileCacheRepository,
    private val chartDao: ChartDao,
    private val tourPassDao: TourPassDao,
    private val themeDao: ThemeDao,
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
                val localLikes = withContext(Dispatchers.IO) {
                    val charts = chartDao.getLikedCharts(limit, offset)
                    val tourPasses = tourPassDao.getLikedTourPasses(limit, offset)
                    val themes = themeDao.getLikedThemes(limit, offset)
                    (charts + tourPasses + themes).distinctBy { it.id }
                }
                if (localLikes.isNotEmpty()) {
                    Log.d(TAG, "Returning likes from Room (${localLikes.size} items)")
                    // Restore cached total + per-type counts so the UI can show
                    // them without a network hit. Fall back to (or merge with)
                    // counts computed from Room so they never disagree with the
                    // sections the observers are already showing.
                    val cachedTotal = profileCacheRepository.getLikesCount()?.toInt()
                    val cachedCounts = profileCacheRepository.getLikesCounts()?.toTriple()
                    val localCounts = withContext(Dispatchers.IO) {
                        Triple(
                            chartDao.countLikedCharts(),
                            tourPassDao.countLikedTourPasses(),
                            themeDao.countLikedThemes()
                        )
                    }
                    val counts = cachedCounts?.let { mergeCounts(it, localCounts) }
                        ?: localCounts
                    return@runCatching PagedResult(
                        items = localLikes,
                        total = cachedTotal ?: counts.toList().sum(),
                        counts = counts
                    )
                }
            }

            if (offset == 0) {
                queueManager.syncPendingInteractionsBeforeRefresh()
            }

            // First page: fetch all content types to receive ContentCounts
            // Subsequent pages: filter to CHART only (the only type we paginate)
            val types = if (offset == 0) null else listOf(CatalogItemType.CHART)
            val page = apiClient.getMyLikes(limit, offset, types)
            // The /me/likes payload never carries like timestamps, so membership
            // in this list is the source of truth: backfill a missing timestamp
            // before persisting, otherwise Room's liked_at IS NOT NULL
            // observers/cache would drop these rows and make the list vanish.
val remoteCharts = chartStateMerger.mergeRemoteCharts(page.items.filterIsInstance<Chart>())
            // The /me/bookmarks payload never carries bookmark timestamps, so
            // membership in this list is the source of truth: backfill a missing
            // timestamp before persisting, otherwise Room's bookmarked_at IS NOT
            // NULL observers/cache would drop these rows and make the list vanish.
            .map { it.copy(bookmarkedAt = it.bookmarkedAt ?: LocalDateTime.now()) }
                .map { it.copy(likedAt = it.likedAt ?: LocalDateTime.now()) }
            val remoteTourPasses = page.items.filterIsInstance<TourPass>()
            val remoteThemes = page.items.filterIsInstance<Theme>()
            Log.d(TAG, "Fetched likes from API (offset=$offset): ${remoteCharts.size} charts, " +
                    "${remoteTourPasses.size} tour passes, ${remoteThemes.size} themes")

            // Persist total count from the first-page response
            if (offset == 0) {
                page.counts?.let { counts ->
                    profileCacheRepository.cacheLikesCount(counts.charts.toLong())
                    profileCacheRepository.cacheLikesCounts(
                        SectionCounts(counts.charts, counts.tourPasses, counts.themes)
                    )
                }
            }

            // Persist before returning so the observers cannot race with stale rows.
            withContext(Dispatchers.IO) {
                if (remoteCharts.isNotEmpty()) chartDao.insert(remoteCharts)
                if (remoteTourPasses.isNotEmpty()) tourPassDao.insert(remoteTourPasses)
                if (remoteThemes.isNotEmpty()) themeDao.insert(remoteThemes)
            }

            PagedResult(
                items = remoteCharts + remoteTourPasses + remoteThemes,
                total = page.counts?.charts,
                counts = page.counts?.let { Triple(it.charts, it.tourPasses, it.themes) },
            )
        }

    override suspend fun getBookmarks(
        limit: Int,
        offset: Int,
        useCache: Boolean
    ): Result<PagedResult<CatalogItem>> = runCatching {
        Log.d(TAG, "getBookmarks called with limit=$limit, offset=$offset, useCache=$useCache")

        if (useCache && offset == 0) {
            val localBookmarks = withContext(Dispatchers.IO) {
                val charts = chartDao.getBookmarkedCharts(limit, offset)
                val tourPasses = tourPassDao.getBookmarkedTourPasses(limit, offset)
                val themes = themeDao.getBookmarkedThemes(limit, offset)
                (charts + tourPasses + themes).distinctBy { it.id }
            }
            Log.d(TAG, "Found ${localBookmarks.size} bookmarked items in Room for offset=$offset")

            if (localBookmarks.isNotEmpty()) {
                Log.d(TAG, "Returning bookmarks from Room (${localBookmarks.size} items)")
                val cachedTotal = profileCacheRepository.getBookmarksCount()?.toInt()
                val cachedCounts = profileCacheRepository.getBookmarksCounts()?.toTriple()
                val localCounts = withContext(Dispatchers.IO) {
                    Triple(
                        chartDao.countBookmarkedCharts(),
                        tourPassDao.countBookmarkedTourPasses(),
                        themeDao.countBookmarkedThemes()
                    )
                }
                val counts = cachedCounts?.let { mergeCounts(it, localCounts) }
                    ?: localCounts
                return@runCatching PagedResult(
                    items = localBookmarks,
                    total = cachedTotal ?: counts.toList().sum(),
                    counts = counts
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
        val remoteCharts = chartStateMerger.mergeRemoteCharts(page.items.filterIsInstance<Chart>())
        val remoteTourPasses = page.items.filterIsInstance<TourPass>()
        val remoteThemes = page.items.filterIsInstance<Theme>()
        val filteredCharts = if (overlay != null) {
            remoteCharts.filterNot { it.id in overlay.forceExcludeIds }
        } else {
            remoteCharts
        }
        val missingPendingBookmarks = if (overlay != null && offset == 0) {
            chartStateMerger.getChartsByIds(
                overlay.forceIncludeIds - filteredCharts.map { it.id }.toSet()
            )
        } else {
            emptyList()
        }
        val effectiveBookmarks = (filteredCharts + missingPendingBookmarks + remoteTourPasses + remoteThemes)
            .distinctBy { it.id }

        // Persist total count from the first-page response
        if (offset == 0) {
            page.counts?.let { counts ->
                profileCacheRepository.cacheBookmarksCount(counts.charts.toLong())
                profileCacheRepository.cacheBookmarksCounts(
                    SectionCounts(counts.charts, counts.tourPasses, counts.themes)
                )
            }
        }

        // Rebuild the whole bookmarks cross-ref set synchronously on the first page
        // so the bookmark observers for every content type stay consistent.
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

            if (offset == 0) {
                collectionDao.deleteAllCrossRefsForCollection("bookmarks")
            }

            val crossRefs = buildList {
                filteredCharts.forEach { chart ->
                    if (overlay?.forceExcludeIds?.contains(chart.id) != true) {
                        add(
                            CollectionItemCrossRef(
                                collectionId = "bookmarks",
                                id = chart.id,
                                contentType = CatalogItemType.CHART
                            )
                        )
                    }
                }
                missingPendingBookmarks.forEach { chart ->
                    add(
                        CollectionItemCrossRef(
                            collectionId = "bookmarks",
                            id = chart.id,
                            contentType = CatalogItemType.CHART
                        )
                    )
                }
                remoteTourPasses.forEach { tourPass ->
                    add(
                        CollectionItemCrossRef(
                            collectionId = "bookmarks",
                            id = tourPass.id,
                            contentType = CatalogItemType.TOUR_PASS
                        )
                    )
                }
                remoteThemes.forEach { theme ->
                    add(
                        CollectionItemCrossRef(
                            collectionId = "bookmarks",
                            id = theme.id,
                            contentType = CatalogItemType.THEME
                        )
                    )
                }
            }

            if (crossRefs.isNotEmpty()) {
                collectionDao.upsertCrossRefs(crossRefs)
            }
        }

        // Rows are idempotent — safe to persist in background even if caller is cancelled
        coroutineScope.launch {
            if (effectiveBookmarks.isNotEmpty()) {
                chartDao.insert(effectiveBookmarks.filterIsInstance<Chart>())
                tourPassDao.insert(effectiveBookmarks.filterIsInstance<TourPass>())
                themeDao.insert(effectiveBookmarks.filterIsInstance<Theme>())
            }
        }

        PagedResult(
            items = effectiveBookmarks,
            total = page.counts?.charts,
            counts = page.counts?.let { Triple(it.charts, it.tourPasses, it.themes) },
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

    override fun observeLikes(): Flow<List<CatalogItem>> =
        combine(
            chartDao.observeLikedCharts(),
            tourPassDao.observeLikedTourPasses(),
            themeDao.observeLikedThemes(),
        ) { charts, tourPasses, themes ->
            charts + tourPasses + themes
        }

    override fun observeBookmarks(): Flow<List<CatalogItem>> =
        combine(
            chartDao.observeBookmarkedCharts(),
            tourPassDao.observeBookmarkedTourPasses(),
            themeDao.observeBookmarkedThemes(),
        ) { charts, tourPasses, themes ->
            charts + tourPasses + themes
        }

    /**
     * Per-type counts for the cache path. The cached server counts can be
     * stale-low (e.g. persisted before the auth fix / before local sync), while
     * the Room counts can be low when only a subset of pages was fetched.
     * Taking the per-type maximum keeps the badge consistent with the highest
     * known truth without ever under-reporting what the observer shows.
     */
    private fun mergeCounts(
        cached: ContentCountsTriple,
        local: ContentCountsTriple
    ): ContentCountsTriple = Triple(
        maxOf(cached.first, local.first),
        maxOf(cached.second, local.second),
        maxOf(cached.third, local.third)
    )
}