@file:UseSerializers(LocalDateTimeSerializer::class)

package com.meninocoiso.bscm.domain.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.meninocoiso.bscm.data.remote.dto.user.SimplifiedUser
import com.meninocoiso.bscm.domain.enums.CollectionKind
import com.meninocoiso.bscm.domain.serialization.LocalDateTimeSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.time.LocalDateTime

@Entity(tableName = "collections")
@Serializable
data class Collection(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "user_id") val userId: String,
    val kind: CollectionKind,
    val name: String,
    val slug: String? = null,
    @ColumnInfo(name = "is_public") val isPublic: Boolean,
    @ColumnInfo(name = "cover_url") val coverUrl: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: LocalDateTime,
    @ColumnInfo(name = "updated_at") val updatedAt: LocalDateTime,
    // Server-side gathered fields — not persisted in Room, but deserialized from API responses.
    // Counts are also overridden locally from the cross-ref table when the app has local data
    // (see CollectionRepositoryRemote.mergeLocalItemCounts).
    @Ignore val chartCount: Int = 0,
    @Ignore val tourPassCount: Int = 0,
    @Ignore val themeCount: Int = 0,
    @Ignore val owner: SimplifiedUser? = null,
) {
    // Secondary constructor required by Room (which ignores @Ignore fields)
    constructor(
        id: String,
        userId: String,
        kind: CollectionKind,
        name: String,
        slug: String?,
        isPublic: Boolean,
        coverUrl: String?,
        createdAt: LocalDateTime,
        updatedAt: LocalDateTime,
    ) : this(
        id = id,
        userId = userId,
        kind = kind,
        name = name,
        slug = slug,
        isPublic = isPublic,
        coverUrl = coverUrl,
        createdAt = createdAt,
        updatedAt = updatedAt,
        owner = null,
    )
}
