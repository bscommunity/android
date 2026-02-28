package com.meninocoiso.bscm.data.repository

import android.util.Log
import com.meninocoiso.bscm.data.local.dao.CollectionDao
import com.meninocoiso.bscm.data.remote.ApiClient
import com.meninocoiso.bscm.domain.enums.CollectionKind
import com.meninocoiso.bscm.domain.enums.ContentType
import com.meninocoiso.bscm.domain.model.Chart
import com.meninocoiso.bscm.domain.model.Collection
import com.meninocoiso.bscm.domain.model.CollectionItemCrossRef
import com.meninocoiso.bscm.domain.repository.CollectionRepository
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow
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
        Log.d(TAG, "Getting collections for user $userId (limit=$limit, offset=$offset, useCache=$useCache)")

        if (userId == "user" && useCache && offset == 0) {
            val localCollections = collectionDao.getUserCollections(limit, offset)
            if (localCollections.isNotEmpty()) {
                Log.d(TAG, "Returning owner collections from Room (${localCollections.size} items)")
                return@runCatching localCollections
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
        Log.d(TAG, "Fetched collections for user $userId from API (${collections.size} items)")

        val userCollections = collections.filter { it.kind == CollectionKind.USER }
        if (userCollections.isNotEmpty()) {
            collectionDao.upsertCollections(userCollections)
        }

        // Cache only first page
        if (offset == 0) {
            profileCacheRepository.cacheCollectionIds(userId, userCollections.map { it.id })
        }

        userCollections
    }.onFailure { error ->
        Log.e(TAG, "Failed to get collections for user $userId: ${error.message}", error)
    }

    override suspend fun getCollectionById(collectionId: String): Result<Collection> = runCatching {
        val cached = collectionDao.getCollectionsByIds(listOf(collectionId)).firstOrNull()
        if (cached != null) return@runCatching cached

        val collection = apiClient.getCollection(collectionId)
        collectionDao.upsertCollection(collection)
        collection
    }

    override suspend fun getCollectionBySlug(username: String, slug: String): Result<Collection> = runCatching {
        val collection = apiClient.getCollectionBySlug(username, slug)
        collectionDao.upsertCollection(collection)
        collection
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
    ): Result<String?> =
        runCatching {
            val slug = apiClient.updateCollection(collectionId, name, isPublic)
            println("Updated collection $collectionId with name=$name, isPublic=$isPublic, new slug=$slug")

            collectionDao.updateCollectionMetadata(
                collectionId = collectionId,
                name = name,
                isPublic = isPublic,
                slug = slug,
                updatedAt = LocalDateTime.now()
            )

            slug
        }

    override suspend fun deleteCollection(collectionId: String): Result<Unit> = runCatching {
        apiClient.deleteCollection(collectionId)

        collectionDao.deleteCollectionById(collectionId)
    }

    override suspend fun getCollectionItems(
        collectionId: String,
        limit: Int,
        offset: Int,
        contentType: String?,
        useCache: Boolean
    ): Result<List<Chart>> = runCatching {
        Log.d(TAG, "Getting items for collection $collectionId (limit=$limit, offset=$offset, useCache=$useCache)")

        // Return Room-cached items on the first page when cache is allowed
        if (useCache && offset == 0) {
            val cached = collectionDao.getChartItems(collectionId, limit, offset)
            if (cached.isNotEmpty()) {
                Log.d(TAG, "Returning cached items for collection $collectionId (${cached.size} items)")
                return@runCatching cached
            }
        }

        // Fetch from API
        val items = apiClient.getCollectionItems(collectionId, contentType, limit, offset)
        Log.d(TAG, "Fetched ${items.size} items for collection $collectionId from API")

        // Persist charts to Room and update cross-refs (first page only to avoid stale data)
        if (offset == 0 && items.isNotEmpty()) {
            collectionDao.upsertCharts(items)
            val crossRefs = items.map { chart ->
                CollectionItemCrossRef(
                    collectionId = collectionId,
                    contentId = chart.contentId ?: chart.id,
                    contentType = ContentType.CHART,
                )
            }
            collectionDao.upsertCrossRefs(crossRefs)
        }

        items
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

    override fun observeUserCollections(): Flow<List<Collection>> =
        collectionDao.observeUserCollections()
}
