package com.meninocoiso.bscm.data.repository

import com.meninocoiso.bscm.data.remote.ApiClient
import com.meninocoiso.bscm.data.remote.dto.collection.UpdateCollectionItemRequest
import com.meninocoiso.bscm.domain.enums.ActionType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "InteractionRepositoryImpl"
private const val BATCH_DELAY_MS = 3000L // 3 seconds for deduplication

@Singleton
class InteractionRepositoryImpl @Inject constructor(
    private val apiClient: ApiClient,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : InteractionRepository {
    private val pendingInteractions = mutableMapOf<Pair<String, String>, UpdateCollectionItemRequest>()
    private val mutex = Mutex()
    private var batchJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    private fun queueInteraction(contentId: String, collectionId: String, action: ActionType) {
        scope.launch {
            mutex.withLock {
                pendingInteractions[Pair(contentId, collectionId)] = UpdateCollectionItemRequest(contentId, collectionId, action)
                if (batchJob == null || batchJob?.isCompleted == true) {
                    batchJob = scope.launch {
                        delay(BATCH_DELAY_MS)
                        sendBatch()
                    }
                }
            }
        }
    }

    private suspend fun sendBatch() {
        val batch: List<UpdateCollectionItemRequest>
        mutex.withLock {
            batch = pendingInteractions.values.toList()
            pendingInteractions.clear()
        }
        if (batch.isNotEmpty()) {
            try {
                apiClient.batchProcessInteractions(batch)
            } catch (e: Exception) {
                // Optionally handle retry logic here
            }
        }
    }

    override suspend fun likeContent(contentId: String): Flow<Result<Unit>> = flow {
        queueInteraction(contentId, "likes", ActionType.ADD)
        emit(Result.success(Unit))
    }.catch { e ->
        emit(Result.failure(e))
    }.flowOn(dispatcher)

    override suspend fun unlikeContent(contentId: String): Flow<Result<Unit>> = flow {
        queueInteraction(contentId, "likes", ActionType.REMOVE)
        emit(Result.success(Unit))
    }.catch { e ->
        emit(Result.failure(e))
    }.flowOn(dispatcher)

    override suspend fun isContentLiked(contentId: String): Flow<Result<Boolean>> = flow {
        val latest = mutex.withLock { pendingInteractions[Pair(contentId, "likes")]?.action }
        emit(Result.success(latest == ActionType.ADD))
    }.catch { e ->
        emit(Result.failure(e))
    }.flowOn(dispatcher)

    override suspend fun favoriteContent(contentId: String): Flow<Result<Unit>> = flow {
        queueInteraction(contentId, "favorites", ActionType.ADD)
        emit(Result.success(Unit))
    }.catch { e ->
        emit(Result.failure(e))
    }.flowOn(dispatcher)

    override suspend fun unfavoriteContent(contentId: String): Flow<Result<Unit>> = flow {
        queueInteraction(contentId, "favorites", ActionType.REMOVE)
        emit(Result.success(Unit))
    }.catch { e ->
        emit(Result.failure(e))
    }.flowOn(dispatcher)

    override suspend fun isContentFavorited(contentId: String): Flow<Result<Boolean>> = flow {
        val latest = mutex.withLock { pendingInteractions[Pair(contentId, "favorites")]?.action }
        emit(Result.success(latest == ActionType.ADD))
    }.catch { e ->
        emit(Result.failure(e))
    }.flowOn(dispatcher)

    override suspend fun addToCollection(contentId: String, collectionId: String): Flow<Result<Unit>> = flow {
        queueInteraction(contentId, collectionId, ActionType.ADD)
        emit(Result.success(Unit))
    }.catch { e ->
        emit(Result.failure(e))
    }.flowOn(dispatcher)

    override suspend fun removeFromCollection(contentId: String, collectionId: String): Flow<Result<Unit>> = flow {
        queueInteraction(contentId, collectionId, ActionType.REMOVE)
        emit(Result.success(Unit))
    }.catch { e ->
        emit(Result.failure(e))
    }.flowOn(dispatcher)

    override suspend fun getQueueSize(): Int {
        return mutex.withLock {
            pendingInteractions.size
        }
    }

    override suspend fun processQueue() {
        // Cancel any pending batch job and send immediately
        batchJob?.cancel()
        sendBatch()
    }
}
