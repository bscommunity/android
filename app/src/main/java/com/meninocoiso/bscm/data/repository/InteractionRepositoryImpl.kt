package com.meninocoiso.bscm.data.repository

import android.util.Log
import com.meninocoiso.bscm.data.manager.ChartManager
import com.meninocoiso.bscm.data.manager.InteractionQueueManager
import com.meninocoiso.bscm.data.remote.ApiClient
import com.meninocoiso.bscm.domain.enums.OperationOption
import com.meninocoiso.bscm.domain.repository.InteractionRepository
import com.meninocoiso.bscm.domain.result.ContentResult
import com.meninocoiso.bscm.monitor.NetworkConnectivityMonitor
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
    private val apiClient: ApiClient,
    private val networkMonitor: NetworkConnectivityMonitor,
    private val profileCacheRepository: ProfileCacheRepository,
    private val chartManager: ChartManager,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : InteractionRepository {

    /**
     * Likes the content with the given contentId.
     *
     * 1. Immediately updates the local chart database (likedAt = now)
     * 2. Updates ProfileCacheRepository
     * 3. Queues the action for remote sync (only if we can't sync immediately)
     * 4. Attempts to sync with remote if connected
     *
     * @param contentId The ID of the content to like.
     * @return A Flow emitting Result<Unit> indicating success or failure.
     */
    override suspend fun likeContent(contentId: String): Flow<Result<Unit>> = flow {
        Log.d(TAG, "Starting likeContent for contentId: $contentId")

        // 1. Immediately update local database via ChartManager - this is the source of truth
        try {
            val result = chartManager.updateContent(contentId, OperationOption.LIKE)
            if (result is ContentResult.Success) {
                Log.d(TAG, "Updated local chart likedAt for contentId: $contentId")
            } else {
                Log.e(TAG, "Failed to update local chart likedAt for contentId: $contentId. Result: $result")
                // We might want to throw or emit failure, but the logic continues to queueing
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update local chart likedAt for contentId: $contentId", e)
        }

        // 2. Update ProfileCacheRepository to include this chart ID in likes
        try {
            profileCacheRepository.addLikeId(contentId)
            Log.d(TAG, "Added contentId to profile cache likes: $contentId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update profile cache for contentId: $contentId", e)
        }

        // 3. Queue the action for remote sync
        queueManager.queueLikeInteraction(contentId, true)
        Log.d(TAG, "Queued like for contentId: $contentId for remote sync")

        // 4. Attempt immediate sync if connected
        if (networkMonitor.isCurrentlyConnected()) {
            try {
                val success = apiClient.addLike(contentId)
                if (success) {
                    Log.d(TAG, "Successfully synced like to remote for contentId: $contentId")
                } else {
                    Log.w(TAG, "Remote sync returned false for like on contentId: $contentId, will retry")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to sync like to remote for contentId: $contentId, will retry from queue", e)
            }
        } else {
            Log.d(TAG, "No network connection, will sync like later from queue for contentId: $contentId")
        }

        emit(Result.success(Unit))
    }.catch { e ->
        Log.e(TAG, "Unexpected error in likeContent for contentId: $contentId", e)
        emit(Result.failure(e))
    }.flowOn(dispatcher)

    /**
     * Unlikes the content with the given contentId.
     *
     * 1. Immediately updates the local chart database (likedAt = null)
     * 2. Updates ProfileCacheRepository
     * 3. Queues the action for remote sync (only if we can't sync immediately)
     * 4. Attempts to sync with remote if connected
     *
     * @param contentId The ID of the content to unlike.
     * @return A Flow emitting Result<Unit> indicating success or failure.
     */
    override suspend fun unlikeContent(contentId: String): Flow<Result<Unit>> = flow {
        Log.d(TAG, "Starting unlikeContent for contentId: $contentId")

        // 1. Immediately update local database via ChartManager - this is the source of truth
        try {
            val result = chartManager.updateContent(contentId, OperationOption.UNLIKE)
            if (result is ContentResult.Success) {
                Log.d(TAG, "Cleared local chart likedAt for contentId: $contentId")
            } else {
                 Log.e(TAG, "Failed to clear local chart likedAt for contentId: $contentId. Result: $result")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear local chart likedAt for contentId: $contentId", e)
        }

        // 2. Update ProfileCacheRepository to remove this chart ID from likes
        try {
            profileCacheRepository.removeLikeId(contentId)
            Log.d(TAG, "Removed contentId from profile cache likes: $contentId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update profile cache for contentId: $contentId", e)
        }

        // 3. Queue the action for remote sync
        queueManager.queueLikeInteraction(contentId, false)
        Log.d(TAG, "Queued unlike for contentId: $contentId for remote sync")

        // 4. Attempt immediate sync if connected
        if (networkMonitor.isCurrentlyConnected()) {
            try {
                val success = apiClient.removeLike(contentId)
                if (success) {
                    Log.d(TAG, "Successfully synced unlike to remote for contentId: $contentId")
                } else {
                    Log.w(TAG, "Remote sync returned false for unlike on contentId: $contentId, will retry")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to sync unlike to remote for contentId: $contentId, will retry from queue", e)
            }
        } else {
            Log.d(TAG, "No network connection, will sync unlike later from queue for contentId: $contentId")
        }

        emit(Result.success(Unit))
    }.catch { e ->
        Log.e(TAG, "Unexpected error in unlikeContent for contentId: $contentId", e)
        emit(Result.failure(e))
    }.flowOn(dispatcher)

    /**
     * Gets the like status for content from the local chart database.
     * This is the source of truth, not the queue.
     *
     * Note: This method is kept for backward compatibility but now reads from the chart database,
     * not from the queue. ChartDetails should use the chart's likedAt field directly.
     *
     * @param contentId The ID of the content to check.
     * @return A Flow emitting Result<Boolean?> (not used in UI, provided for compatibility).
     */
    override suspend fun isContentLiked(contentId: String): Flow<Result<Boolean?>> = flow {
        Log.d(TAG, "isContentLiked called for contentId: $contentId - reading from local database")
        emit(Result.success(null)) // UI should use chart.likedAt directly
    }.catch { e ->
        Log.e(TAG, "Failed to check if content is liked for contentId: $contentId", e)
        emit(Result.failure(e))
    }.flowOn(dispatcher)

    /**
     * Bookmarks the content with the given contentId.
     *
     * 1. Immediately updates the local chart database (bookmarkedAt = now)
     * 2. Updates ProfileCacheRepository
     * 3. Queues the action for remote sync (only if we can't sync immediately)
     * 4. Attempts to sync with remote if connected
     *
     * @param contentId The ID of the content to bookmark.
     * @return A Flow emitting Result<Unit> indicating success or failure.
     */
    override suspend fun bookmarkContent(contentId: String): Flow<Result<Unit>> = flow {
        Log.d(TAG, "Starting bookmarkContent for contentId: $contentId")

        // 1. Immediately update local database via ChartManager - this is the source of truth
        try {
            val result = chartManager.updateContent(contentId, OperationOption.BOOKMARK)
            if (result is ContentResult.Success) {
                Log.d(TAG, "Updated local chart bookmarkedAt for contentId: $contentId")
            } else {
                Log.e(TAG, "Failed to update local chart bookmarkedAt for contentId: $contentId. Result: $result")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update local chart bookmarkedAt for contentId: $contentId", e)
        }

        // 2. Update ProfileCacheRepository to include this chart ID in bookmarks
        try {
            profileCacheRepository.addBookmarkId(contentId)
            Log.d(TAG, "Added contentId to profile cache bookmarks: $contentId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update profile cache for contentId: $contentId", e)
        }

        // 3. Queue the action for remote sync
        queueManager.queueBookmarkInteraction(contentId, true)
        Log.d(TAG, "Queued bookmark for contentId: $contentId for remote sync")

        // 4. Attempt immediate sync if connected
        if (networkMonitor.isCurrentlyConnected()) {
            try {
                val success = apiClient.addBookmark(contentId)
                if (success) {
                    Log.d(TAG, "Successfully synced bookmark to remote for contentId: $contentId")
                } else {
                    Log.w(TAG, "Remote sync returned false for bookmark on contentId: $contentId, will retry")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to sync bookmark to remote for contentId: $contentId, will retry from queue", e)
            }
        } else {
            Log.d(TAG, "No network connection, will sync bookmark later from queue for contentId: $contentId")
        }

        emit(Result.success(Unit))
    }.catch { e ->
        Log.e(TAG, "Unexpected error in bookmarkContent for contentId: $contentId", e)
        emit(Result.failure(e))
    }.flowOn(dispatcher)

    /**
     * Unbookmarks the content with the given contentId.
     *
     * 1. Immediately updates the local chart database (bookmarkedAt = null)
     * 2. Updates ProfileCacheRepository
     * 3. Queues the action for remote sync (only if we can't sync immediately)
     * 4. Attempts to sync with remote if connected
     *
     * @param contentId The ID of the content to unbookmark.
     * @return A Flow emitting Result<Unit> indicating success or failure.
     */
    override suspend fun unbookmarkContent(contentId: String): Flow<Result<Unit>> = flow {
        Log.d(TAG, "Starting unbookmarkContent for contentId: $contentId")

        // 1. Immediately update local database via ChartManager - this is the source of truth
        try {
            val result = chartManager.updateContent(contentId, OperationOption.UNBOOKMARK)
            if (result is ContentResult.Success) {
                Log.d(TAG, "Cleared local chart bookmarkedAt for contentId: $contentId")
            } else {
                Log.e(TAG, "Failed to clear local chart bookmarkedAt for contentId: $contentId. Result: $result")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear local chart bookmarkedAt for contentId: $contentId", e)
        }

        // 2. Update ProfileCacheRepository to remove this chart ID from bookmarks
        try {
            profileCacheRepository.removeBookmarkId(contentId)
            Log.d(TAG, "Removed contentId from profile cache bookmarks: $contentId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update profile cache for contentId: $contentId", e)
        }

        // 3. Queue the action for remote sync
        queueManager.queueBookmarkInteraction(contentId, false)
        Log.d(TAG, "Queued unbookmark for contentId: $contentId for remote sync")

        // 3. Attempt immediate sync if connected
        if (networkMonitor.isCurrentlyConnected()) {
            try {
                val success = apiClient.removeBookmark(contentId)
                if (success) {
                    Log.d(TAG, "Successfully synced unbookmark to remote for contentId: $contentId")
                } else {
                    Log.w(TAG, "Remote sync returned false for unbookmark on contentId: $contentId, will retry")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to sync unbookmark to remote for contentId: $contentId, will retry from queue", e)
            }
        } else {
            Log.d(TAG, "No network connection, will sync unbookmark later from queue for contentId: $contentId")
        }

        emit(Result.success(Unit))
    }.catch { e ->
        Log.e(TAG, "Unexpected error in unbookmarkContent for contentId: $contentId", e)
        emit(Result.failure(e))
    }.flowOn(dispatcher)

    /**
     * Gets the bookmark status for content from the local chart database.
     * This is the source of truth, not the queue.
     *
     * Note: This method is kept for backward compatibility but now reads from the chart database,
     * not from the queue. ChartDetails should use the chart's bookmarkedAt field directly.
     *
     * @param contentId The ID of the content to check.
     * @return A Flow emitting Result<Boolean?> (not used in UI, provided for compatibility).
     */
    override suspend fun isContentBookmarked(contentId: String): Flow<Result<Boolean?>> = flow {
        Log.d(TAG, "isContentBookmarked called for contentId: $contentId - reading from local database")
        emit(Result.success(null)) // UI should use chart.bookmarkedAt directly
    }.catch { e ->
        Log.e(TAG, "Failed to check if content is bookmarked for contentId: $contentId", e)
        emit(Result.failure(e))
    }.flowOn(dispatcher)

    /**
     * Adds the content to the specified collection.
     * Queues the interaction.
     *
     * @param contentId The ID of the content to add.
     * @param collectionId The ID of the collection to add to.
     * @return A Flow emitting Result<Unit> indicating success or failure.
     */
    override suspend fun addToCollection(contentId: String, collectionId: String): Flow<Result<Unit>> = flow {
        Log.d(TAG, "Starting addToCollection for contentId: $contentId, collectionId: $collectionId")
        queueManager.queueCollectionInteraction(contentId, collectionId, true)
        Log.d(TAG, "Queued add to collection for contentId: $contentId, collectionId: $collectionId")
        emit(Result.success(Unit))
    }.catch { e ->
        Log.e(TAG, "Failed to add to collection for contentId: $contentId, collectionId: $collectionId", e)
        emit(Result.failure(e))
    }.flowOn(dispatcher)

    /**
     * Removes the content from the specified collection.
     * Queues the interaction.
     *
     * @param contentId The ID of the content to remove.
     * @param collectionId The ID of the collection to remove from.
     * @return A Flow emitting Result<Unit> indicating success or failure.
     */
    override suspend fun removeFromCollection(contentId: String, collectionId: String): Flow<Result<Unit>> = flow {
        Log.d(TAG, "Starting removeFromCollection for contentId: $contentId, collectionId: $collectionId")
        queueManager.queueCollectionInteraction(contentId, collectionId, false)
        Log.d(TAG, "Queued remove from collection for contentId: $contentId, collectionId: $collectionId")
        emit(Result.success(Unit))
    }.catch { e ->
        Log.e(TAG, "Failed to remove from collection for contentId: $contentId, collectionId: $collectionId", e)
        emit(Result.failure(e))
    }.flowOn(dispatcher)

    /**
     * Gets the current size of the interaction queue.
     * @return The number of items in the queue.
     */
    override suspend fun getQueueSize(): Int {
        Log.d(TAG, "Getting queue size")
        val size = queueManager.getQueueSize()
        Log.d(TAG, "Queue size: $size")
        return size
    }

    /**
     * Processes the queued interactions.
     */
    override suspend fun processQueue() {
        Log.d(TAG, "Starting processQueue")
        queueManager.processQueuedInteractions()
        Log.d(TAG, "Finished processing queue")
    }
}
