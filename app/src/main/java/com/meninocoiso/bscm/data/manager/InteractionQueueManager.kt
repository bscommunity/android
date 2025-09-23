package com.meninocoiso.bscm.data.manager

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.meninocoiso.bscm.data.local.dao.InteractionQueueDao
import com.meninocoiso.bscm.data.local.entity.QueuedInteractionEntity
import com.meninocoiso.bscm.data.remote.ApiClient
import com.meninocoiso.bscm.data.remote.dto.LikeRequest
import com.meninocoiso.bscm.domain.enums.ContentType
import com.meninocoiso.bscm.domain.model.InteractionResult
import com.meninocoiso.bscm.domain.model.InteractionType
import com.meninocoiso.bscm.domain.model.QueuedInteraction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "InteractionQueueManager"

@Singleton
class InteractionQueueManager @Inject constructor(
    private val queueDao: InteractionQueueDao,
    private val apiClient: ApiClient,
    private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    /**
     * Queues a like/unlike interaction for processing
     */
    suspend fun queueLikeInteraction(
        contentType: ContentType,
        contentId: ULong,
        isLike: Boolean
    ): String {
        val interactionId = UUID.randomUUID().toString()
        val interaction = QueuedInteractionEntity(
            id = interactionId,
            contentType = contentType,
            contentId = contentId,
            interactionType = if (isLike) InteractionType.LIKE else InteractionType.UNLIKE,
            timestamp = System.currentTimeMillis()
        )
        
        queueDao.insertInteraction(interaction)
        Log.d(TAG, "Queued ${if (isLike) "like" else "unlike"} interaction for $contentType:$contentId")
        
        // Try to process immediately if online
        if (isNetworkAvailable()) {
            processQueuedInteractions()
        }
        
        return interactionId
    }
    
    /**
     * Queues a bookmark/unbookmark interaction for processing
     */
    suspend fun queueBookmarkInteraction(
        contentType: ContentType,
        contentId: ULong,
        collectionId: ULong,
        userId: String,
        isBookmark: Boolean
    ): String {
        val interactionId = UUID.randomUUID().toString()
        val interaction = QueuedInteractionEntity(
            id = interactionId,
            contentType = contentType,
            contentId = contentId,
            interactionType = if (isBookmark) InteractionType.BOOKMARK else InteractionType.UNBOOKMARK,
            timestamp = System.currentTimeMillis()
        )
        
        queueDao.insertInteraction(interaction)
        Log.d(TAG, "Queued ${if (isBookmark) "bookmark" else "unbookmark"} interaction for $contentType:$contentId")
        
        // Try to process immediately if online
        if (isNetworkAvailable()) {
            processQueuedInteractions()
        }
        
        return interactionId
    }
    
    /**
     * Processes all queued interactions
     */
    fun processQueuedInteractions() {
        scope.launch {
            if (!isNetworkAvailable()) {
                Log.d(TAG, "Network not available, skipping queue processing")
                return@launch
            }
            
            val queuedInteractions = queueDao.getAllQueuedInteractions()
            Log.d(TAG, "Processing ${queuedInteractions.size} queued interactions")
            
            for (interaction in queuedInteractions) {
                try {
                    val result = processInteraction(interaction)
                    
                    if (result.success) {
                        queueDao.deleteInteraction(interaction.id)
                        Log.d(TAG, "Successfully processed interaction ${interaction.id}")
                    } else if (result.shouldRetry && interaction.retryCount < interaction.maxRetries) {
                        val updatedInteraction = interaction.copy(
                            retryCount = interaction.retryCount + 1
                        )
                        queueDao.updateInteraction(updatedInteraction)
                        Log.w(TAG, "Retry ${updatedInteraction.retryCount}/${interaction.maxRetries} for interaction ${interaction.id}")
                    } else {
                        queueDao.deleteInteraction(interaction.id)
                        Log.e(TAG, "Failed to process interaction ${interaction.id} after ${interaction.retryCount} retries: ${result.error}")
                    }
                    
                    // Add delay between requests to avoid rate limiting
                    delay(200)
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing interaction ${interaction.id}", e)
                    
                    if (interaction.retryCount < interaction.maxRetries) {
                        val updatedInteraction = interaction.copy(
                            retryCount = interaction.retryCount + 1
                        )
                        queueDao.updateInteraction(updatedInteraction)
                    } else {
                        queueDao.deleteInteraction(interaction.id)
                    }
                }
            }
        }
    }
    
    /**
     * Processes a single interaction
     */
    private suspend fun processInteraction(interaction: QueuedInteractionEntity): InteractionResult {
        return try {
            when (interaction.interactionType) {
                InteractionType.LIKE -> {
                    val request = LikeRequest(interaction.contentType, interaction.contentId)
                    val success = apiClient.likeContent(request)
                    InteractionResult(success = success, shouldRetry = !success)
                }
                
                InteractionType.UNLIKE -> {
                    val success = apiClient.unlikeContent(interaction.contentType, interaction.contentId)
                    InteractionResult(success = success, shouldRetry = !success)
                }
                
                InteractionType.BOOKMARK, InteractionType.UNBOOKMARK -> {
                    // For now, we'll implement bookmark logic when collection system is ready
                    // This is a placeholder for bookmark functionality
                    Log.w(TAG, "Bookmark functionality not yet implemented")
                    InteractionResult(success = false, shouldRetry = false, error = "Bookmark functionality not implemented")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to process interaction", e)
            InteractionResult(
                success = false,
                shouldRetry = true,
                error = e.message
            )
        }
    }
    
    /**
     * Gets the current queue as a Flow
     */
    fun getQueuedInteractionsFlow(): Flow<List<QueuedInteraction>> {
        return queueDao.getAllQueuedInteractionsFlow().map { entities ->
            entities.map { entity ->
                QueuedInteraction(
                    id = entity.id,
                    contentType = entity.contentType,
                    contentId = entity.contentId,
                    interactionType = entity.interactionType,
                    timestamp = entity.timestamp,
                    retryCount = entity.retryCount,
                    maxRetries = entity.maxRetries
                )
            }
        }
    }
    
    /**
     * Gets the current queue size
     */
    suspend fun getQueueSize(): Int {
        return queueDao.getQueueSize()
    }
    
    /**
     * Cleans up failed interactions that exceeded max retries
     */
    suspend fun cleanupFailedInteractions() {
        queueDao.deleteFailedInteractions()
    }
    
    /**
     * Checks if network is available
     */
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
