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
        // Try cache first if requested
        if (useCache) {
            profileCacheRepository.getProfile()?.let { cached ->
                Log.d(TAG, "Returning cached profile")
                return@runCatching cached
            }
        }

        // Fetch from API
        val profile = apiClient.getMyProfile()

        // Cache the result (owner)
        profileCacheRepository.cacheProfile(profile = profile)

        profile
    }

    override suspend fun getActivity(limit: Int, offset: Int, useCache: Boolean): Result<List<ActivityItemResponse>> = runCatching {
        // Only use cache for first page
        if (useCache && offset == 0) {
            profileCacheRepository.getActivity()?.let { cached ->
                Log.d(TAG, "Returning cached activity")
                // hydrate charts for each activity entry using chartManager (ensures DB content is used)
                return@runCatching cached
            }
        }

        // Fetch from API
        val activity = apiClient.getMyActivity(limit, offset)

        // Cache only first page
        if (offset == 0) {
            profileCacheRepository.cacheActivity(activity)
        }

        activity
    }

    override suspend fun getLikes(limit: Int, offset: Int, useCache: Boolean): Result<List<Chart>> = runCatching {
        val localLikes = withContext(Dispatchers.IO) { chartDao.getLikedCharts(limit, offset) }
        if (localLikes.isNotEmpty()) {
            Log.d(TAG, "Returning likes from Room (${localLikes.size} items)")
            return@runCatching localLikes
        }
        if (useCache && offset > 0) {
            return@runCatching emptyList()
        }

        // Fetch from API
        val likes = apiClient.getMyLikes(limit, offset)
        Log.d(TAG, "Fetched ${likes.size} likes from API")

        // Persist likedAt metadata for UI usage in background
        coroutineScope.launch { chartManager.persistCharts(likes) }

        likes
    }

    override suspend fun getBookmarks(limit: Int, offset: Int, useCache: Boolean): Result<List<Chart>> = runCatching {
        val localBookmarks = withContext(Dispatchers.IO) { chartDao.getBookmarkedCharts(limit, offset) }
        if (localBookmarks.isNotEmpty()) {
            Log.d(TAG, "Returning bookmarks from Room (${localBookmarks.size} items)")
            return@runCatching localBookmarks
        }
        if (useCache && offset > 0) {
            return@runCatching emptyList()
        }

        // Fetch from API
        val bookmarks = apiClient.getMyBookmarks(limit, offset)

        // Persist bookmarkedAt metadata for UI usage in background
        coroutineScope.launch { chartManager.persistCharts(bookmarks) }

        bookmarks
    }
}
