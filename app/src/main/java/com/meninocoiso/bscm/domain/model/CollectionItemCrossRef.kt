package com.meninocoiso.bscm.domain.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.meninocoiso.bscm.domain.enums.CatalogItemType
import java.time.LocalDateTime

@Entity(
    tableName = "collection_item_cross_ref",
    primaryKeys = ["collection_id", "content_id"],
    foreignKeys = [
        ForeignKey(
            entity = Collection::class,
            parentColumns = ["id"],
            childColumns = ["collection_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("content_id"), Index("collection_id")]
)
data class CollectionItemCrossRef(
    @ColumnInfo(name = "collection_id") val collectionId: String,
    @ColumnInfo(name = "content_id") val id: String,
    @ColumnInfo(name = "content_type") val contentType: CatalogItemType,
    @ColumnInfo(name = "added_at") val addedAt: LocalDateTime = LocalDateTime.now()
)