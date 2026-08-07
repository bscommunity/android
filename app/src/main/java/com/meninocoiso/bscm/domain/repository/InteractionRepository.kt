package com.meninocoiso.bscm.domain.repository

interface InteractionRepository {
    
    /**
     * Queues a like interaction for offline-first processing
     */
    suspend fun likeContent(id: String): Result<Unit>

    /**
     * Queues an unlike interaction for offline-first processing
     */
    suspend fun unlikeContent(id: String): Result<Unit>

    /**
     * Queues a bookmark interaction for offline-first processing
     */
    suspend fun bookmarkContent(id: String): Result<Unit>

    /**
     * Queues an unbookmark interaction for offline-first processing
     */
    suspend fun unbookmarkContent(id: String): Result<Unit>

    /**
     * Adds content to a custom collection while preserving bookmark state.
     * Collections are additive labels layered on top of a bookmark.
     */
    suspend fun addToCollection(
        id: String,
        collectionId: String
    ): Result<Unit>

    /**
     * Removes content from a custom collection without touching bookmark state.
     */
    suspend fun removeFromCollection(
        id: String,
        collectionId: String
    ): Result<Unit>

    /**
     * Gets the current interaction queue size
     */
    suspend fun getQueueSize(): Int
    
    /**
     * Forces processing of the interaction queue
     */
    suspend fun processQueue()
}
