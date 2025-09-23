package com.meninocoiso.bscm.data.repository

import android.util.Log
import com.meninocoiso.bscm.data.manager.InteractionQueueManager
import com.meninocoiso.bscm.data.remote.ApiClient
import com.meninocoiso.bscm.domain.enums.ContentType
import com.meninocoiso.bscm.domain.model.InteractionResult
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
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : InteractionRepository {
    
    // Local state to track likes/bookmarks optimistically
    private val localLikeState = mutableMapOf<String, Boolean>()
    private val localBookmarkState = mutableMapOf<String, Boolean>()
    
    override suspend fun likeContent(
        contentType: ContentType,
        contentId: ULong
    ): Flow<Result<InteractionResult>> = flow {
        try {
            // Update local state optimistically
            val key = "${contentType.name}:$contentId"
            localLikeState[key] = true
            
            // Queue the interaction
            val interactionId = queueManager.queueLikeInteraction(contentType, contentId, true)
            
            Log.d(TAG, "Queued like interaction $interactionId for $contentType:$contentId")
            
            emit(Result.success(InteractionResult(
                success = true,
                shouldRetry = false
            )))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to queue like interaction", e)
            emit(Result.failure(e))
        }
    }.catch { e ->
        emit(Result.failure(e))
    }.flowOn(dispatcher)
    
    override suspend fun unlikeContent(
        contentType: ContentType,
        contentId: ULong
    ): Flow<Result<InteractionResult>> = flow {
        try {
            // Update local state optimistically
            val key = "${contentType.name}:$contentId"
            localLikeState[key] = false
            
            // Queue the interaction
            val interactionId = queueManager.queueLikeInteraction(contentType, contentId, false)
            
            Log.d(TAG, "Queued unlike interaction $interactionId for $contentType:$contentId")
            
            emit(Result.success(InteractionResult(
                success = true,
                shouldRetry = false
            )))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to queue unlike interaction", e)
            emit(Result.failure(e))
        }
    }.catch { e ->
        emit(Result.failure(e))
    }.flowOn(dispatcher)
    
    override suspend fun isContentLiked(
        contentType: ContentType,
        contentId: ULong
    ): Flow<Result<Boolean>> = flow {
        try {
            val key = "${contentType.name}:$contentId"
            
            // Check local state first for immediate response
            if (localLikeState.containsKey(key)) {
                emit(Result.success(localLikeState[key] ?: false))
                return@flow
            }
            
            // Fall back to server check if not in local state
            try {
                val isLiked = apiClient.isContentLiked(contentType, contentId)
                localLikeState[key] = isLiked // Cache the result
                emit(Result.success(isLiked))
            } catch (e: Exception) {
                Log.w(TAG, "Failed to check like status from server, assuming false", e)
                emit(Result.success(false))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check like status", e)
            emit(Result.failure(e))
        }
    }.catch { e ->
        emit(Result.failure(e))
    }.flowOn(dispatcher)
    
    override suspend fun bookmarkContent(
        contentType: ContentType,
        contentId: ULong,
        collectionId: ULong,
        userId: String
    ): Flow<Result<InteractionResult>> = flow {
        try {
            // Update local state optimistically
            val key = "${contentType.name}:$contentId:$collectionId"
            localBookmarkState[key] = true
            
            // Queue the interaction
            val interactionId = queueManager.queueBookmarkInteraction(
                contentType, contentId, collectionId, userId, true
            )
            
            Log.d(TAG, "Queued bookmark interaction $interactionId for $contentType:$contentId")
            
            emit(Result.success(InteractionResult(
                success = true,
                shouldRetry = false
            )))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to queue bookmark interaction", e)
            emit(Result.failure(e))
        }
    }.catch { e ->
        emit(Result.failure(e))
    }.flowOn(dispatcher)
    
    override suspend fun unbookmarkContent(
        contentType: ContentType,
        contentId: ULong,
        collectionId: ULong,
        userId: String
    ): Flow<Result<InteractionResult>> = flow {
        try {
            // Update local state optimistically
            val key = "${contentType.name}:$contentId:$collectionId"
            localBookmarkState[key] = false
            
            // Queue the interaction
            val interactionId = queueManager.queueBookmarkInteraction(
                contentType, contentId, collectionId, userId, false
            )
            
            Log.d(TAG, "Queued unbookmark interaction $interactionId for $contentType:$contentId")
            
            emit(Result.success(InteractionResult(
                success = true,
                shouldRetry = false
            )))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to queue unbookmark interaction", e)
            emit(Result.failure(e))
        }
    }.catch { e ->
        emit(Result.failure(e))
    }.flowOn(dispatcher)
    
    override suspend fun getQueueSize(): Int {
        return queueManager.getQueueSize()
    }
    
    override suspend fun processQueue() {
        queueManager.processQueuedInteractions()
    }
}
