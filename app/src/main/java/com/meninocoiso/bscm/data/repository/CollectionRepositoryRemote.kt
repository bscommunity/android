package com.meninocoiso.bscm.data.repository

import android.util.Log
import com.meninocoiso.bscm.data.local.dao.CollectionDao
import com.meninocoiso.bscm.data.remote.ApiClient
import com.meninocoiso.bscm.domain.enums.CollectionKind
import com.meninocoiso.bscm.domain.model.Chart
import com.meninocoiso.bscm.domain.model.Collection
import com.meninocoiso.bscm.domain.repository.CollectionRepository
import jakarta.inject.Inject
import java.time.LocalDateTime

private const val TAG = "CollectionRepositoryRemote"

class CollectionRepositoryRemote @Inject constructor(
    private val apiClient: ApiClient,
    private val collectionDao: CollectionDao,
    private val profileCacheRepository: ProfileCacheRepository
) : CollectionRepository {
    override suspend fun getUserCollections(
        userId: String,
        limit: Int,
        offset: Int,
        useCache: Boolean
    ): Result<List<Collection>> = runCatching {
        if (userId == "user") {
            val localCollections = collectionDao.getUserCollections(limit, offset)
            if (useCache && offset == 0 && localCollections.isNotEmpty()) {
                Log.d(TAG, "Returning owner collections from Room (${localCollections.size} items)")
                return@runCatching localCollections
            }
            if (useCache && offset > 0) {
                return@runCatching emptyList()
            }
        }

        // Other profiles: quick cache by IDs + Room hydration
        if (userId != "user" && useCache && offset == 0) {
            val cachedIds = profileCacheRepository.getCollections(userId)
            if (!cachedIds.isNullOrEmpty()) {
                val cachedCollections = collectionDao.getCollectionsByIds(cachedIds)
                    .sortedBy { cachedIds.indexOf(it.id) }
                if (cachedCollections.isNotEmpty()) {
                    Log.d(TAG, "Returning cached collections for user $userId (${cachedCollections.size} items)")
                    return@runCatching cachedCollections
                }
            }
        }

        // Fetch from API
        val collections = if (userId == "user") {
            apiClient.getMyCollections(limit, offset)
        } else {
            apiClient.getUserCollections(userId, limit, offset)
        }

        val userCollections = collections.filter { it.kind == CollectionKind.USER }
        if (userCollections.isNotEmpty()) {
            collectionDao.upsertCollections(userCollections)
        }

        // Cache only first page
        if (offset == 0) {
            profileCacheRepository.cacheCollectionIds(userId, userCollections.map { it.id })
        }

        userCollections
    }

    override suspend fun createCollection(name: String, isPublic: Boolean): Result<Collection> =
        runCatching {
            val collection = apiClient.createCollection(name, isPublic)

            collectionDao.upsertCollection(collection)

            collection
        }

    override suspend fun updateCollection(
        collectionId: String,
        name: String?,
        isPublic: Boolean?
    ): Result<Unit> =
        runCatching {
            apiClient.updateCollection(collectionId, name, isPublic)

            collectionDao.updateCollectionMetadata(
                collectionId = collectionId,
                name = name,
                isPublic = isPublic,
                updatedAt = LocalDateTime.now()
            )
        }

    override suspend fun deleteCollection(collectionId: String): Result<Unit> = runCatching {
        apiClient.deleteCollection(collectionId)

        collectionDao.deleteCollectionById(collectionId)
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
