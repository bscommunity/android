package com.meninocoiso.bscm.data.repository

import android.util.Log
import com.meninocoiso.bscm.data.local.dao.ChartDao
import com.meninocoiso.bscm.data.local.dao.ThemeDao
import com.meninocoiso.bscm.data.local.dao.TourPassDao
import com.meninocoiso.bscm.data.remote.ApiClient
import com.meninocoiso.bscm.data.remote.dto.activity.ActivityItemResponse
import com.meninocoiso.bscm.data.remote.dto.user.UserProfileResponse
import com.meninocoiso.bscm.domain.enums.CatalogItemType
import com.meninocoiso.bscm.domain.model.CatalogItem
import com.meninocoiso.bscm.domain.model.Chart
import com.meninocoiso.bscm.domain.model.Theme
import com.meninocoiso.bscm.domain.model.TourPass
import com.meninocoiso.bscm.domain.repository.ProfileRepository
import com.meninocoiso.bscm.presentation.viewmodel.profile.PagedResult
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "ProfileRepositoryRemote"

class ProfileRepositoryRemote @Inject constructor(
    private val apiClient: ApiClient,
    private val profileCacheRepository: ProfileCacheRepository,
    private val chartDao: ChartDao,
    private val tourPassDao: TourPassDao,
    private val themeDao: ThemeDao,
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
    ): Result<PagedResult<CatalogItem>> =
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
                    val cachedItems = withContext(Dispatchers.IO) {
                        val charts = chartDao.getChartsByIds(
                            cached.items.filter { it.type == CatalogItemType.CHART.name }.map { it.id }
                        )
                        val tourPasses = tourPassDao.getTourPassesByIds(
                            cached.items.filter { it.type == CatalogItemType.TOUR_PASS.name }.map { it.id }
                        )
                        val themes = themeDao.getThemesByIds(
                            cached.items.filter { it.type == CatalogItemType.THEME.name }.map { it.id }
                        )
                        (charts + tourPasses + themes).distinctBy { it.id }
                    }
                    Log.d(TAG, "Cached library for user $userId: ${cachedItems.size} items")
                    return@runCatching PagedResult(cachedItems, cached.total)
                }
            }

            // Fetch from API
            val page = apiClient.getUserCharts(userId, limit, offset)
            val charts = page.items.filterIsInstance<Chart>()
            val tourPasses = page.items.filterIsInstance<TourPass>()
            val themes = page.items.filterIsInstance<Theme>()
            Log.d(
                TAG,
                "Fetched library for user $userId from API (${charts.size} charts, " +
                        "${tourPasses.size} tour passes, ${themes.size} themes)"
            )

            // Cache only first page — persist must complete before caching IDs so
            // that a subsequent getByIds() call finds the rows in the DB/memory store.
            val total = if (offset == 0) {
                page.counts?.charts?.toLong().also { t ->
                    withContext(Dispatchers.IO) {
                        if (charts.isNotEmpty()) chartDao.insert(charts)
                        if (tourPasses.isNotEmpty()) tourPassDao.insert(tourPasses)
                        if (themes.isNotEmpty()) themeDao.insert(themes)
                    }
                    profileCacheRepository.cacheLibrary(userId, page.items, t)
                }
            } else null

            PagedResult(
                items = page.items,
                total = total?.toInt(),
                counts = page.counts?.let { Triple(it.charts, it.tourPasses, it.themes) },
            )
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
