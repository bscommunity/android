package com.meninocoiso.bscm.data.manager

import android.util.Log
import com.meninocoiso.bscm.data.local.dao.InteractionQueueDao
import com.meninocoiso.bscm.data.local.entity.QueuedInteractionEntity
import com.meninocoiso.bscm.data.remote.ApiClient
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
private const val RECENT_SYNCED_ACTION_TTL_MILLIS = 30_000L

data class CollectionMembershipOverlay(
    val forceIncludeIds: Set<String> = emptySet(),
    val forceExcludeIds: Set<String> = emptySet(),
)

internal fun buildCollectionMembershipOverlay(
    pending: List<QueuedInteractionEntity>,
    collectionKind: CollectionKind,
    collectionId: String? = null,
): CollectionMembershipOverlay = when (collectionKind) {
    CollectionKind.BOOKMARKS -> CollectionMembershipOverlay(
        forceIncludeIds = pending
            .filter { it.collectionKind == CollectionKind.BOOKMARKS && it.action == ActionType.ADD }
            .map { it.id }
            .toSet(),
        forceExcludeIds = pending
            .filter { it.collectionKind == CollectionKind.BOOKMARKS && it.action == ActionType.REMOVE }
            .map { it.id }
            .toSet(),
    )

    CollectionKind.USER -> {
        val effectiveCollectionId = collectionId ?: return CollectionMembershipOverlay()
        CollectionMembershipOverlay(
            forceIncludeIds = pending
                .filter {
                    it.collectionKind == CollectionKind.USER &&
                        it.collectionId == effectiveCollectionId &&
                        it.action == ActionType.ADD
                }
                .map { it.id }
                .toSet(),
            forceExcludeIds = pending
                .filter {
                    it.collectionKind == CollectionKind.USER &&
                        it.collectionId == effectiveCollectionId &&
                        it.action == ActionType.REMOVE
                }
                .map { it.id }
                .toSet(),
        )
    }

    CollectionKind.LIKES -> CollectionMembershipOverlay()
}

private data class RecentSyncedAction(
    val id: String,
    val collectionId: String?,
    val collectionKind: CollectionKind,
    val action: ActionType,
    val timestamp: Long,
)

