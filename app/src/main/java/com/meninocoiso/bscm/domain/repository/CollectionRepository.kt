package com.meninocoiso.bscm.domain.repository

import com.meninocoiso.bscm.domain.model.Chart
import com.meninocoiso.bscm.domain.model.Collection

interface CollectionRepository {
    suspend fun getUserCollections(userId: String = "user", limit: Int, offset: Int, useCache: Boolean = true): Result<List<Collection>>
    suspend fun createCollection(name: String, isPublic: Boolean): Result<Collection>
    suspend fun updateCollection(collectionId: String, name: String?, isPublic: Boolean?): Result<Unit>
    suspend fun deleteCollection(collectionId: String): Result<Unit>

    suspend fun getCollectionItems(
        collectionId: String,
        limit: Int,
        offset: Int,
        contentType: String? = null
    ): Result<List<Chart>>

    suspend fun addItemToCollection(collectionId: String, contentId: String): Result<Unit>
    suspend fun removeItemFromCollection(collectionId: String, contentId: String): Result<Unit>
}
