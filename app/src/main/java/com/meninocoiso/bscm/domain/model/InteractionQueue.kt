package com.meninocoiso.bscm.domain.model

import com.meninocoiso.bscm.domain.enums.ContentType
import kotlinx.serialization.Serializable

@Serializable
data class QueuedInteraction(
    val id: String,
    val contentType: ContentType,
    val contentId: ULong,
    val interactionType: InteractionType,
    val timestamp: Long,
    val retryCount: Int = 0,
    val maxRetries: Int = 3
)

enum class InteractionType {
    LIKE,
    UNLIKE,
    BOOKMARK,
    UNBOOKMARK
}

data class InteractionResult(
    val success: Boolean,
    val shouldRetry: Boolean = false,
    val error: String? = null
)
