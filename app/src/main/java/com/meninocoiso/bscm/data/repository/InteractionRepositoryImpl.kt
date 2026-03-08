package com.meninocoiso.bscm.data.repository

import android.util.Log
import com.meninocoiso.bscm.data.local.dao.ChartDao
import com.meninocoiso.bscm.data.local.dao.CollectionDao
import com.meninocoiso.bscm.data.manager.ChartManager
import com.meninocoiso.bscm.data.manager.InteractionQueueManager
import com.meninocoiso.bscm.domain.enums.CollectionKind
import com.meninocoiso.bscm.domain.enums.ContentType
import com.meninocoiso.bscm.domain.enums.OperationOption
import com.meninocoiso.bscm.domain.model.Collection
import com.meninocoiso.bscm.domain.model.CollectionItemCrossRef
import com.meninocoiso.bscm.domain.repository.InteractionRepository
import com.meninocoiso.bscm.domain.result.ContentResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "InteractionRepositoryImpl"
private const val BOOKMARKS_COLLECTION_ID = "bookmarks"
private const val LOCAL_USER_ID = "user"

@Singleton
class InteractionRepositoryImpl @Inject constructor(
    private val queueManager: InteractionQueueManager,
    private val chartManager: ChartManager,
    private val chartDao: ChartDao,
    private val collectionDao: CollectionDao,
    private val profileCacheRepository: ProfileCacheRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : InteractionRepository {

    override suspend fun likeContent(id: String, contentId: String): Result<Unit> =
        withContext(dispatcher) {
            runCatching {
                val shouldIncrement = chartDao.getChart(id)?.likedAt == null
                updateLocalState(id = id, operation = OperationOption.LIKE)
                if (shouldIncrement) {
                    profileCacheRepository.adjustLikesCount(delta = 1)
                }
                queueManager.queueAndSyncLike(contentId, isLike = true)
            }.onFailure { e ->
                Log.e(TAG, "Unexpected error in likeContent for contentId: $contentId", e)
            }
        }

    override suspend fun unlikeContent(id: String, contentId: String): Result<Unit> =
        withContext(dispatcher) {
            runCatching {
                val shouldDecrement = chartDao.getChart(id)?.likedAt != null
                updateLocalState(id = id, operation = OperationOption.UNLIKE)
                if (shouldDecrement) {
                    profileCacheRepository.adjustLikesCount(delta = -1)
                }
                queueManager.queueAndSyncLike(contentId, isLike = false)
            }.onFailure { e ->
                Log.e(TAG, "Unexpected error in unlikeContent for contentId: $contentId", e)
            }
        }

    override suspend fun bookmarkContent(id: String, contentId: String): Result<Unit> =
        withContext(dispatcher) {
            runCatching {
                val chart = chartDao.getChart(id)
                val hasBookmarkMembership = collectionDao.hasCrossRef(BOOKMARKS_COLLECTION_ID, contentId)
                val userCollectionIds = collectionDao.getUserCollectionIdsForContent(contentId)
                val wasSavedLocally = chart?.bookmarkedAt != null || hasBookmarkMembership || userCollectionIds.isNotEmpty()

                ensureBookmarksCollectionExists()
                if (!hasBookmarkMembership) {
                    collectionDao.upsertCrossRef(
                        CollectionItemCrossRef(
                            collectionId = BOOKMARKS_COLLECTION_ID,
                            contentId = contentId,
                            contentType = ContentType.CHART
                        )
                    )
                }
                if (!wasSavedLocally) {
                    updateLocalState(id = id, operation = OperationOption.BOOKMARK)
                    profileCacheRepository.adjustBookmarksCount(delta = 1)
                }
                queueManager.queueAndSyncBookmark(contentId, isBookmarked = true)
            }.onFailure { e ->
                Log.e(TAG, "Unexpected error in bookmarkContent for contentId: $contentId", e)
            }
        }

    override suspend fun unbookmarkContent(id: String, contentId: String): Result<Unit> =
        withContext(dispatcher) {
            runCatching {
                val chart = chartDao.getChart(id)
                val hasBookmarkMembership = collectionDao.hasCrossRef(BOOKMARKS_COLLECTION_ID, contentId)
                val userCollectionIds = collectionDao.getUserCollectionIdsForContent(contentId)
                val wasSavedLocally = chart?.bookmarkedAt != null || hasBookmarkMembership || userCollectionIds.isNotEmpty()

                collectionDao.deleteCrossRef(BOOKMARKS_COLLECTION_ID, contentId)
                if (userCollectionIds.isNotEmpty()) {
                    collectionDao.deleteUserCrossRefsForContent(contentId)
                 }
                if (wasSavedLocally) {
                    updateLocalState(id = id, operation = OperationOption.UNBOOKMARK)
                    profileCacheRepository.adjustBookmarksCount(delta = -1)
                }
                queueManager.queueAndSyncBookmark(contentId, isBookmarked = false)
                userCollectionIds.forEach { collectionId ->
                    queueManager.queueAndSyncCollection(contentId, collectionId, isAdd = false)
                }
            }.onFailure { e ->
                Log.e(TAG, "Unexpected error in unbookmarkContent for contentId: $contentId", e)
            }
        }

    override suspend fun addToCollection(
        id: String,
        contentId: String,
        collectionId: String
    ): Result<Unit> =
        withContext(dispatcher) {
            runCatching {
                val chart = chartDao.getChart(id)
                val hasBookmarkMembership = collectionDao.hasCrossRef(BOOKMARKS_COLLECTION_ID, contentId)
                val hasCollectionMembership = collectionDao.hasCrossRef(collectionId, contentId)
                val userCollectionIds = collectionDao.getUserCollectionIdsForContent(contentId)
                val wasSavedLocally = chart?.bookmarkedAt != null || hasBookmarkMembership || userCollectionIds.isNotEmpty()
                val now = LocalDateTime.now()

                ensureBookmarksCollectionExists(now)
                if (!hasBookmarkMembership) {
                    collectionDao.upsertCrossRef(
                        CollectionItemCrossRef(
                            collectionId = BOOKMARKS_COLLECTION_ID,
                            contentId = contentId,
                            contentType = ContentType.CHART,
                            addedAt = now,
                        )
                    )
                }
                if (!hasCollectionMembership) {
                    collectionDao.upsertCrossRef(
                        CollectionItemCrossRef(
                            collectionId = collectionId,
                            contentId = contentId,
                            contentType = ContentType.CHART,
                            addedAt = now,
                        )
                    )
                }
                if (!wasSavedLocally) {
                    updateLocalState(id, OperationOption.BOOKMARK)
                    profileCacheRepository.adjustBookmarksCount(delta = 1)
                }
                if (!hasBookmarkMembership) {
                    queueManager.queueAndSyncBookmark(contentId, isBookmarked = true)
                }
                queueManager.queueAndSyncCollection(contentId, collectionId, isAdd = true)
            }.onFailure { e ->
                Log.e(TAG, "Failed to add to collection for contentId: $contentId, collectionId: $collectionId", e)
            }
        }

    override suspend fun removeFromCollection(
        id: String,
        contentId: String,
        collectionId: String
    ): Result<Unit> =
        withContext(dispatcher) {
            runCatching {
                val hadCollectionMembership = collectionDao.hasCrossRef(collectionId, contentId)
                if (hadCollectionMembership) {
                    collectionDao.deleteCrossRef(collectionId, contentId)
                }
                queueManager.queueAndSyncCollection(contentId, collectionId, isAdd = false)
            }.onFailure { e ->
                Log.e(TAG, "Failed to remove from collection for contentId: $contentId, collectionId: $collectionId", e)
            }
        }


    override suspend fun getQueueSize(): Int = queueManager.getQueueSize()

    override suspend fun processQueue() = queueManager.processQueuedInteractions()

    private suspend fun ensureBookmarksCollectionExists(updatedAt: LocalDateTime = LocalDateTime.now()) {
        collectionDao.upsertCollection(
            Collection(
                id = BOOKMARKS_COLLECTION_ID,
                userId = LOCAL_USER_ID,
                kind = CollectionKind.BOOKMARKS,
                name = "Bookmarks",
                isPublic = false,
                createdAt = updatedAt,
                updatedAt = updatedAt,
            )
        )
    }

    /**
     * Updates local chart state by chart `id` (the primary key).
     * Used by like/unlike/bookmark/unbookmark where we always have the chart id.
     */
    private suspend fun updateLocalState(id: String, operation: OperationOption) {
        try {
            val result = chartManager.updateContentById(id, operation)
            if (result !is ContentResult.Success) {
                Log.e(TAG, "Failed to update local chart for id=$id, operation=$operation, result=$result")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception updating local chart for id=$id, operation=$operation", e)
        }
    }
}