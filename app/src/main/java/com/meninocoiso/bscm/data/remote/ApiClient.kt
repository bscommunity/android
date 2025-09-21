package com.meninocoiso.bscm.data.remote

import com.meninocoiso.bscm.data.remote.dto.LikeRequest
import com.meninocoiso.bscm.domain.enums.ContentType
import com.meninocoiso.bscm.domain.enums.Difficulty
import com.meninocoiso.bscm.domain.enums.Genre
import com.meninocoiso.bscm.domain.enums.OperationType
import com.meninocoiso.bscm.domain.enums.SortOption
import com.meninocoiso.bscm.domain.model.Chart
import com.meninocoiso.bscm.domain.model.LikedContent
import com.meninocoiso.bscm.domain.model.User
import com.meninocoiso.bscm.domain.model.Version
import com.meninocoiso.bscm.domain.model.auth.AuthRequest
import com.meninocoiso.bscm.domain.model.auth.AuthResponse
import com.meninocoiso.bscm.domain.model.auth.RefreshTokenRequest
import com.meninocoiso.bscm.domain.model.collection.AddItemRequest
import com.meninocoiso.bscm.domain.model.collection.Collection
import com.meninocoiso.bscm.domain.model.collection.CollectionItem
import com.meninocoiso.bscm.domain.model.collection.CreateCollectionRequest
import com.meninocoiso.bscm.domain.model.collection.UpdateCollectionRequest
import com.meninocoiso.bscm.domain.model.internal.ContributionCategory

interface ApiClient {
    suspend fun getUsers(): List<User>
    suspend fun getUser(id: String): User
    suspend fun getChart(id: String): Chart
    suspend fun getFeedCharts(sortBy: SortOption, limit: Int? = 10, offset: Int = 0): List<Chart>
    suspend fun getCharts(
        query: String?,
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
        operationType: OperationType
    ): Boolean

    // Authentication methods
    suspend fun authenticateWithDiscord(authRequest: AuthRequest): AuthResponse
    suspend fun refreshToken(refreshRequest: RefreshTokenRequest): AuthResponse
    suspend fun getCurrentUser(): User

    // Utils methods
    suspend fun getContributors(): List<ContributionCategory>

    // Like-related methods
    suspend fun getUserLikes(contentType: ContentType? = null, limit: Int? = null, offset: Int = 0): List<LikedContent>
    suspend fun likeContent(request: LikeRequest): Boolean
    suspend fun unlikeContent(contentType: ContentType, contentId: ULong): Boolean
    suspend fun isContentLiked(contentType: ContentType, contentId: ULong): Boolean

    // Collections
    suspend fun getPublicCollections(limit: Int? = null, offset: Int? = null): List<Collection>
    suspend fun getCollection(collectionId: ULong, userId: String? = null): Collection?
    suspend fun getCollectionItems(collectionId: ULong, userId: String? = null): List<CollectionItem>
    suspend fun getUserCollections(userId: String): List<Collection>
    suspend fun createCollection(userId: String, request: CreateCollectionRequest): Collection
    suspend fun updateCollection(collectionId: ULong, userId: String, request: UpdateCollectionRequest): Boolean
    suspend fun deleteCollection(collectionId: ULong, userId: String): Boolean
    suspend fun addItemToCollection(collectionId: ULong, userId: String, request: AddItemRequest): Boolean
    suspend fun removeItemFromCollection(collectionId: ULong, userId: String, contentType: ContentType, contentId: ULong): Boolean
    suspend fun getUserCollectionsContaining(userId: String, contentType: ContentType, contentId: ULong): List<Collection>
    
    // Favorites
    suspend fun getUserFavorites(userId: String): List<CollectionItem>
    suspend fun addToFavorites(userId: String, request: AddItemRequest): Boolean
    suspend fun removeFromFavorites(userId: String, contentType: ContentType, contentId: ULong): Boolean
    suspend fun isInFavorites(userId: String, contentType: ContentType, contentId: ULong): Boolean
}
