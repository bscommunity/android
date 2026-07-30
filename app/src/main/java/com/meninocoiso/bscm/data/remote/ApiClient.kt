package com.meninocoiso.bscm.data.remote

import com.meninocoiso.bscm.data.remote.dto.BundleDownloadResponse
import com.meninocoiso.bscm.data.remote.dto.activity.ActivityItemResponse
import com.meninocoiso.bscm.data.remote.dto.collection.BatchCollectionItemRequest
import com.meninocoiso.bscm.data.remote.dto.user.ItemsPage
import com.meninocoiso.bscm.data.remote.dto.user.UserProfileResponse
import com.meninocoiso.bscm.domain.enums.CatalogItemType
import com.meninocoiso.bscm.domain.enums.Difficulty
import com.meninocoiso.bscm.domain.enums.Genre
import com.meninocoiso.bscm.domain.enums.OperationOption
import com.meninocoiso.bscm.domain.enums.SortOption
import com.meninocoiso.bscm.domain.model.CatalogItem
import com.meninocoiso.bscm.domain.model.Chart
import com.meninocoiso.bscm.domain.model.Collection
import com.meninocoiso.bscm.domain.model.User
import com.meninocoiso.bscm.domain.model.Version
import com.meninocoiso.bscm.domain.model.auth.AuthRequest
import com.meninocoiso.bscm.domain.model.auth.AuthResponse
import com.meninocoiso.bscm.domain.model.auth.RefreshTokenRequest
import com.meninocoiso.bscm.domain.model.internal.ContributionCategory

interface ApiClient {
    suspend fun getChart(id: String): Chart
    suspend fun getChartByContentId(contentId: String): Chart
    suspend fun getCharts(
        query: String?,
        sortBy: SortOption? = null,
        difficulties: List<Difficulty>? = null,
        genres: List<Genre>? = null,
        limit: Int? = 10,
        offset: Int = 0
    ): List<Chart>
    suspend fun getChartsByContentIds(contentIds: List<String>): List<Chart>

    suspend fun getChartsByIds(ids: List<String>): List<Chart>
    suspend fun getSuggestions(query: String, limit: Int? = null): List<String>
    suspend fun getLatestVersionsByChartIds(ids: List<String>): List<Version>
    suspend fun postAnalytics(
        id: String,
        operationOption: OperationOption
    ): Boolean

    // Authentication methods
    suspend fun authenticateWithDiscord(authRequest: AuthRequest): AuthResponse
    suspend fun refreshToken(refreshRequest: RefreshTokenRequest): AuthResponse
    suspend fun getCurrentUser(): User

    // Users
    suspend fun getUsers(search: String? = null): List<User>
    suspend fun getUser(id: String): User
    suspend fun getUserProfile(id: String): UserProfileResponse
    suspend fun getUserProfileByUsername(username: String, counts: Set<String> = emptySet()): UserProfileResponse
    suspend fun getUserActivity(id: String, limit: Int? = null, offset: Int? = null): List<ActivityItemResponse>
    suspend fun getUserCharts(id: String, limit: Int? = null, offset: Int? = null): ItemsPage<Chart>
    // suspend fun getUserTourPasses(userId: String, limit: Int? = null, offset: Int? = null): ItemsPage<TourPass>
    // suspend fun getUserThemes(userId: String, limit: Int? = null, offset: Int? = null): ItemsPage<Theme>
    suspend fun getUserCollections(userId: String, limit: Int? = null, offset: Int? = null): ItemsPage<Collection>
    suspend fun followUser(id: String): Boolean
    suspend fun unfollowUser(id: String): Boolean

    // Me
    suspend fun getMyProfile(): UserProfileResponse
    suspend fun getMyActivity(limit: Int? = null, offset: Int? = null): List<ActivityItemResponse>
    suspend fun getMyCollections(limit: Int? = null, offset: Int? = null): ItemsPage<Collection>
    suspend fun getMyLikes(limit: Int? = null, offset: Int? = null, types: List<CatalogItemType>? = null): ItemsPage<Chart>
    suspend fun getMyBookmarks(limit: Int? = null, offset: Int? = null, types: List<CatalogItemType>? = null): ItemsPage<Chart>
    suspend fun addLike(contentId: String): Boolean
    suspend fun removeLike(contentId: String): Boolean
    suspend fun addBookmark(contentId: String): Boolean
    suspend fun removeBookmark(contentId: String): Boolean

    // Collections
    suspend fun getCollection(collectionId: String): Collection
    suspend fun getCollectionBySlug(username: String, slug: String): Collection
    suspend fun createCollection(name: String, isPublic: Boolean): Collection
    suspend fun updateCollection(collectionId: String, name: String?, isPublic: Boolean?): String?
    suspend fun deleteCollection(collectionId: String): Boolean
    suspend fun getCollectionItems(
        collectionId: String,
        types: List<CatalogItemType>? = null,
        limit: Int? = null,
        offset: Int? = null
    ): ItemsPage<CatalogItem>
    suspend fun addItemToCollection(collectionId: String, contentId: String): Boolean
    suspend fun removeItemFromCollection(collectionId: String, contentId: String): Boolean

    // Batch interactions
    suspend fun batchProcessInteractions(interactions: List<BatchCollectionItemRequest>): Boolean

    // Bundle download URLs
    suspend fun getChartBundleUrl(id: String): BundleDownloadResponse
    suspend fun getThemeBundleUrl(id: String): BundleDownloadResponse

    // Utils methods
    suspend fun getContributors(): List<ContributionCategory>
}
