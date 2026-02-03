package com.meninocoiso.bscm.data.repository

import android.util.Log
import com.meninocoiso.bscm.data.remote.ApiClient
import com.meninocoiso.bscm.data.remote.dto.activity.ActivityEntry
import com.meninocoiso.bscm.data.remote.dto.user.UserProfileResponse
import com.meninocoiso.bscm.domain.model.Chart
import com.meninocoiso.bscm.domain.repository.MeRepository
import jakarta.inject.Inject

private const val TAG = "MeRepositoryRemote"

class MeRepositoryRemote @Inject constructor(
    private val apiClient: ApiClient,
    private val profileCacheRepository: ProfileCacheRepository
) : MeRepository {
    override suspend fun getProfile(useCache: Boolean): Result<UserProfileResponse> = runCatching {
        // Try cache first if requested
        if (useCache) {
            profileCacheRepository.getProfile("")?.let { cached ->
                Log.d(TAG, "Returning cached profile")
                return@runCatching cached
            }
        }

        // Fetch from API
        val profile = apiClient.getMyProfile()

        // Cache the result
        profileCacheRepository.cacheProfile("", profile)

        profile
    }

    override suspend fun getActivity(limit: Int, offset: Int, useCache: Boolean): Result<List<ActivityEntry>> = runCatching {
        // Only use cache for first page
        if (useCache && offset == 0) {
            profileCacheRepository.getActivity("")?.let { cached ->
                Log.d(TAG, "Returning cached activity")
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
            /*profileCacheRepository.getMyLikes()?.let { cached ->
                Log.d(TAG, "Returning cached likes (${cached.size} items)")
                return@runCatching cached
            }*/
        }

        // Fetch from API
        val likes = apiClient.getMyLikes(limit, offset)

        // Cache only first page
        /*if (offset == 0) {
            profileCacheRepository.cacheMyLikes(likes)
        }*/

        likes
    }

    override suspend fun getBookmarks(limit: Int, offset: Int, useCache: Boolean): Result<List<Chart>> = runCatching {
        // Only use cache for first page
        if (useCache && offset == 0) {
            /*profileCacheRepository.getMyBookmarks()?.let { cached ->
                Log.d(TAG, "Returning cached bookmarks (${cached.size} items)")
                return@runCatching cached
            }*/
        }

        // Fetch from API
        val bookmarks = apiClient.getMyBookmarks(limit, offset)

        // Cache only first page
        /*if (offset == 0) {
            profileCacheRepository.cacheMyBookmarks(bookmarks)
        }*/

        bookmarks
    }
}
