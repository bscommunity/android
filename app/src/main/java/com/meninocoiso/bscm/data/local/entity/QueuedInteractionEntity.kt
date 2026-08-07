package com.meninocoiso.bscm.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.meninocoiso.bscm.domain.enums.ActionType
import com.meninocoiso.bscm.domain.enums.CollectionKind

@Entity(tableName = "interaction_queue")
data class QueuedInteractionEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "row_id")
    val rowId: Long = 0,
    @ColumnInfo(name = "id")
    val id: String,
    val collectionId: String?,
    val collectionKind: CollectionKind,
    val action: ActionType,
    val timestamp: Long,
    val retryCount: Int = 0
)