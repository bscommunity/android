package com.meninocoiso.bscm.data.remote

import com.meninocoiso.bscm.data.remote.dto.activity.ActivityEntry
import com.meninocoiso.bscm.data.remote.dto.collection.CreateCollectionItemRequest
import com.meninocoiso.bscm.data.remote.dto.user.UserProfileResponse
import com.meninocoiso.bscm.domain.enums.Difficulty
import com.meninocoiso.bscm.domain.enums.Genre
import com.meninocoiso.bscm.domain.enums.OperationOption
import com.meninocoiso.bscm.domain.enums.SortOption
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
    suspend fun getCharts(
        query: String?,
        sortBy: SortOption? = null,
        difficulties: List<Difficulty>? = null,
        genres: List<Genre>? = null,
        limit: Int? = 10,
        offset: Int = 0
    ): List<Chart>

    suspend fun getChartsById(ids: List<String>): List<Chart>
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
    suspend fun getUserProfileByUsername(username: String): UserProfileResponse
    suspend fun getUserActivity(id: String, limit: Int? = null, offset: Int? = null): List<ActivityEntry>
    suspend fun getUserCharts(id: String, limit: Int? = null, offset: Int? = null): List<Chart>
    suspend fun followUser(id: String): Boolean
    suspend fun unfollowUser(id: String): Boolean

    // Me
    suspend fun getMyProfile(): UserProfileResponse
    suspend fun getMyActivity(limit: Int? = null, offset: Int? = null): List<ActivityEntry>
    suspend fun getMyLikes(limit: Int? = null, offset: Int? = null): List<Chart>
    suspend fun getMyBookmarks(limit: Int? = null, offset: Int? = null): List<Chart>

    // Collections
    suspend fun getUserCollections(limit: Int? = null, offset: Int? = null): List<Collection>
    suspend fun createCollection(name: String, isPublic: Boolean): Collection
    suspend fun updateCollection(collectionId: String, name: String?, isPublic: Boolean?): Boolean
    suspend fun deleteCollection(collectionId: String): Boolean
    suspend fun getCollectionItems(
        collectionId: String,
        contentType: String? = null,
        limit: Int? = null,
        offset: Int? = null
    ): List<Chart>
    suspend fun addItemToCollection(collectionId: String, contentId: String): Boolean
    suspend fun removeItemFromCollection(collectionId: String, contentId: String): Boolean

    // Batch interactions
    suspend fun batchProcessInteractions(interactions: List<CreateCollectionItemRequest>): Boolean

    // Utils methods
    suspend fun getContributors(): List<ContributionCategory>
}
