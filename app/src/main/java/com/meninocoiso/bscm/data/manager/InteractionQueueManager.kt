package com.meninocoiso.bscm.data.manager

import android.util.Log
import com.meninocoiso.bscm.data.local.dao.InteractionQueueDao
import com.meninocoiso.bscm.data.local.entity.QueuedInteractionEntity
import com.meninocoiso.bscm.data.remote.ApiClient
import com.meninocoiso.bscm.data.remote.dto.collection.CreateCollectionItemRequest
import com.meninocoiso.bscm.domain.enums.ActionType
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
private const val MAX_RETRIES = 3
private const val BATCH_DELAY_MS = 3000L // 3 seconds to deduplicate interactions

@Singleton
class InteractionQueueManager @Inject constructor(
    private val queueDao: InteractionQueueDao,
    private val apiClient: ApiClient,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var batchJob: Job? = null
    
    /**
     * Queue a like/unlike interaction
     */
    suspend fun queueLikeInteraction(contentId: String, isLike: Boolean): String {
        val action = if (isLike) ActionType.ADD else ActionType.REMOVE
        return queueInteraction(contentId, "likes", action)
    }
    
    /**
     * Queue a favorite/unfavorite interaction
     */
    suspend fun queueFavoriteInteraction(contentId: String, isFavorite: Boolean): String {
        val action = if (isFavorite) ActionType.ADD else ActionType.REMOVE
        return queueInteraction(contentId, "favorites", action)
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
        return queueInteraction(contentId, collectionId, action)
    }
    
    /**
     * Generic method to queue any interaction
     */
    private suspend fun queueInteraction(
        contentId: String,
        collectionId: String,
        action: ActionType
    ): String = withContext(Dispatchers.IO) {
        val interaction = QueuedInteractionEntity(
            contentId = contentId,
            collectionId = collectionId,
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
            val deduplicatedMap = mutableMapOf<String, QueuedInteractionEntity>()
            val interactionsToDelete = mutableListOf<QueuedInteractionEntity>()
            
            for (interaction in allInteractions) {
                val key = "${interaction.contentId}:${interaction.collectionId}"
                
                val existing = deduplicatedMap[key]
                if (existing == null) {
                    // First interaction for this key
                    deduplicatedMap[key] = interaction
                } else {
                    // Keep the latest one (highest timestamp)
                    if (interaction.timestamp > existing.timestamp) {
                        interactionsToDelete.add(existing)
                        deduplicatedMap[key] = interaction
                    } else {
                        interactionsToDelete.add(interaction)
                    }
                }
            }
            
            // Remove duplicates from the queue
            if (interactionsToDelete.isNotEmpty()) {
                queueDao.delete(interactionsToDelete)
                Log.d(TAG, "Removed ${interactionsToDelete.size} duplicate interactions")
            }
            
            // Check for opposite actions that cancel each other out
            val finalInteractions = mutableListOf<QueuedInteractionEntity>()
            val cancelledIds = mutableListOf<Long>()
            
            for ((key, interaction) in deduplicatedMap) {
                // Check if this interaction's action would result in no-op
                // For example, if the current state is already what we want
                finalInteractions.add(interaction)
            }
            
            if (finalInteractions.isEmpty()) {
                Log.d(TAG, "All interactions cancelled out")
                return@withContext
            }
            
            // Convert to batch request format
            val batchRequest = finalInteractions.map { entity ->
                CreateCollectionItemRequest(
                    contentId = entity.contentId,
                    collectionId = entity.collectionId,
                    action = entity.action
                )
            }
            
            Log.d(TAG, "Sending batch of ${batchRequest.size} interactions to server")
            
            // Send batch request to server
            /*try {
                val success = apiClient.batchProcessInteractions(batchRequest)
                
                if (success) {
                    // Clear all processed interactions
                    // val processedIds = finalInteractions.map { it.id }
                    queueDao.delete(finalInteractions)
                    Log.d(TAG, "Successfully processed and removed ${finalInteractions.size} interactions")
                } else {
                    Log.w(TAG, "Batch processing returned false")
                    handleRetries(finalInteractions)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send batch request", e)
                handleRetries(finalInteractions)
            }*/
        } catch (e: Exception) {
            Log.e(TAG, "Error in processQueuedInteractions", e)
        }
    }
    
    /**
     * Handle retry logic for failed interactions
     */
    private suspend fun handleRetries(interactions: List<QueuedInteractionEntity>) {
        for (interaction in interactions) {
            if (interaction.retryCount < MAX_RETRIES) {
                // Increment retry count
                val updated = interaction.copy(retryCount = interaction.retryCount + 1)
                queueDao.update(updated)
                Log.d(TAG, "Incremented retry count for interaction ${interaction.id} to ${updated.retryCount}")
            } else {
                // Max retries reached, remove from queue
                queueDao.delete(interaction)
                Log.w(TAG, "Max retries reached for interaction ${interaction.id}, removing from queue")
            }
        }
    }
    
    /**
     * Get the current queue size
     */
    suspend fun getQueueSize(): Int = withContext(Dispatchers.IO) {
        queueDao.getQueueSize()
    }
    
    /**
     * Get the latest queued action for a specific content and collection
     * This helps determine the optimistic UI state
     */
    suspend fun getLatestAction(contentId: String, collectionId: String): ActionType? =
        withContext(Dispatchers.IO) {
            queueDao.getLatestForContent(contentId, collectionId)?.action
        }
}
