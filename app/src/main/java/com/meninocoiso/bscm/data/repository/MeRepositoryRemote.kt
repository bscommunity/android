package com.meninocoiso.bscm.data.repository

import android.util.Log
import com.meninocoiso.bscm.data.local.dao.ChartDao
import com.meninocoiso.bscm.data.local.dao.CollectionDao
import com.meninocoiso.bscm.data.remote.ApiClient
import com.meninocoiso.bscm.data.remote.dto.activity.ActivityItemResponse
import com.meninocoiso.bscm.data.remote.dto.user.UserProfileResponse
import com.meninocoiso.bscm.di.ApplicationScope
import com.meninocoiso.bscm.domain.enums.CollectionKind
import com.meninocoiso.bscm.domain.enums.ContentType
import com.meninocoiso.bscm.domain.model.Chart
import com.meninocoiso.bscm.domain.model.Collection
import com.meninocoiso.bscm.domain.model.CollectionItemCrossRef
import com.meninocoiso.bscm.domain.repository.MeRepository
import jakarta.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "MeRepositoryRemote"

class MeRepositoryRemote @Inject constructor(
    @param:ApplicationScope private val coroutineScope: CoroutineScope,
    private val apiClient: ApiClient,
    private val profileCacheRepository: ProfileCacheRepository,
    private val chartDao: ChartDao,
    private val collectionDao: CollectionDao
) : MeRepository {
    override suspend fun getProfile(useCache: Boolean): Result<UserProfileResponse> = runCatching {
        if (useCache) {
            profileCacheRepository.getProfile()?.let { cached ->
                Log.d(TAG, "Returning cached profile")
                return@runCatching cached
            }
        }

        val profile = apiClient.getMyProfile()
        profileCacheRepository.cacheProfile(profile = profile)
        profile
    }

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

    override suspend fun getLikes(limit: Int, offset: Int, useCache: Boolean): Result<List<Chart>> =
        runCatching {
            Log.d(TAG, "getLikes called with limit=$limit, offset=$offset, useCache=$useCache")
            val localLikes = withContext(Dispatchers.IO) { chartDao.getLikedCharts(limit, offset) }
            Log.d(TAG, "Found ${localLikes.size} liked charts in Room for offset=$offset")
            if (useCache && offset == 0 && localLikes.isNotEmpty()) {
                Log.d(TAG, "Returning likes from Room (${localLikes.size} items)")
                return@runCatching localLikes
            }

            val likes = apiClient.getMyLikes(limit, offset)
            Log.d(TAG, "Fetched ${likes.size} likes from API")

            coroutineScope.launch { chartDao.insert(likes) }

            likes
        }

    override suspend fun getBookmarks(
        limit: Int,
        offset: Int,
        useCache: Boolean
    ): Result<List<Chart>> = runCatching {
        val localBookmarks =
            withContext(Dispatchers.IO) { chartDao.getBookmarkedCharts(limit, offset) }
        if (useCache && offset == 0 && localBookmarks.isNotEmpty()) {
            return@runCatching localBookmarks
        }

        val bookmarks = apiClient.getMyBookmarks(limit, offset)

        // Do the stale eviction synchronously before returning — this is what
        // prevents the observer from seeing an intermediate state with ghost items.
        // The writes are cheap (single-page DB ops) and must happen before the
        // caller considers this fetch complete.
        withContext(Dispatchers.IO) {
            collectionDao.upsertCollection(
                Collection(
                    id = "bookmarks",
                    userId = "me",
                    kind = CollectionKind.BOOKMARKS,
                    name = "Bookmarks",
                    isPublic = false,
                    createdAt = java.time.LocalDateTime.now(),
                    updatedAt = java.time.LocalDateTime.now(),
                )
            )

            val crossRefs = bookmarks.mapNotNull { chart ->
                chart.contentId?.let { contentId ->
                    CollectionItemCrossRef(
                        collectionId = "bookmarks",
                        contentId = contentId,
                        contentType = ContentType.CHART
                    )
                }
            }
            val retainedContentIds = crossRefs.map { it.contentId }

            if (offset == 0) {
                if (retainedContentIds.isNotEmpty()) {
                    collectionDao.deleteStaleBookmarkCrossRefs("bookmarks", retainedContentIds)
                } else {
                    collectionDao.deleteAllCrossRefsForCollection("bookmarks")
                }
            }

            if (crossRefs.isNotEmpty()) {
                collectionDao.upsertCrossRefs(crossRefs)
            }
        }

        // Chart rows are idempotent — safe to persist in background even if caller is cancelled
        coroutineScope.launch { chartDao.insert(bookmarks) }

        bookmarks
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