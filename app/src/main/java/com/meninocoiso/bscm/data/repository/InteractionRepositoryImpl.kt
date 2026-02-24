package com.meninocoiso.bscm.data.repository

import android.util.Log
import com.meninocoiso.bscm.data.local.dao.ChartDao
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "InteractionRepositoryImpl"

@Singleton
class InteractionRepositoryImpl @Inject constructor(
    private val queueManager: InteractionQueueManager,
    private val chartManager: ChartManager,
    private val chartDao: ChartDao,
    private val collectionDao: CollectionDao,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : InteractionRepository {

    override suspend fun likeContent(id: String, contentId: String): Flow<Result<Unit>> = flow {
        updateLocalState(id = id, operation = OperationOption.LIKE)
        queueManager.queueAndSyncLike(contentId, isLike = true)
        emit(Result.success(Unit))
    }.catch { e ->
        Log.e(TAG, "Unexpected error in likeContent for contentId: $contentId", e)
        emit(Result.failure(e))
    }.flowOn(dispatcher)

    override suspend fun unlikeContent(id: String, contentId: String): Flow<Result<Unit>> = flow {
        updateLocalState(id = id, operation = OperationOption.UNLIKE)
        queueManager.queueAndSyncLike(contentId, isLike = false)
        emit(Result.success(Unit))
    }.catch { e ->
        Log.e(TAG, "Unexpected error in unlikeContent for contentId: $contentId", e)
        emit(Result.failure(e))
    }.flowOn(dispatcher)

    override suspend fun bookmarkContent(id: String, contentId: String): Flow<Result<Unit>> = flow {
        updateLocalState(id = id, operation = OperationOption.BOOKMARK)
        queueManager.queueAndSyncBookmark(contentId, isBookmarked = true)
        emit(Result.success(Unit))
    }.catch { e ->
        Log.e(TAG, "Unexpected error in bookmarkContent for contentId: $contentId", e)
        emit(Result.failure(e))
    }.flowOn(dispatcher)

    override suspend fun unbookmarkContent(id: String, contentId: String): Flow<Result<Unit>> = flow {
        updateLocalState(id = id, operation = OperationOption.UNBOOKMARK)
        queueManager.queueAndSyncBookmark(contentId, isBookmarked = false)
        emit(Result.success(Unit))
    }.catch { e ->
        Log.e(TAG, "Unexpected error in unbookmarkContent for contentId: $contentId", e)
        emit(Result.failure(e))
    }.flowOn(dispatcher)

    override suspend fun addToCollection(
        contentId: String,
        collectionId: String
    ): Flow<Result<Unit>> = flow {
        collectionDao.upsertCrossRef(
            CollectionItemCrossRef(
                collectionId = collectionId,
                contentId = contentId,
                contentType = ContentType.CHART
            )
        )
        collectionDao.incrementCollectionChartCount(collectionId, java.time.LocalDateTime.now())
        queueManager.queueAndSyncCollection(contentId, collectionId, isAdd = true)
        emit(Result.success(Unit))
    }.catch { e ->
        Log.e(TAG, "Failed to add to collection for contentId: $contentId, collectionId: $collectionId", e)
        emit(Result.failure(e))
    }.flowOn(dispatcher)

    override suspend fun removeFromCollection(
        contentId: String,
        collectionId: String
    ): Flow<Result<Unit>> = flow {
        collectionDao.deleteCrossRef(collectionId, contentId)
        collectionDao.decrementCollectionChartCount(collectionId, java.time.LocalDateTime.now())
        queueManager.queueAndSyncCollection(contentId, collectionId, isAdd = false)
        emit(Result.success(Unit))
    }.catch { e ->
        Log.e(TAG, "Failed to remove from collection for contentId: $contentId, collectionId: $collectionId", e)
        emit(Result.failure(e))
    }.flowOn(dispatcher)

    /**
     * Moves content from BOOKMARKS into a custom USER collection (or vice-versa).
     *
     * The previous implementation only managed the queue but never updated the
     * local Room state, so:
     *   - bookmarked_at was never cleared → item kept appearing in the bookmarks list
     *   - The cross-ref for the new collection was never written → item didn't appear
     *     in the collection
     *
     * Fix: we now explicitly clear/set local state based on the target collection kind
     * before delegating queue work to InteractionQueueManager.
     *
     * BOOKMARKS → USER collection:
     *   1. Clear bookmarked_at in charts table (removes from bookmarks observer)
     *   2. Write cross-ref into collection_item_cross_ref (adds to collection)
     *   3. Increment collection chart count
     *
     * USER collection → BOOKMARKS (reverse direction, if ever needed):
     *   1. Remove cross-ref from the source collection
     *   2. Set bookmarked_at = now (adds to bookmarks observer)
     */
    override suspend fun changeContentCollection(
        contentId: String,
        targetCollectionId: String,
        targetCollectionKind: CollectionKind
    ): Flow<Result<Unit>> = flow {
        when (targetCollectionKind) {
            CollectionKind.USER -> {
                // Moving OUT of bookmarks INTO a custom collection.
                // We need the chart's local `id` (not contentId) to clear bookmarked_at.
                // ChartManager.updateContentByContentId handles the lookup.
                updateLocalStateByContentId(contentId = contentId, operation = OperationOption.UNBOOKMARK)

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
                    java.time.LocalDateTime.now()
                )
            }

            CollectionKind.BOOKMARKS -> {
                // Moving OUT of a custom collection INTO bookmarks.
                // Remove the cross-ref from the source collection first.
                // (Caller should pass the source collectionId separately if needed;
                //  for now we rely on queueManager to handle the removal side.)
                updateLocalStateByContentId(contentId = contentId, operation = OperationOption.BOOKMARK)
            }

            else -> {
                // LIKES or other system kinds — no local state change needed here
                Log.w(TAG, "changeContentCollection called with unexpected kind: $targetCollectionKind")
            }
        }

        // Always defer remote sync to the queue manager — handles conflict resolution
        // and ensures offline-first behaviour is preserved.
        queueManager.clearConflictingInteractions(contentId, targetCollectionKind)
        queueManager.queueAndSyncCollection(contentId, targetCollectionId, isAdd = true)
        emit(Result.success(Unit))
    }.catch { e ->
        Log.e(TAG, "Failed to change content collection for contentId: $contentId", e)
        emit(Result.failure(e))
    }.flowOn(dispatcher)

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

    /**
     * Updates local chart state by `contentId` (the remote content identifier).
     * Used by [changeContentCollection] where we only have the contentId, not the
     * chart's local primary key.
     *
     * We look up the chart's local `id` via [CollectionDao] / [ChartDao] first,
     * then delegate to the existing [updateLocalState] — no new ChartManager method needed.
     */
    private suspend fun updateLocalStateByContentId(contentId: String, operation: OperationOption) {
        try {
            val chart = chartDao.getChartByContentId(contentId)
            if (chart == null) {
                Log.w(TAG, "No local chart found for contentId=$contentId, skipping local state update")
                return
            }
            updateLocalState(id = chart.id, operation = operation)
        } catch (e: Exception) {
            Log.e(TAG, "Exception updating local chart for contentId=$contentId, operation=$operation", e)
        }
    }
}