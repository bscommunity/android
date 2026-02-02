package com.meninocoiso.bscm.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.meninocoiso.bscm.domain.enums.ActionType
import com.meninocoiso.bscm.domain.enums.CollectionKind

@Entity(tableName = "interaction_queue")
data class QueuedInteractionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val contentId: String,
    val collectionId: String?,
    val collectionKind: CollectionKind,
    val action: ActionType,
    val timestamp: Long,
    val retryCount: Int = 0
)