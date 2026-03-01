package com.meninocoiso.bscm.data.repository

import android.util.Log
import com.meninocoiso.bscm.data.local.dao.ChartDao
import com.meninocoiso.bscm.data.remote.ApiClient
import com.meninocoiso.bscm.data.remote.dto.activity.ActivityItemResponse
import com.meninocoiso.bscm.data.remote.dto.user.UserProfileResponse
import com.meninocoiso.bscm.domain.model.Chart
import com.meninocoiso.bscm.domain.repository.ProfileRepository
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "ProfileRepositoryRemote"

class ProfileRepositoryRemote @Inject constructor(
    private val apiClient: ApiClient,
    private val profileCacheRepository: ProfileCacheRepository,
    private val chartDao: ChartDao
) : ProfileRepository {
    override suspend fun getProfileHeader(
        username: String,
        useCache: Boolean
    ): Result<UserProfileResponse> = runCatching {

        Log.d(TAG, "Fetching profile for user: $username (useCache=$useCache)")

        // Try cache first if requested
        if (useCache) {
            profileCacheRepository.getProfile(username)?.let { cached ->
                Log.d(TAG, "Returning cached profile for user: $username")
                return@runCatching cached
            }
        }

        // Fetch from API
        val profile = apiClient.getUserProfileByUsername(username, setOf("library"))
        Log.d(TAG, "Fetched profile for user $username from API: $profile")

        // Cache the result
        profileCacheRepository.cacheProfile(username, profile)

        profile
    }

    override suspend fun getActivity(
        userId: String,
        limit: Int,
        offset: Int,
        useCache: Boolean
    ): Result<List<ActivityItemResponse>> =
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

    override suspend fun getUserCharts(
        userId: String,
        limit: Int,
        offset: Int,
        useCache: Boolean
    ): Result<List<Chart>> =
        runCatching {
            Log.d(
                TAG,
                "Getting library for user $userId (limit=$limit, offset=$offset, useCache=$useCache)"
            )

            // Only use cache for first page
            if (useCache && offset == 0) {
                val cached = profileCacheRepository.getLibrary(userId)
                if (cached != null) {
                    Log.d(TAG, "Returning cached library for user: $userId")
                    val cachedCharts =
                        withContext(Dispatchers.IO) { chartDao.getChartsByIds(cached) }
                    Log.d(TAG, "Cached charts for user $userId: ${cachedCharts.size} items")
                    return@runCatching cachedCharts
                }
            }

            // Fetch from API
            val charts = apiClient.getUserCharts(userId, limit, offset)
            Log.d(
                TAG,
                "Fetched library charts for user $userId from API (${charts.size} items)"
            )

            // Cache only first page — persist must complete before caching IDs so
            // that a subsequent getChartsById() call finds the rows in the DB/memory store.
            if (offset == 0) {
                withContext(Dispatchers.IO) { chartDao.insert(charts) }
                profileCacheRepository.cacheLibrary(userId, charts.map { it.id })
            }

            charts
        }

    override suspend fun followUser(userId: String, username: String): Result<Unit> = runCatching {
        apiClient.followUser(userId)

        val profile = profileCacheRepository.getProfile(username) ?: return@runCatching

        profileCacheRepository.cacheProfile(
            username,
            profile.copy(isFollowing = true)
        )
    }

    override suspend fun unfollowUser(userId: String, username: String): Result<Unit> = runCatching {
        apiClient.unfollowUser(userId)

        val profile = profileCacheRepository.getProfile(username) ?: return@runCatching

        profileCacheRepository.cacheProfile(
            username,
            profile.copy(isFollowing = false)
        )
    }
}
