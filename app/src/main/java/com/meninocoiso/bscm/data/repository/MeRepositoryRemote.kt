package com.meninocoiso.bscm.data.repository

import android.util.Log
import com.meninocoiso.bscm.data.local.dao.ChartDao
import com.meninocoiso.bscm.data.manager.ChartManager
import com.meninocoiso.bscm.data.remote.ApiClient
import com.meninocoiso.bscm.data.remote.dto.activity.ActivityItemResponse
import com.meninocoiso.bscm.data.remote.dto.user.UserProfileResponse
import com.meninocoiso.bscm.di.ApplicationScope
import com.meninocoiso.bscm.domain.model.Chart
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
    private val chartManager: ChartManager,
    private val chartDao: ChartDao
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

    override suspend fun getActivity(limit: Int, offset: Int, useCache: Boolean): Result<List<ActivityItemResponse>> = runCatching {
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

    override suspend fun getLikes(limit: Int, offset: Int, useCache: Boolean): Result<List<Chart>> = runCatching {
        Log.d(TAG, "getLikes called with limit=$limit, offset=$offset, useCache=$useCache")
        val localLikes = withContext(Dispatchers.IO) { chartDao.getLikedCharts(limit, offset) }
        Log.d(TAG, "Found ${localLikes.size} liked charts in Room for offset=$offset")
        if (useCache && offset == 0 && localLikes.isNotEmpty()) {
            Log.d(TAG, "Returning likes from Room (${localLikes.size} items)")
            return@runCatching localLikes
        }

        val likes = apiClient.getMyLikes(limit, offset)
        Log.d(TAG, "Fetched ${likes.size} likes from API")

        coroutineScope.launch { chartManager.persistCharts(likes) }

        likes
    }

    override suspend fun getBookmarks(limit: Int, offset: Int, useCache: Boolean): Result<List<Chart>> = runCatching {
        Log.d(TAG, "getBookmarks called with limit=$limit, offset=$offset, useCache=$useCache")
        val localBookmarks = withContext(Dispatchers.IO) { chartDao.getBookmarkedCharts(limit, offset) }
        if (useCache && offset == 0 && localBookmarks.isNotEmpty()) {
            Log.d(TAG, "Returning bookmarks from Room (${localBookmarks.size} items)")
            return@runCatching localBookmarks
        }

        val bookmarks = apiClient.getMyBookmarks(limit, offset)

        coroutineScope.launch { chartManager.persistCharts(bookmarks) }

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