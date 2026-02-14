package com.meninocoiso.bscm.data.repository

import android.util.Log
import com.meninocoiso.bscm.data.manager.ChartManager
import com.meninocoiso.bscm.data.remote.ApiClient
import com.meninocoiso.bscm.data.remote.dto.activity.ActivityItemResponse
import com.meninocoiso.bscm.data.remote.dto.user.UserProfileResponse
import com.meninocoiso.bscm.domain.model.Chart
import com.meninocoiso.bscm.domain.repository.MeRepository
import com.meninocoiso.bscm.domain.result.ContentResult
import jakarta.inject.Inject
import kotlinx.coroutines.flow.first

private const val TAG = "MeRepositoryRemote"

class MeRepositoryRemote @Inject constructor(
    private val apiClient: ApiClient,
    private val profileCacheRepository: ProfileCacheRepository,
    private val chartManager: ChartManager
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
            profileCacheRepository.cacheActivity("", activity)
        }

        activity
    }

    override suspend fun getLikes(limit: Int, offset: Int, useCache: Boolean): Result<List<Chart>> = runCatching {
        // Only use cache for first page
        if (useCache && offset == 0) {
            profileCacheRepository.getMyLikesIds()?.let { cachedIds ->
                Log.d(TAG, "Returning cached likes IDs (${cachedIds.size} items)")
                // Hydrate via chartManager which will read from memory/local/remote as needed
                val chartsRes = chartManager.getChartsById(cachedIds).first { it !is ContentResult.Loading }
                when (chartsRes) {
                    is ContentResult.Success<*> -> return@runCatching (chartsRes as ContentResult.Success<List<Chart>>).data
                    is ContentResult.Error -> Log.e(TAG, "Error hydrating cached likes: ${chartsRes.message}")
                    else -> {}
                }
            }
        }

        // Fetch from API
        val likes = apiClient.getMyLikes(limit, offset)
        Log.d(TAG, "Fetched ${likes.size} likes from API")
        // Update local cache

        // Update local cache with latest data for each liked chart (ensures DB content is updated)
        // chartManager.updateCharts()

        // Cache only first page: store IDs
        if (offset == 0) {
            profileCacheRepository.cacheMyLikesIds(likeIds = likes.map { it.id })
        }

        likes
    }

    override suspend fun getBookmarks(limit: Int, offset: Int, useCache: Boolean): Result<List<Chart>> = runCatching {
        // Only use cache for first page
        if (useCache && offset == 0) {
            profileCacheRepository.getMyBookmarksIds()?.let { cachedIds ->
                Log.d(TAG, "Returning cached bookmarks IDs (${cachedIds.size} items)")
                val chartsRes = chartManager.getChartsById(cachedIds).first { it !is ContentResult.Loading }
                when (chartsRes) {
                    is ContentResult.Success<*> -> return@runCatching (chartsRes as ContentResult.Success<List<Chart>>).data
                    is ContentResult.Error -> Log.e(TAG, "Error hydrating cached bookmarks: ${chartsRes.message}")
                    else -> {}
                }
            }
        }

        // Fetch from API
        val bookmarks = apiClient.getMyBookmarks(limit, offset)
        // Update local cache

        // Cache only first page: store IDs
        if (offset == 0) {
            profileCacheRepository.cacheMyBookmarksIds(bookmarks.map { it.id })
        }

        bookmarks
    }
}
