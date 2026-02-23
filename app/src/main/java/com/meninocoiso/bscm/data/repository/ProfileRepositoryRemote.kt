package com.meninocoiso.bscm.data.repository

import android.util.Log
import com.meninocoiso.bscm.data.manager.ChartManager
import com.meninocoiso.bscm.data.remote.ApiClient
import com.meninocoiso.bscm.data.remote.dto.activity.ActivityItemResponse
import com.meninocoiso.bscm.data.remote.dto.user.UserProfileResponse
import com.meninocoiso.bscm.domain.model.Chart
import com.meninocoiso.bscm.domain.repository.ProfileRepository
import com.meninocoiso.bscm.domain.result.ContentResult
import jakarta.inject.Inject
import kotlinx.coroutines.flow.first

private const val TAG = "ProfileRepositoryRemote"

class ProfileRepositoryRemote @Inject constructor(
    private val apiClient: ApiClient,
    private val profileCacheRepository: ProfileCacheRepository,
    private val chartManager: ChartManager
) : ProfileRepository {
    override suspend fun getProfileHeader(username: String, useCache: Boolean): Result<UserProfileResponse> = runCatching {

        Log.d(TAG, "Fetching profile for user: $username (useCache=$useCache)")

        // Try cache first if requested
        if (useCache) {
            profileCacheRepository.getProfile(username)?.let { cached ->
                Log.d(TAG, "Returning cached profile for user: $username")
                return@runCatching cached
            }
        }

        // Fetch from API
        val profile = apiClient.getUserProfileByUsername(username)

        // Cache the result
        profileCacheRepository.cacheProfile(username, profile)

        profile
    }

    override suspend fun getActivity(userId: String, limit: Int, offset: Int, useCache: Boolean): Result<List<ActivityItemResponse>> =
        runCatching {
            // Only use cache for first page
            if (useCache && offset == 0) {
                profileCacheRepository.getActivity(userId)?.let { cached ->
                    Log.d(TAG, "Returning cached activity for user: $userId")
                    return@runCatching cached
                }
            }

            // Fetch from API
            val activity = apiClient.getUserActivity(userId, limit, offset)
            // Update local cache

            // Cache only first page
            if (offset == 0) {
                profileCacheRepository.cacheActivity(userId, activity)
            }

            activity
        }

    override suspend fun getUserCharts(userId: String, limit: Int, offset: Int, useCache: Boolean): Result<List<Chart>> =
        runCatching {
            // Only use cache for first page
            if (useCache && offset == 0) {
                val cached = profileCacheRepository.getLibrary(userId)
                if (cached != null) {
                    Log.d(TAG, "Returning cached library for user: $userId")
                    when (val charts = chartManager.getChartsById(cached).first { it !is ContentResult.Loading }) {
                        is ContentResult.Success -> return@runCatching charts.data
                        is ContentResult.Error -> Log.e(TAG, "Error fetching charts for cached library: ${charts.message}")
                        else -> {}
                    }
                }
            }

            // Fetch from API
            val charts = apiClient.getUserCharts(userId, limit, offset)
            // Update local cache

            // Cache only first page
            if (offset == 0) {
                profileCacheRepository.cacheLibrary(userId, charts.map { it.id })
            }

            charts
        }

    override suspend fun followUser(userId: String): Result<Unit> = runCatching {
        apiClient.followUser(userId)

        profileCacheRepository.invalidateProfile(userId)
    }

    override suspend fun unfollowUser(userId: String): Result<Unit> = runCatching {
        apiClient.unfollowUser(userId)

        profileCacheRepository.invalidateProfile(userId)
    }
}
