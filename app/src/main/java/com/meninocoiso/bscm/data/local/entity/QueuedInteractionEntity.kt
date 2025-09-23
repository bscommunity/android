package com.meninocoiso.bscm.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.meninocoiso.bscm.domain.enums.ContentType
import com.meninocoiso.bscm.domain.model.InteractionType

@Entity(tableName = "interaction_queue")
data class QueuedInteractionEntity(
    @PrimaryKey
    val id: String,
    val contentType: ContentType,
    val contentId: ULong,
    val interactionType: InteractionType,
    val timestamp: Long,
    val retryCount: Int = 0,
    val maxRetries: Int = 3
)
