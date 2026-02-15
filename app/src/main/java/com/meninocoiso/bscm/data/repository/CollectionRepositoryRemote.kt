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
    override suspend fun getUserCollections(
        userId: String,
        limit: Int,
        offset: Int,
        useCache: Boolean
    ): Result<List<Collection>> = runCatching {
        // Only use cache for first page
        if (useCache && offset == 0) {
            profileCacheRepository.getCollections(userId)?.let { cached ->
                Log.d(TAG, "Returning cached collections (${cached.size} items)")
                return@runCatching cached
            }
        }

        // Fetch from API
        val collections = if (userId == "user") {
            apiClient.getMyCollections(limit, offset)
        } else {
            apiClient.getUserCollections(userId, limit, offset)
        }

        // Cache only first page
        if (offset == 0) {
            profileCacheRepository.cacheCollections(userId, collections)
        }

        collections
    }

    override suspend fun createCollection(name: String, isPublic: Boolean): Result<Collection> =
        runCatching {
            val collection = apiClient.createCollection(name, isPublic)

            // Add to cache immediately so it shows up in UI without needing to refetch
            profileCacheRepository.addCollection(collection = collection)

            collection
        }

    override suspend fun updateCollection(
        collectionId: String,
        name: String?,
        isPublic: Boolean?
    ): Result<Unit> =
        runCatching {
            apiClient.updateCollection(collectionId, name, isPublic)

            // Update cache immediately so it reflects in UI without needing to refetch
            profileCacheRepository.updateCollection(
                collectionId = collectionId,
                name = name,
                isPublic = isPublic
            )
        }

    override suspend fun deleteCollection(collectionId: String): Result<Unit> = runCatching {
        apiClient.deleteCollection(collectionId)

        // Remove from cache immediately so it reflects in UI without needing to refetch
        profileCacheRepository.removeCollection(collectionId = collectionId)
    }

    override suspend fun getCollectionItems(
        collectionId: String,
        limit: Int,
        offset: Int,
        contentType: String?
    ): Result<List<Chart>> = runCatching {
        apiClient.getCollectionItems(collectionId, contentType, limit, offset)
    }

    override suspend fun addItemToCollection(
        collectionId: String,
        contentId: String
    ): Result<Unit> = runCatching {
        apiClient.addItemToCollection(collectionId, contentId)
    }

    override suspend fun removeItemFromCollection(
        collectionId: String,
        contentId: String
    ): Result<Unit> = runCatching {
        apiClient.removeItemFromCollection(collectionId, contentId)
    }
}
