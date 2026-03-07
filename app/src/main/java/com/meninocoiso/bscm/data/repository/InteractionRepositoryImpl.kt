package com.meninocoiso.bscm.data.repository

import android.util.Log
import com.meninocoiso.bscm.data.local.dao.CollectionDao
import com.meninocoiso.bscm.data.manager.ChartManager
import com.meninocoiso.bscm.data.manager.InteractionQueueManager
import com.meninocoiso.bscm.domain.enums.CollectionKind
import com.meninocoiso.bscm.domain.enums.ContentType
import com.meninocoiso.bscm.domain.enums.OperationOption
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

@Singleton
class InteractionRepositoryImpl @Inject constructor(
    private val queueManager: InteractionQueueManager,
    private val chartManager: ChartManager,
    private val collectionDao: CollectionDao,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : InteractionRepository {

    override suspend fun likeContent(id: String, contentId: String): Result<Unit> =
        withContext(dispatcher) {
            runCatching {
                updateLocalState(id = id, operation = OperationOption.LIKE)
                queueManager.queueAndSyncLike(contentId, isLike = true)
            }.onFailure { e ->
                Log.e(TAG, "Unexpected error in likeContent for contentId: $contentId", e)
            }
        }

    override suspend fun unlikeContent(id: String, contentId: String): Result<Unit> =
        withContext(dispatcher) {
            runCatching {
                updateLocalState(id = id, operation = OperationOption.UNLIKE)
                queueManager.queueAndSyncLike(contentId, isLike = false)
            }.onFailure { e ->
                Log.e(TAG, "Unexpected error in unlikeContent for contentId: $contentId", e)
            }
        }

    override suspend fun bookmarkContent(id: String, contentId: String): Result<Unit> =
        withContext(dispatcher) {
            runCatching {
                // Insert cross-ref for BOOKMARKS collection
                collectionDao.upsertCrossRef(
                    CollectionItemCrossRef(
                        collectionId = "bookmarks",
                        contentId = contentId,
                        contentType = ContentType.CHART
                    )
                )
                // Update local chart state so UI reflects bookmark immediately
                updateLocalState(id = id, operation = OperationOption.BOOKMARK)
                queueManager.queueAndSyncBookmark(contentId, isBookmarked = true)
            }.onFailure { e ->
                Log.e(TAG, "Unexpected error in bookmarkContent for contentId: $contentId", e)
            }
        }

    override suspend fun unbookmarkContent(id: String, contentId: String): Result<Unit> =
        withContext(dispatcher) {
            runCatching {
                // Remove cross-ref from BOOKMARKS collection
                collectionDao.deleteCrossRef("bookmarks", contentId)
                // Update local chart state so UI reflects unbookmark immediately
                updateLocalState(id = id, operation = OperationOption.UNBOOKMARK)
                queueManager.queueAndSyncBookmark(contentId, isBookmarked = false)
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
                collectionDao.upsertCrossRef(
                    CollectionItemCrossRef(
                        collectionId = collectionId,
                        contentId = contentId,
                        contentType = ContentType.CHART
                    )
                )
                collectionDao.incrementCollectionChartCount(collectionId, LocalDateTime.now())
                updateLocalState(id, OperationOption.BOOKMARK)
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
                collectionDao.deleteCrossRef(collectionId, contentId)
                collectionDao.decrementCollectionChartCount(collectionId, LocalDateTime.now())
                updateLocalState(id, OperationOption.UNBOOKMARK)
                queueManager.queueAndSyncCollection(contentId, collectionId, isAdd = false)
            }.onFailure { e ->
                Log.e(TAG, "Failed to remove from collection for contentId: $contentId, collectionId: $collectionId", e)
            }
        }

    /**
     * Moves content from BOOKMARKS into a custom USER collection (or vice-versa).
     *
     * BOOKMARKS → USER collection:
     *   1. Remove cross-ref from BOOKMARKS
     *   2. Write cross-ref into collection_item_cross_ref (adds to collection)
     *   3. Increment collection chart count
     *
     * USER collection → BOOKMARKS:
     *   1. Remove cross-ref from the source USER collection
     *   2. Decrement source collection chart count
     *   3. Write cross-ref into BOOKMARKS
     */
    override suspend fun changeContentCollection(
        contentId: String,
        targetCollectionId: String,
        targetCollectionKind: CollectionKind
    ): Result<Unit> =
        withContext(dispatcher) {
            runCatching {
                when (targetCollectionKind) {
                    CollectionKind.USER -> {
                        // Moving OUT of bookmarks INTO a custom collection.
                        // Remove cross-ref from BOOKMARKS
                        collectionDao.deleteCrossRef("bookmarks", contentId)

                        // Write the cross-ref so the item appears in the collection immediately
                        collectionDao.upsertCrossRef(
                            CollectionItemCrossRef(
                                collectionId = targetCollectionId,
                                contentId = contentId,
                                contentType = ContentType.CHART
                            )
                        )
                        collectionDao.incrementCollectionChartCount(
                            targetCollectionId,
                            LocalDateTime.now()
                        )
                    }

                    else -> {
                        // BOOKMARKS reverse-move not yet implemented — throw so it's not silently swallowed
                        error("changeContentCollection: unsupported targetCollectionKind=$targetCollectionKind")
                    }
                }

                // Always defer remote sync to the queue manager — handles conflict resolution
                // and ensures offline-first behaviour is preserved.
                queueManager.clearConflictingInteractions(contentId, targetCollectionKind)
                queueManager.queueAndSyncCollection(contentId, targetCollectionId, isAdd = true)
            }.onFailure { e ->
                Log.e(TAG, "Failed to change content collection for contentId: $contentId", e)
            }
        }

    override suspend fun getQueueSize(): Int = queueManager.getQueueSize()

    override suspend fun processQueue() = queueManager.processQueuedInteractions()

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