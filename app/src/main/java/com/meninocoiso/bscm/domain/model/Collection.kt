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
import kotlinx.serialization.Transient
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
    @ColumnInfo(name = "chart_count") val chartCount: Int = 0,
    @ColumnInfo(name = "tour_pass_count") val tourPassCount: Int = 0,
    @ColumnInfo(name = "theme_count") val themeCount: Int = 0,
    @ColumnInfo(name = "created_at") val createdAt: LocalDateTime,
    @ColumnInfo(name = "updated_at") val updatedAt: LocalDateTime,
) {
    @Ignore
    @Transient
    var owner: SimplifiedUser? = null

    @Ignore
    @Transient
    var items: List<CatalogItem> = emptyList()
}