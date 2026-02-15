package com.meninocoiso.bscm.data.manager

import android.util.Log
import com.meninocoiso.bscm.data.local.dao.InteractionQueueDao
import com.meninocoiso.bscm.data.local.entity.QueuedInteractionEntity
import com.meninocoiso.bscm.data.remote.ApiClient
import com.meninocoiso.bscm.data.remote.dto.collection.CreateCollectionItemRequest
import com.meninocoiso.bscm.domain.enums.ActionType
import com.meninocoiso.bscm.domain.enums.CollectionKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "InteractionQueueManager"
private const val BATCH_DELAY_MS = 3000L // 3 seconds to deduplicate interactions

@Singleton
class InteractionQueueManager @Inject constructor(
    private val queueDao: InteractionQueueDao,
    private val apiClient: ApiClient,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var batchJob: Job? = null

    // Already done by [InteractionSyncService]
    /*init {
        // Try to process any pending interactions from previous session
        scope.launch {
            try {
                if (queueDao.getQueueSize() > 0) {
                    processQueuedInteractions()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing pending interactions on init", e)
            }
        }
    }*/

    /**
     * Queue a like/unlike interaction
     */
    suspend fun queueLikeInteraction(contentId: String, isLike: Boolean): String {
        val action = if (isLike) ActionType.ADD else ActionType.REMOVE
        return queueInteraction(contentId, null, CollectionKind.LIKES, action)
    }

    /**
     * Queue a bookmark/unbookmark interaction
     */
    suspend fun queueBookmarkInteraction(contentId: String, isBookmarked: Boolean): String {
        val action = if (isBookmarked) ActionType.ADD else ActionType.REMOVE
        return queueInteraction(contentId, null, CollectionKind.BOOKMARKS, action)
    }

    /**
     * Queue a collection add/remove interaction
     */
    suspend fun queueCollectionInteraction(
        contentId: String,
        collectionId: String,
        isAdd: Boolean
    ): String {
        val action = if (isAdd) ActionType.ADD else ActionType.REMOVE
        return queueInteraction(contentId, collectionId, CollectionKind.USER, action)
    }

    /**
     * Generic method to queue any interaction
     */
    private suspend fun queueInteraction(
        contentId: String,
        collectionId: String?,
        collectionKind: CollectionKind = CollectionKind.USER,
        action: ActionType
    ): String = withContext(Dispatchers.IO) {
        if (collectionId == null && collectionKind == CollectionKind.USER) {
            throw IllegalArgumentException("Collection ID must be provided for user collections")
        }

        val interaction = QueuedInteractionEntity(
            contentId = contentId,
            collectionId = collectionId,
            collectionKind = collectionKind,
            action = action,
            timestamp = System.currentTimeMillis()
        )

        val id = queueDao.insert(interaction)
        Log.d(TAG, "Queued interaction: contentId=$contentId, collection=$collectionId, action=$action")

        // Schedule batch processing with delay to allow deduplication
        scheduleBatchProcessing()

        id.toString()
    }

    /**
     * Schedule batch processing with a delay to allow for deduplication
     */
    private fun scheduleBatchProcessing() {
        // Cancel existing job if any
        batchJob?.cancel()

        // Schedule a new batch processing job
        batchJob = scope.launch {
            delay(BATCH_DELAY_MS)
            try {
                processQueuedInteractions()
            } catch (e: Exception) {
                Log.e(TAG, "Error processing batch", e)
            }
        }
    }

    /**
     * Process queued interactions by deduplicating and sending batch request
     */
    suspend fun processQueuedInteractions() = withContext(Dispatchers.IO) {
        try {
            val allInteractions = queueDao.getAllQueued()

            if (allInteractions.isEmpty()) {
                Log.d(TAG, "No interactions to process")
                return@withContext
            }

            Log.d(TAG, "Processing ${allInteractions.size} queued interactions")

            // Deduplicate: For each contentId + collectionId, keep only the latest interaction
            val latestInteractionsMap = mutableMapOf<String, QueuedInteractionEntity>()

            for (interaction in allInteractions) {
                val key = "${interaction.contentId}:${interaction.collectionId}"
                val existing = latestInteractionsMap[key]

                if (existing == null || interaction.timestamp > existing.timestamp) {
                    latestInteractionsMap[key] = interaction
                }
            }

            val batchRequest = latestInteractionsMap.values.map { entity ->
                CreateCollectionItemRequest(
                    contentId = entity.contentId,
                    collectionId = entity.collectionId,
                    collectionKind = entity.collectionKind,
                    action = entity.action
                )
            }

            if (batchRequest.isEmpty()) {
                // This might happen if we had logic to cancel out interactions, but here we just take the latest.
                // If the latest is "Remove" and the item wasn't on server, passing "Remove" is fine (idempotent).
                return@withContext
            }

            Log.d(TAG, "Sending batch of ${batchRequest.size} interactions to server")

            // Send batch request to server
            try {
                val success = apiClient.batchProcessInteractions(batchRequest)

                if (success) {
                    // If successful, we can delete ALL interactions we processed,
                    // because the server state now matches the latest interaction.
                    // Note: In a highly concurrent scenario, new interactions might have come in
                    // since we fetched `allInteractions`. We should only delete `allInteractions`.
                    queueDao.delete(allInteractions)
                    Log.d(TAG, "Successfully processed and removed ${allInteractions.size} interactions")
                } else {
                    Log.w(TAG, "Batch processing returned false, keeping items in queue for next attempt")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send batch request, keeping items in queue", e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in processQueuedInteractions", e)
        }
    }


    /**
     * Get the current queue size
     */
    suspend fun getQueueSize(): Int = withContext(Dispatchers.IO) {
        queueDao.getQueueSize()
    }
}
