package com.meninocoiso.bscm.domain.repository

import kotlinx.coroutines.flow.Flow

interface InteractionRepository {
    
    /**
     * Queues a like interaction for offline-first processing
     */
    suspend fun likeContent(id: String, contentId: String): Flow<Result<Unit>>

    /**
     * Queues an unlike interaction for offline-first processing
     */
    suspend fun unlikeContent(id: String, contentId: String): Flow<Result<Unit>>

    /**
     * Queues a bookmark interaction for offline-first processing
     */
    suspend fun bookmarkContent(id: String, contentId: String): Flow<Result<Unit>>
    
    /**
     * Queues an unbookmark interaction for offline-first processing
     */
    suspend fun unbookmarkContent(id: String, contentId: String): Flow<Result<Unit>>

    /**
     * Queues adding content to a custom collection
     */
    suspend fun addToCollection(
        contentId: String,
        collectionId: String
    ): Flow<Result<Unit>>
    
    /**
     * Queues removing content from a custom collection
     */
    suspend fun removeFromCollection(
        contentId: String,
        collectionId: String
    ): Flow<Result<Unit>>
    
    /**
     * Gets the current interaction queue size
     */
    suspend fun getQueueSize(): Int
    
    /**
     * Forces processing of the interaction queue
     */
    suspend fun processQueue()
}
