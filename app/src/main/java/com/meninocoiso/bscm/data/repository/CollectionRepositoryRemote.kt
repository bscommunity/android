package com.meninocoiso.bscm.data.repository

import android.util.Log
import com.meninocoiso.bscm.data.remote.ApiClient
import com.meninocoiso.bscm.domain.model.Chart
import com.meninocoiso.bscm.domain.model.Collection
import com.meninocoiso.bscm.domain.repository.CollectionRepository
import jakarta.inject.Inject

private const val TAG = "CollectionRepositoryRemote"

class CollectionRepositoryRemote @Inject constructor(
    private val apiClient: ApiClient,
    private val profileCacheRepository: ProfileCacheRepository
) : CollectionRepository {
    override suspend fun getUserCollections(limit: Int, offset: Int, useCache: Boolean): Result<List<Collection>> = runCatching {
        // Only use cache for first page
        if (useCache && offset == 0) {
            profileCacheRepository.getCollections("")?.let { cached ->
                Log.d(TAG, "Returning cached collections (${cached.size} items)")
                return@runCatching cached
            }
        }

        // Fetch from API
        val collections = apiClient.getUserCollections(limit, offset)

        // Cache only first page
        if (offset == 0) {
            profileCacheRepository.cacheCollections("", collections)
        }

        collections
    }

    override suspend fun createCollection(name: String, isPublic: Boolean): Result<Collection> = runCatching {
        val collection = apiClient.createCollection(name, isPublic)

        // Invalidate cache so it refreshes on next fetch
        // profileCacheRepository.invalidateMyCollections()

        collection
    }

    override suspend fun updateCollection(collectionId: String, name: String?, isPublic: Boolean?): Result<Unit> =
        runCatching {
            apiClient.updateCollection(collectionId, name, isPublic)

            // Invalidate cache so it refreshes on next fetch
            // profileCacheRepository.invalidateMyCollections()

            Unit
        }

    override suspend fun deleteCollection(collectionId: String): Result<Unit> = runCatching {
        apiClient.deleteCollection(collectionId)

        // Invalidate cache so it refreshes on next fetch
        // profileCacheRepository.invalidateMyCollections()

        Unit
    }

    override suspend fun getCollectionItems(
        collectionId: String,
        limit: Int,
        offset: Int,
        contentType: String?
    ): Result<List<Chart>> = runCatching {
        apiClient.getCollectionItems(collectionId, contentType, limit, offset)
    }

    override suspend fun addItemToCollection(collectionId: String, contentId: String): Result<Unit> = runCatching {
        apiClient.addItemToCollection(collectionId, contentId)
        Unit
    }

    override suspend fun removeItemFromCollection(collectionId: String, contentId: String): Result<Unit> = runCatching {
        apiClient.removeItemFromCollection(collectionId, contentId)
        Unit
    }
}
