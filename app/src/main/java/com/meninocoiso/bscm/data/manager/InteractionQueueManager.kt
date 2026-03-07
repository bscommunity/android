package com.meninocoiso.bscm.data.manager

import android.util.Log
import com.meninocoiso.bscm.data.local.dao.InteractionQueueDao
import com.meninocoiso.bscm.data.local.entity.QueuedInteractionEntity
import com.meninocoiso.bscm.data.remote.ApiClient
import com.meninocoiso.bscm.data.remote.ApiException
import com.meninocoiso.bscm.data.remote.dto.collection.BatchCollectionItemRequest
import com.meninocoiso.bscm.di.ApplicationScope
import com.meninocoiso.bscm.domain.enums.ActionType
import com.meninocoiso.bscm.domain.enums.CollectionKind
import com.meninocoiso.bscm.monitor.NetworkConnectivityMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "InteractionQueueManager"

data class CollectionMembershipOverlay(
    val forceIncludeContentIds: Set<String> = emptySet(),
    val forceExcludeContentIds: Set<String> = emptySet(),
)

@Singleton
class InteractionQueueManager @Inject constructor(
    private val queueDao: InteractionQueueDao,
    private val apiClient: ApiClient,
    private val networkMonitor: NetworkConnectivityMonitor,
    @param:ApplicationScope private val applicationScope: CoroutineScope   // ← add this
) {
    private val processingMutex = Mutex()

    /**
     * Queues a like/unlike interaction and attempts an immediate sync if connected.
     * The queue is the safety net — if the app dies before sync completes, the
     * interaction is already persisted and will be retried on next session.
     */
    suspend fun queueAndSyncLike(contentId: String, isLike: Boolean) {
        val action = if (isLike) ActionType.ADD else ActionType.REMOVE
        val apiCall: suspend () -> Boolean = if (isLike) {
            { apiClient.addLike(contentId) }
        } else {
            { apiClient.removeLike(contentId) }
        }
        queueAndSync(contentId, null, CollectionKind.LIKES, action, apiCall)
    }

    /**
     * Queues a bookmark/unbookmark interaction and attempts an immediate sync if connected.
     */
    suspend fun queueAndSyncBookmark(contentId: String, isBookmarked: Boolean) {
        val action = if (isBookmarked) ActionType.ADD else ActionType.REMOVE
        val apiCall: suspend () -> Boolean = if (isBookmarked) {
            { apiClient.addBookmark(contentId) }
        } else {
            { apiClient.removeBookmark(contentId) }
        }
        queueAndSync(contentId, null, CollectionKind.BOOKMARKS, action, apiCall)
    }

    /**
     * Queues a collection add/remove interaction and attempts an immediate sync if connected.
     */
    suspend fun queueAndSyncCollection(
        contentId: String,
        collectionId: String,
        isAdd: Boolean
    ) {
        val action = if (isAdd) ActionType.ADD else ActionType.REMOVE
        val apiCall: suspend () -> Boolean = if (isAdd) {
            { apiClient.addItemToCollection(collectionId, contentId) }
        } else {
            { apiClient.removeItemFromCollection(collectionId, contentId) }
        }
        queueAndSync(contentId, collectionId, CollectionKind.USER, action, apiCall)
    }

    /**
     * The core lifecycle method owned entirely by the queue manager:
     *
     * 1. Persist to queue immediately (safe against app death during API timeout)
     * 2. Attempt immediate sync if network is available
     * 3. On success: remove from queue (prevents duplicate send by the batch processor)
     * 4. On failure: leave in queue for the batch processor to retry later
     */
    private suspend fun queueAndSync(
        contentId: String,
        collectionId: String?,
        collectionKind: CollectionKind,
        action: ActionType,
        apiCall: suspend () -> Boolean
    ) = withContext(Dispatchers.IO) {
        Log.d("BookmarkDebug", "queueAndSync: contentId=$contentId, kind=$collectionKind, action=$action, queueSize=${queueDao.getQueueSize()}")
        // 1. Persist to queue first — this is our safety net
        val interaction = QueuedInteractionEntity(
            contentId = contentId,
            collectionId = collectionId,
            collectionKind = collectionKind,
            action = action,
            timestamp = System.currentTimeMillis()
        )
        queueDao.insert(interaction)
        Log.d(TAG, "Queued interaction: contentId=$contentId, collection=$collectionId, kind=$collectionKind, action=$action")

        // InteractionSyncService handles retry on reconnect — nothing else to do
        if (!networkMonitor.isCurrentlyConnected()) {
            Log.d(TAG, "Offline, interaction queued for later sync: contentId=$contentId")
            return@withContext
        }

        applicationScope.launch(Dispatchers.IO) {
            syncPendingInteractionsBeforeRefresh()
        }
    }

    suspend fun syncPendingInteractionsBeforeRefresh() {
        if (!networkMonitor.isCurrentlyConnected()) {
            awaitIdle()
            return
        }

        if (queueDao.getQueueSize() == 0) {
            awaitIdle()
            return
        }

        processQueuedInteractions()
    }

    suspend fun awaitIdle() {
        processingMutex.withLock {
            // Waiting for the lock is enough. No-op body by design.
        }
    }

    suspend fun getPendingInteractionsSnapshot(): List<QueuedInteractionEntity> = withContext(Dispatchers.IO) {
        deduplicateLatest(queueDao.getAllQueued())
    }

    suspend fun getCollectionMembershipOverlay(
        collectionKind: CollectionKind,
        collectionId: String? = null,
    ): CollectionMembershipOverlay = withContext(Dispatchers.IO) {
        val pending = deduplicateLatest(queueDao.getAllQueued())

        when (collectionKind) {
            CollectionKind.BOOKMARKS -> {
                val pendingBookmarkAdds = pending
                    .filter { it.collectionKind == CollectionKind.BOOKMARKS && it.action == ActionType.ADD }
                    .map { it.contentId }
                    .toSet()
                val pendingBookmarkRemovals = pending
                    .filter { it.collectionKind == CollectionKind.BOOKMARKS && it.action == ActionType.REMOVE }
                    .map { it.contentId }
                    .toSet()
                val pendingMovesToUserCollections = pending
                    .filter { it.collectionKind == CollectionKind.USER && it.action == ActionType.ADD }
                    .map { it.contentId }
                    .toSet()

                CollectionMembershipOverlay(
                    forceIncludeContentIds = pendingBookmarkAdds,
                    forceExcludeContentIds = pendingBookmarkRemovals + pendingMovesToUserCollections,
                )
            }

            CollectionKind.USER -> {
                val effectiveCollectionId = collectionId ?: return@withContext CollectionMembershipOverlay()
                CollectionMembershipOverlay(
                    forceIncludeContentIds = pending
                        .filter {
                            it.collectionKind == CollectionKind.USER &&
                                it.collectionId == effectiveCollectionId &&
                                it.action == ActionType.ADD
                        }
                        .map { it.contentId }
                        .toSet(),
                    forceExcludeContentIds = pending
                        .filter {
                            it.collectionKind == CollectionKind.USER &&
                                it.collectionId == effectiveCollectionId &&
                                it.action == ActionType.REMOVE
                        }
                        .map { it.contentId }
                        .toSet(),
                )
            }

            CollectionKind.LIKES -> CollectionMembershipOverlay()
        }
    }

    /**
     * Removes mutually exclusive collection interactions from the queue before
     * queuing a new one. BOOKMARKS and USER collections cannot coexist.
     *
     * Call this before [queueAndSyncCollection] or [queueAndSyncBookmark] when
     * moving content between collection types.
     */
    suspend fun clearConflictingInteractions(
        contentId: String,
        targetCollectionKind: CollectionKind
    ) = withContext(Dispatchers.IO) {
        when (targetCollectionKind) {
            CollectionKind.BOOKMARKS -> {
                // Moving to BOOKMARKS: clear any USER collection interaction
                removeQueuedInteraction(contentId, CollectionKind.USER)
                Log.d(TAG, "Cleared USER interaction for contentId=$contentId before queuing BOOKMARKS")
            }
            CollectionKind.USER -> {
                // Moving to USER: clear any BOOKMARKS interaction
                removeQueuedInteraction(contentId, CollectionKind.BOOKMARKS)
                Log.d(TAG, "Cleared BOOKMARKS interaction for contentId=$contentId before queuing USER")
            }
            CollectionKind.LIKES -> {
                // LIKES coexists with both — nothing to clear
            }
        }
    }

    /**
     * Processes all queued interactions by deduplicating and sending a batch request.
     * Called by [com.meninocoiso.bscm.service.InteractionSyncService] on connectivity restore or app resume.
     */
    suspend fun processQueuedInteractions() = withContext(Dispatchers.IO) {
        processingMutex.withLock {
            try {
                val allInteractions = queueDao.getAllQueued()

                if (allInteractions.isEmpty()) {
                    Log.d(TAG, "No interactions to process")
                    return@withLock
                }

                val processedIds = allInteractions.map { it.id }
                val deduplicatedInteractions = deduplicateLatest(allInteractions)

                Log.d(TAG, "Processing ${allInteractions.size} queued interactions")

                val batchRequest = deduplicatedInteractions.map { entity ->
                    BatchCollectionItemRequest(
                        contentId = entity.contentId,
                        collectionId = entity.collectionId,
                        collectionKind = entity.collectionKind,
                        action = entity.action
                    )
                }

                Log.d("BookmarkDebug", "processBatch: ${batchRequest.map { "${it.contentId}:${it.collectionKind}:${it.action}" }}")
                Log.d(TAG, "Sending batch of ${batchRequest.size} interactions to server")

                try {
                    val success = apiClient.batchProcessInteractions(batchRequest)
                    if (success) {
                        queueDao.deleteByIds(processedIds)
                        Log.d(TAG, "Batch succeeded, removed ${allInteractions.size} interactions")
                    } else {
                        Log.w(TAG, "Batch returned false, keeping items for next attempt")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Batch request failed, keeping items for next attempt", e)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in processQueuedInteractions", e)
            }
        }
    }

    /**
     * Returns the current number of pending interactions in the queue.
     */
    suspend fun getQueueSize(): Int = withContext(Dispatchers.IO) {
        queueDao.getQueueSize()
    }

    /**
     * Helper method to remove a specific queued interaction based on contentId and collection.
     */
    private suspend fun removeQueuedInteraction(
        contentId: String,
        collectionKind: CollectionKind,
        collectionId: String? = null
    ) = withContext(Dispatchers.IO) {
        val interaction = if (collectionId != null) {
            queueDao.getLatestForContent(contentId, collectionId)
        } else {
            queueDao.getLatestForContentByKind(contentId, collectionKind.name)
        }

        if (interaction != null) {
            queueDao.delete(interaction)
            Log.d(TAG, "Removed from queue: contentId=$contentId, kind=$collectionKind, collection=$collectionId")
        }
    }

    private fun deduplicateLatest(interactions: List<QueuedInteractionEntity>): List<QueuedInteractionEntity> {
        val latestInteractionsMap = mutableMapOf<String, QueuedInteractionEntity>()
        for (interaction in interactions) {
            val key = buildInteractionKey(interaction)
            val existing = latestInteractionsMap[key]
            if (existing == null || interaction.timestamp > existing.timestamp) {
                latestInteractionsMap[key] = interaction
            }
        }

        return latestInteractionsMap.values.toList()
    }

    private fun buildInteractionKey(interaction: QueuedInteractionEntity): String =
        "${interaction.contentId}:${interaction.collectionKind}:${interaction.collectionId.orEmpty()}"
}