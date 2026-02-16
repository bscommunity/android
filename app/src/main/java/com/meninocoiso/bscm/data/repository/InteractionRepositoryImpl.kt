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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "InteractionRepositoryImpl"

// The repository's only job is to:
//   1. Update local state (ChartManager + ProfileCacheRepository)
//   2. Hand off remote sync to InteractionQueueManager
//
// It knows nothing about queue internals, network state, or API calls.

@Singleton
class InteractionRepositoryImpl @Inject constructor(
    private val queueManager: InteractionQueueManager,
    private val chartManager: ChartManager,
    private val collectionDao: CollectionDao,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : InteractionRepository {

    /**
     * Likes the content with the given id.
     *
     * 1. Updates local chart database (likedAt = now)
     * 2. Updates ProfileCacheRepository
     * 3. Delegates queue + remote sync to InteractionQueueManager
     */
    override suspend fun likeContent(id: String, contentId: String): Flow<Result<Unit>> = flow {
        updateLocalState(id = id, operation = OperationOption.LIKE)
        queueManager.queueAndSyncLike(contentId, isLike = true)
        emit(Result.success(Unit))
    }.catch { e ->
        Log.e(TAG, "Unexpected error in likeContent for contentId: $contentId", e)
        emit(Result.failure(e))
    }.flowOn(dispatcher)

    /**
     * Unlikes the content with the given id.
     *
     * 1. Updates local chart database (likedAt = null)
     * 2. Updates ProfileCacheRepository
     * 3. Delegates queue + remote sync to InteractionQueueManager
     */
    override suspend fun unlikeContent(id: String, contentId: String): Flow<Result<Unit>> = flow {
        updateLocalState(id = id, operation = OperationOption.UNLIKE)
        queueManager.queueAndSyncLike(contentId, isLike = false)
        emit(Result.success(Unit))
    }.catch { e ->
        Log.e(TAG, "Unexpected error in unlikeContent for contentId: $contentId", e)
        emit(Result.failure(e))
    }.flowOn(dispatcher)

    /**
     * Bookmarks the content with the given id.
     *
     * 1. Updates local chart database (bookmarkedAt = now)
     * 2. Updates ProfileCacheRepository
     * 3. Delegates queue + remote sync to InteractionQueueManager
     */
    override suspend fun bookmarkContent(id: String, contentId: String): Flow<Result<Unit>> = flow {
        updateLocalState(id = id, operation = OperationOption.BOOKMARK)
        queueManager.queueAndSyncBookmark(contentId, isBookmarked = true)
        emit(Result.success(Unit))
    }.catch { e ->
        Log.e(TAG, "Unexpected error in bookmarkContent for contentId: $contentId", e)
        emit(Result.failure(e))
    }.flowOn(dispatcher)

    /**
     * Unbookmarks the content with the given id.
     *
     * 1. Updates local chart database (bookmarkedAt = null)
     * 2. Updates ProfileCacheRepository
     * 3. Delegates queue + remote sync to InteractionQueueManager
     */
    override suspend fun unbookmarkContent(id: String, contentId: String): Flow<Result<Unit>> = flow {
        updateLocalState(id = id, operation = OperationOption.UNBOOKMARK)
        queueManager.queueAndSyncBookmark(contentId, isBookmarked = false)
        emit(Result.success(Unit))
    }.catch { e ->
        Log.e(TAG, "Unexpected error in unbookmarkContent for contentId: $contentId", e)
        emit(Result.failure(e))
    }.flowOn(dispatcher)

    /**
     * Adds the content to the specified collection.
     */
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
        collectionDao.incrementCollectionItemCount(collectionId, java.time.LocalDateTime.now())
        queueManager.queueAndSyncCollection(contentId, collectionId, isAdd = true)
        emit(Result.success(Unit))
    }.catch { e ->
        Log.e(TAG, "Failed to add to collection for contentId: $contentId, collectionId: $collectionId", e)
        emit(Result.failure(e))
    }.flowOn(dispatcher)

    /**
     * Removes the content from the specified collection.
     */
    override suspend fun removeFromCollection(
        contentId: String,
        collectionId: String
    ): Flow<Result<Unit>> = flow {
        collectionDao.deleteCrossRef(collectionId, contentId)
        collectionDao.decrementCollectionItemCount(collectionId, java.time.LocalDateTime.now())
        queueManager.queueAndSyncCollection(contentId, collectionId, isAdd = false)
        emit(Result.success(Unit))
    }.catch { e ->
        Log.e(TAG, "Failed to remove from collection for contentId: $contentId, collectionId: $collectionId", e)
        emit(Result.failure(e))
    }.flowOn(dispatcher)

    /**
     * Moves content to a different collection, respecting the mutual exclusion rule:
     * content CANNOT be in BOOKMARKS and a USER collection at the same time.
     *
     * Delegates conflict resolution and sync entirely to InteractionQueueManager.
     */
    override suspend fun changeContentCollection(
        contentId: String,
        targetCollectionId: String,
        targetCollectionKind: CollectionKind
    ): Flow<Result<Unit>> = flow {
        // Clear any conflicting queued interaction first (BOOKMARKS <-> USER are mutually exclusive)
        queueManager.clearConflictingInteractions(contentId, targetCollectionKind)
        queueManager.queueAndSyncCollection(contentId, targetCollectionId, isAdd = true)
        emit(Result.success(Unit))
    }.catch { e ->
        Log.e(TAG, "Failed to change content collection for contentId: $contentId", e)
        emit(Result.failure(e))
    }.flowOn(dispatcher)

    override suspend fun getQueueSize(): Int {
        return queueManager.getQueueSize()
    }

    override suspend fun processQueue() {
        queueManager.processQueuedInteractions()
    }

    /**
     * Updates the local chart database and the profile cache.
     * Errors are caught and logged individually so a cache failure
     * doesn't prevent the chart update (and vice versa).
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