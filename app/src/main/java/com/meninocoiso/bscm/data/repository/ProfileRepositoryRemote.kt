package com.meninocoiso.bscm.data.repository

import com.meninocoiso.bscm.data.remote.ApiClient
import com.meninocoiso.bscm.data.remote.dto.activity.ActivityEntry
import com.meninocoiso.bscm.data.remote.dto.user.UserProfileResponse
import com.meninocoiso.bscm.domain.model.Chart
import com.meninocoiso.bscm.domain.repository.ProfileRepository
import jakarta.inject.Inject

private const val TAG = "ProfileRepositoryRemote"

class ProfileRepositoryRemote @Inject constructor(
    private val apiClient: ApiClient,
    private val profileCacheRepository: ProfileCacheRepository
) : ProfileRepository {
    override suspend fun getProfileHeader(userId: String, useCache: Boolean): Result<UserProfileResponse> = runCatching {
        // Try cache first if requested
        /*if (useCache) {
            profileCacheRepository.getUserProfile(userId)?.let { cached ->
                Log.d(TAG, "Returning cached profile for user: $userId")
                return@runCatching cached
            }
        }*/

        // Fetch from API
        val profile = apiClient.getUserProfile(userId)

        // Cache the result
        // profileCacheRepository.cacheUserProfile(userId, profile)

        profile
    }

    override suspend fun getProfileHeaderByUsername(username: String, useCache: Boolean): Result<UserProfileResponse> = runCatching {
        // Username-based lookups don't use cache since we don't know the userId yet
        apiClient.getUserProfileByUsername(username)
    }

    override suspend fun getActivity(userId: String, limit: Int, offset: Int, useCache: Boolean): Result<List<ActivityEntry>> =
        runCatching {
            // Only use cache for first page
            /*if (useCache && offset == 0) {
                profileCacheRepository.getUserActivity(userId)?.let { cached ->
                    Log.d(TAG, "Returning cached activity for user: $userId")
                    return@runCatching cached
                }
            }*/

            // Fetch from API
            val activity = apiClient.getUserActivity(userId, limit, offset)

            // Cache only first page
            /*if (offset == 0) {
                profileCacheRepository.cacheUserActivity(userId, activity)
            }*/

            activity
        }

    override suspend fun getUserCharts(userId: String, limit: Int, offset: Int, useCache: Boolean): Result<List<Chart>> =
        runCatching {
            // Only use cache for first page
            /*if (useCache && offset == 0) {
                profileCacheRepository.getUserLibrary(userId)?.let { cached ->
                    Log.d(TAG, "Returning cached library for user: $userId")
                    return@runCatching cached
                }
            }*/

            // Fetch from API
            val charts = apiClient.getUserCharts(userId, limit, offset)

            // Cache only first page
            /*if (offset == 0) {
                profileCacheRepository.cacheUserLibrary(userId, charts)
            }*/

            charts
        }

    override suspend fun followUser(userId: String): Result<Unit> = runCatching {
        apiClient.followUser(userId)

        // Invalidate the user profile cache since follow count changed
        // profileCacheRepository.invalidateUserProfile(userId)
    }

    override suspend fun unfollowUser(userId: String): Result<Unit> = runCatching {
        apiClient.unfollowUser(userId)

        // Invalidate the user profile cache since follow count changed
        // profileCacheRepository.invalidateUserProfile(userId)
    }
}
