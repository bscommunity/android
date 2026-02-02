package com.meninocoiso.bscm.data.repository

import android.util.Log
import com.meninocoiso.bscm.data.manager.InteractionQueueManager
import com.meninocoiso.bscm.data.remote.ApiClient
import com.meninocoiso.bscm.domain.enums.ActionType
import com.meninocoiso.bscm.domain.repository.InteractionRepository
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
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : InteractionRepository {
    /**
     * Likes the content with the given contentId.
     * Attempts to like online if connected, otherwise queues the interaction.
     *
     * @param contentId The ID of the content to like.
     * @return A Flow emitting Result<Unit> indicating success or failure.
     */
    override suspend fun likeContent(contentId: String): Flow<Result<Unit>> = flow {
        Log.d(TAG, "Starting likeContent for contentId: $contentId")
        if (networkMonitor.isCurrentlyConnected()) {
            try {
                val success = apiClient.addLike(contentId)
                if (success) {
                    Log.d(TAG, "Successfully liked contentId: $contentId online")
                    emit(Result.success(Unit))
                } else {
                    Log.d(TAG, "Queued like for contentId: $contentId due to server response")
                    queueManager.queueLikeInteraction(contentId, true)
                    emit(Result.success(Unit))
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to like online, falling back to queue", e)
                queueManager.queueLikeInteraction(contentId, true)
                emit(Result.success(Unit))
            }
        } else {
            Log.d(TAG, "Queued like for contentId: $contentId due to offline")
            queueManager.queueLikeInteraction(contentId, true)
            emit(Result.success(Unit))
        }
    }.catch { e ->
        Log.e(TAG, "Unexpected error in likeContent for contentId: $contentId", e)
        emit(Result.failure(e))
    }.flowOn(dispatcher)

    /**
     * Unlikes the content with the given contentId.
     * Attempts to unlike online if connected, otherwise queues the interaction.
     *
     * @param contentId The ID of the content to unlike.
     * @return A Flow emitting Result<Unit> indicating success or failure.
     */
    override suspend fun unlikeContent(contentId: String): Flow<Result<Unit>> = flow {
        Log.d(TAG, "Starting unlikeContent for contentId: $contentId")
        if (networkMonitor.isCurrentlyConnected()) {
            try {
                val success = apiClient.removeLike(contentId)
                if (success) {
                    Log.d(TAG, "Successfully unliked contentId: $contentId online")
                    emit(Result.success(Unit))
                } else {
                    Log.d(TAG, "Queued unlike for contentId: $contentId due to server response")
                    queueManager.queueLikeInteraction(contentId, false)
                    emit(Result.success(Unit))
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to unlike online, falling back to queue", e)
                queueManager.queueLikeInteraction(contentId, false)
                emit(Result.success(Unit))
            }
        } else {
            Log.d(TAG, "Queued unlike for contentId: $contentId due to offline")
            queueManager.queueLikeInteraction(contentId, false)
            emit(Result.success(Unit))
        }
    }.catch { e ->
        Log.e(TAG, "Unexpected error in unlikeContent for contentId: $contentId", e)
        emit(Result.failure(e))
    }.flowOn(dispatcher)

    /**
     * Checks if the content is liked.
     * Returns the latest action from the queue.
     *
     * @param contentId The ID of the content to check.
     * @return A Flow emitting Result<Boolean> where true if liked, false otherwise.
     */
    override suspend fun isContentLiked(contentId: String): Flow<Result<Boolean>> = flow {
        Log.d(TAG, "Starting isContentLiked for contentId: $contentId")
        val latest = queueManager.getLatestAction(contentId, "likes")
        val isLiked = latest == ActionType.ADD
        Log.d(TAG, "Content $contentId is liked: $isLiked")
        emit(Result.success(isLiked))
    }.catch { e ->
        Log.e(TAG, "Failed to check if content is liked for contentId: $contentId", e)
        emit(Result.failure(e))
    }.flowOn(dispatcher)

    /**
     * Bookmarks the content with the given contentId.
     * Attempts to bookmark online if connected, otherwise queues the interaction.
     *
     * @param contentId The ID of the content to bookmark.
     * @return A Flow emitting Result<Unit> indicating success or failure.
     */
    override suspend fun bookmarkContent(contentId: String): Flow<Result<Unit>> = flow {
        Log.d(TAG, "Starting bookmarkContent for contentId: $contentId")
        if (networkMonitor.isCurrentlyConnected()) {
            try {
                val success = apiClient.addBookmark(contentId)
                if (success) {
                    Log.d(TAG, "Successfully bookmarked contentId: $contentId online")
                    emit(Result.success(Unit))
                } else {
                    Log.d(TAG, "Queued bookmark for contentId: $contentId due to server response")
                    queueManager.queueBookmarkInteraction(contentId, true)
                    emit(Result.success(Unit))
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to bookmark online, falling back to queue", e)
                queueManager.queueBookmarkInteraction(contentId, true)
                emit(Result.success(Unit))
            }
        } else {
            Log.d(TAG, "Queued bookmark for contentId: $contentId due to offline")
            queueManager.queueBookmarkInteraction(contentId, true)
            emit(Result.success(Unit))
        }
    }.catch { e ->
        Log.e(TAG, "Unexpected error in bookmarkContent for contentId: $contentId", e)
        emit(Result.failure(e))
    }.flowOn(dispatcher)

    /**
     * Unbookmarks the content with the given contentId.
     * Attempts to unbookmark online if connected, otherwise queues the interaction.
     *
     * @param contentId The ID of the content to unbookmark.
     * @return A Flow emitting Result<Unit> indicating success or failure.
     */
    override suspend fun unbookmarkContent(contentId: String): Flow<Result<Unit>> = flow {
        Log.d(TAG, "Starting unbookmarkContent for contentId: $contentId")
        if (networkMonitor.isCurrentlyConnected()) {
            try {
                val success = apiClient.removeBookmark(contentId)
                if (success) {
                    Log.d(TAG, "Successfully unbookmarked contentId: $contentId online")
                    emit(Result.success(Unit))
                } else {
                    Log.d(TAG, "Queued unbookmark for contentId: $contentId due to server response")
                    queueManager.queueBookmarkInteraction(contentId, false)
                    emit(Result.success(Unit))
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to unbookmark online, falling back to queue", e)
                queueManager.queueBookmarkInteraction(contentId, false)
                emit(Result.success(Unit))
            }
        } else {
            Log.d(TAG, "Queued unbookmark for contentId: $contentId due to offline")
            queueManager.queueBookmarkInteraction(contentId, false)
            emit(Result.success(Unit))
        }
    }.catch { e ->
        Log.e(TAG, "Unexpected error in unbookmarkContent for contentId: $contentId", e)
        emit(Result.failure(e))
    }.flowOn(dispatcher)

    /**
     * Checks if the content is bookmarked.
     * Returns the latest action from the queue.
     *
     * @param contentId The ID of the content to check.
     * @return A Flow emitting Result<Boolean> where true if bookmarked, false otherwise.
     */
    override suspend fun isContentBookmarked(contentId: String): Flow<Result<Boolean>> = flow {
        Log.d(TAG, "Starting isContentBookmarked for contentId: $contentId")
        val latest = queueManager.getLatestAction(contentId, "bookmarks")
        val isBookmarked = latest == ActionType.ADD
        Log.d(TAG, "Content $contentId is bookmarked: $isBookmarked")
        emit(Result.success(isBookmarked))
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
        Log.d(TAG, "Successfully queued add to collection for contentId: $contentId, collectionId: $collectionId")
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
        Log.d(TAG, "Successfully queued remove from collection for contentId: $contentId, collectionId: $collectionId")
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
