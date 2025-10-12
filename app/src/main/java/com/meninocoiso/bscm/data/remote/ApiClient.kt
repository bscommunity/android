package com.meninocoiso.bscm.data.remote

import com.meninocoiso.bscm.data.remote.dto.collection.CreateCollectionRequest
import com.meninocoiso.bscm.data.remote.dto.collection.UpdateCollectionItemRequest
import com.meninocoiso.bscm.data.remote.dto.collection.UpdateCollectionRequest
import com.meninocoiso.bscm.domain.enums.ContentType
import com.meninocoiso.bscm.domain.enums.Difficulty
import com.meninocoiso.bscm.domain.enums.Genre
import com.meninocoiso.bscm.domain.enums.OperationOption
import com.meninocoiso.bscm.domain.enums.SortOption
import com.meninocoiso.bscm.domain.model.CatalogItem
import com.meninocoiso.bscm.domain.model.Chart
import com.meninocoiso.bscm.domain.model.User
import com.meninocoiso.bscm.domain.model.Version
import com.meninocoiso.bscm.domain.model.auth.AuthRequest
import com.meninocoiso.bscm.domain.model.auth.AuthResponse
import com.meninocoiso.bscm.domain.model.auth.RefreshTokenRequest
import com.meninocoiso.bscm.domain.model.Collection
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
        operationOption: OperationOption
    ): Boolean

    // Authentication methods
    suspend fun authenticateWithDiscord(authRequest: AuthRequest): AuthResponse
    suspend fun refreshToken(refreshRequest: RefreshTokenRequest): AuthResponse
    suspend fun getCurrentUser(): User

    // Utils methods
    suspend fun getContributors(): List<ContributionCategory>

    // Collections
    suspend fun getCollections(limit: Int?, offset: Int?): List<Collection>
    suspend fun createCollection(request: CreateCollectionRequest): Collection
    suspend fun updateCollection(collectionId: String, request: UpdateCollectionRequest): Boolean
    suspend fun deleteCollection(collectionId: String): Boolean

    // Collection Items
    suspend fun getCollectionItems(
        collectionId: String,
        category: ContentType,
        limit: Int? = null,
        offset: Int? = null
    ): List<CatalogItem>

    suspend fun addItemToCollection(collectionId: String, contentId: String): Boolean
    suspend fun removeItemFromCollection(collectionId: String, contentId: String): Boolean

    // Batch processing
    suspend fun batchProcessInteractions(interactions: List<UpdateCollectionItemRequest>): Boolean
}
