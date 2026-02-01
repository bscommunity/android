package com.meninocoiso.bscm.data.repository

import com.meninocoiso.bscm.data.manager.InteractionQueueManager
import com.meninocoiso.bscm.domain.enums.ActionType
import com.meninocoiso.bscm.domain.repository.InteractionRepository
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
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : InteractionRepository {
    override suspend fun likeContent(contentId: String): Flow<Result<Unit>> = flow {
        queueManager.queueLikeInteraction(contentId, true)
        emit(Result.success(Unit))
    }.catch { e ->
        emit(Result.failure(e))
    }.flowOn(dispatcher)

    override suspend fun unlikeContent(contentId: String): Flow<Result<Unit>> = flow {
        queueManager.queueLikeInteraction(contentId, false)
        emit(Result.success(Unit))
    }.catch { e ->
        emit(Result.failure(e))
    }.flowOn(dispatcher)

    override suspend fun isContentLiked(contentId: String): Flow<Result<Boolean>> = flow {
        val latest = queueManager.getLatestAction(contentId, "likes")
        emit(Result.success(latest == ActionType.ADD))
    }.catch { e ->
        emit(Result.failure(e))
    }.flowOn(dispatcher)

    override suspend fun bookmarkContent(contentId: String): Flow<Result<Unit>> = flow {
        queueManager.queueBookmarkInteraction(contentId, true)
        emit(Result.success(Unit))
    }.catch { e ->
        emit(Result.failure(e))
    }.flowOn(dispatcher)

    override suspend fun unbookmarkContent(contentId: String): Flow<Result<Unit>> = flow {
        queueManager.queueBookmarkInteraction(contentId, false)
        emit(Result.success(Unit))
    }.catch { e ->
        emit(Result.failure(e))
    }.flowOn(dispatcher)

    override suspend fun isContentBookmarked(contentId: String): Flow<Result<Boolean>> = flow {
        val latest = queueManager.getLatestAction(contentId, "bookmarks")
        emit(Result.success(latest == ActionType.ADD))
    }.catch { e ->
        emit(Result.failure(e))
    }.flowOn(dispatcher)

    override suspend fun addToCollection(contentId: String, collectionId: String): Flow<Result<Unit>> = flow {
        queueManager.queueCollectionInteraction(contentId, collectionId, true)
        emit(Result.success(Unit))
    }.catch { e ->
        emit(Result.failure(e))
    }.flowOn(dispatcher)

    override suspend fun removeFromCollection(contentId: String, collectionId: String): Flow<Result<Unit>> = flow {
        queueManager.queueCollectionInteraction(contentId, collectionId, false)
        emit(Result.success(Unit))
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