@Singleton
class InteractionQueueManager @Inject constructor(
    private val queueDao: InteractionQueueDao,
    private val apiClient: ApiClient,
    private val networkMonitor: NetworkConnectivityMonitor,
    @param:ApplicationScope private val applicationScope: CoroutineScope
) {
    private val processingMutex = Mutex()
    private val recentSyncedActionsMutex = Mutex()
    private val recentSyncedActions = mutableMapOf<String, RecentSyncedAction>()

    /**
     * Queues a like/unlike interaction and attempts an immediate sync if connected.
     * The queue is the safety net — if the app dies before sync completes, the
     * interaction is already persisted and will be retried on next session.
     */
    suspend fun queueAndSyncLike(id: String, isLike: Boolean) {
        val action = if (isLike) ActionType.ADD else ActionType.REMOVE
        queueAndSync(id, null, CollectionKind.LIKES, action)
    }

    /**
     * Queues a bookmark/unbookmark interaction and attempts an immediate sync if connected.
     */
    suspend fun queueAndSyncBookmark(id: String, isBookmarked: Boolean) {
        val action = if (isBookmarked) ActionType.ADD else ActionType.REMOVE
        queueAndSync(id, null, CollectionKind.BOOKMARKS, action)
    }

    /**
     * Queues a collection add/remove interaction and attempts an immediate sync if connected.
     */
    suspend fun queueAndSyncCollection(
        id: String,
        collectionId: String,
        isAdd: Boolean
    ) {
        val action = if (isAdd) ActionType.ADD else ActionType.REMOVE
        queueAndSync(id, collectionId, CollectionKind.USER, action)
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
        id: String,
        collectionId: String?,
        collectionKind: CollectionKind,
        action: ActionType,
    ) = withContext(Dispatchers.IO) {
        val interaction = QueuedInteractionEntity(
            id = id,
            collectionId = collectionId,
            collectionKind = collectionKind,
            action = action,
            timestamp = System.currentTimeMillis()
        )
        queueDao.insert(interaction)
        Log.d(TAG, "Queued interaction: id=$id, collection=$collectionId, kind=$collectionKind, action=$action")

        if (!networkMonitor.isCurrentlyConnected()) {
            Log.d(TAG, "Offline, interaction queued for later sync: id=$id")
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

        // Another caller is already syncing; just wait for completion.
        if (processingMutex.isLocked) {
            awaitIdle()
            return
        }

        if (queueDao.getQueueSize() == 0) {
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
        val pending = deduplicateLatest(queueDao.getAllQueued())
        val recent = getRecentSyncedActionsSnapshot()

        if (recent.isEmpty()) return@withContext pending

        // Recent synced actions are merged as an overlay in case the backend is eventually consistent.
        val mergedByKey = linkedMapOf<String, QueuedInteractionEntity>()
        pending.forEach { mergedByKey[buildInteractionKey(it)] = it }

        recent.forEach { action ->
            val key = buildInteractionKey(action.id, action.collectionKind, action.collectionId)
            val existing = mergedByKey[key]
            if (existing == null || action.timestamp > existing.timestamp) {
                mergedByKey[key] = QueuedInteractionEntity(
                    rowId = existing?.rowId ?: 0L,
                    id = action.id,
                    collectionId = action.collectionId,
                    collectionKind = action.collectionKind,
                    action = action.action,
                    timestamp = action.timestamp,
                    retryCount = 0,
                )
            }
        }

        mergedByKey.values.toList()
    }

    suspend fun getCollectionMembershipOverlay(
        collectionKind: CollectionKind,
        collectionId: String? = null,
    ): CollectionMembershipOverlay = withContext(Dispatchers.IO) {
        buildCollectionMembershipOverlay(
            pending = getPendingInteractionsSnapshot(),
            collectionKind = collectionKind,
            collectionId = collectionId,
        )
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
                    return@withLock
                }

                val processedIds = allInteractions.map { it.rowId }
                val deduplicatedInteractions = deduplicateLatest(allInteractions)

                Log.d(TAG, "Processing ${allInteractions.size} queued interactions")

                val batchRequest = deduplicatedInteractions.map { entity ->
                    BatchCollectionItemRequest(
                        catalogId = entity.id,
                        collectionId = entity.collectionId,
                        collectionKind = entity.collectionKind,
                        action = entity.action
                    )
                }

                Log.d(TAG, "Sending batch of ${batchRequest.size} interactions to server")

                try {
                    val success = apiClient.batchProcessInteractions(batchRequest)
                    if (success) {
                        queueDao.deleteByIds(processedIds)
                        rememberRecentlySyncedActions(deduplicatedInteractions)
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

    private suspend fun rememberRecentlySyncedActions(interactions: List<QueuedInteractionEntity>) {
        val now = System.currentTimeMillis()
        recentSyncedActionsMutex.withLock {
            pruneExpiredRecentActionsLocked(now)
            interactions.forEach { interaction ->
                recentSyncedActions[buildInteractionKey(interaction)] = RecentSyncedAction(
                    id = interaction.id,
                    collectionId = interaction.collectionId,
                    collectionKind = interaction.collectionKind,
                    action = interaction.action,
                    timestamp = now,
                )
            }
        }
    }

    private suspend fun getRecentSyncedActionsSnapshot(): List<RecentSyncedAction> {
        val now = System.currentTimeMillis()
        return recentSyncedActionsMutex.withLock {
            pruneExpiredRecentActionsLocked(now)
            recentSyncedActions.values.toList()
        }
    }

    private fun pruneExpiredRecentActionsLocked(now: Long) {
        val iterator = recentSyncedActions.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (now - entry.value.timestamp > RECENT_SYNCED_ACTION_TTL_MILLIS) {
                iterator.remove()
            }
        }
    }

    private fun buildInteractionKey(interaction: QueuedInteractionEntity): String =
        buildInteractionKey(interaction.id, interaction.collectionKind, interaction.collectionId)

    private fun buildInteractionKey(
        id: String,
        collectionKind: CollectionKind,
        collectionId: String?,
    ): String =
        "$id:$collectionKind:${collectionId.orEmpty()}"
}