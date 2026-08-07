package com.meninocoiso.bscm.domain.repository

import com.meninocoiso.bscm.domain.enums.CatalogItemType
import com.meninocoiso.bscm.domain.model.CatalogItem
import com.meninocoiso.bscm.domain.model.Collection
import com.meninocoiso.bscm.presentation.viewmodel.profile.PagedResult
import kotlinx.coroutines.flow.Flow

interface CollectionRepository {
    suspend fun getUserCollections(userId: String = "user", limit: Int, offset: Int, useCache: Boolean = true): Result<PagedResult<Collection>>
    suspend fun getCollectionById(collectionId: String): Result<Collection>
    suspend fun getCollectionBySlug(username: String, slug: String): Result<Collection>
    suspend fun createCollection(name: String, isPublic: Boolean): Result<Collection>
    suspend fun updateCollection(collectionId: String, name: String?, isPublic: Boolean?): Result<String?>
    suspend fun deleteCollection(collectionId: String): Result<Unit>

    suspend fun getCollectionItems(
        collectionId: String,
        limit: Int,
        offset: Int,
        types: List<CatalogItemType>? = null,
        useCache: Boolean = true
    ): Result<PagedResult<CatalogItem>>

    suspend fun addItemToCollection(collectionId: String, id: String): Result<Unit>
    suspend fun removeItemFromCollection(collectionId: String, id: String): Result<Unit>

    fun observeCollectionChartIds(collectionId: String): Flow<List<String>>
    fun observeUserCollections(): Flow<List<Collection>>
}
