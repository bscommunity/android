package com.meninocoiso.bscm.data.repository

import com.meninocoiso.bscm.domain.enums.ContentType
import com.meninocoiso.bscm.domain.model.InteractionResult
import kotlinx.coroutines.flow.Flow

interface InteractionRepository {
    
    /**
     * Queues a like interaction for offline-first processing
     */
    suspend fun likeContent(
        contentType: ContentType,
        contentId: ULong
    ): Flow<Result<InteractionResult>>
    
    /**
     * Queues an unlike interaction for offline-first processing
     */
    suspend fun unlikeContent(
        contentType: ContentType,
        contentId: ULong
    ): Flow<Result<InteractionResult>>
    
    /**
     * Checks if content is liked (from local state or server)
     */
    suspend fun isContentLiked(
        contentType: ContentType,
        contentId: ULong
    ): Flow<Result<Boolean>>
    
    /**
     * Queues a bookmark interaction for offline-first processing
     */
    suspend fun bookmarkContent(
        contentType: ContentType,
        contentId: ULong,
        collectionId: ULong,
        userId: String
    ): Flow<Result<InteractionResult>>
    
    /**
     * Queues an unbookmark interaction for offline-first processing
     */
    suspend fun unbookmarkContent(
        contentType: ContentType,
        contentId: ULong,
        collectionId: ULong,
        userId: String
    ): Flow<Result<InteractionResult>>
    
    /**
     * Gets the current interaction queue size
     */
    suspend fun getQueueSize(): Int
    
    /**
     * Forces processing of the interaction queue
     */
    suspend fun processQueue()
}
