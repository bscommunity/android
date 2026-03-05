@file:UseSerializers(LocalDateTimeSerializer::class)

package com.meninocoiso.bscm.domain.model

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.meninocoiso.bscm.domain.serialization.LocalDateTimeSerializer
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.time.LocalDateTime

@Entity(tableName = "tour_passes")
@Serializable
@Parcelize
@SerialName("tour_pass")
data class TourPass(
    val name: String,
    val artist: String?,
    val charts: List<Chart>,

    @PrimaryKey @ColumnInfo(name = "id") override val id: String,
    @ColumnInfo(name = "content_id") override val contentId: String?,
    @ColumnInfo(name = "cover_url") override val coverUrl: String,
    @ColumnInfo(name = "is_featured") override val isFeatured: Boolean,
    @ColumnInfo(name = "downloads_sum") override val downloadsSum: Int = 0,

    @ColumnInfo(name = "created_at") override val createdAt: LocalDateTime,
    @ColumnInfo(name = "updated_at") override val updatedAt: LocalDateTime,
    @ColumnInfo(name = "liked_at") override val likedAt: LocalDateTime? = null,
    @ColumnInfo(name = "bookmarked_at") override val bookmarkedAt: LocalDateTime? = null,

    override val contributors: List<Contributor>,

    // Device-specific fields
    @ColumnInfo(name = "is_installed") override val isInstalled: Boolean? = false,
) : Parcelable, CatalogItem