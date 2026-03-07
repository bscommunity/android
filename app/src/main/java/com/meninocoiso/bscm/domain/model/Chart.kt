@file:UseSerializers(LocalDateTimeSerializer::class)

package com.meninocoiso.bscm.domain.model

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.meninocoiso.bscm.domain.enums.Genre
import com.meninocoiso.bscm.domain.serialization.LocalDateTimeSerializer
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.time.LocalDateTime

@Entity(tableName = "charts")
@Serializable
@Parcelize
@SerialName("chart")
data class Chart(
    val artist: String,
    val track: String,
    val album: String?,
    val genre: Genre?,
    val colors: List<String>? = null,

    @ColumnInfo(name = "track_urls") val trackUrls: List<StreamingLink>,
    @ColumnInfo(name = "track_preview_url") val trackPreviewUrl: String? = null,

    @PrimaryKey @ColumnInfo(name = "id") override val id: String,
    @ColumnInfo(name = "content_id") override val contentId: String? = null,
    
    @ColumnInfo(name = "cover_url") override val coverUrl: String,
    @ColumnInfo(name = "is_featured") override val isFeatured: Boolean = false,
    @ColumnInfo(name = "downloads_sum") override val downloadsSum: Int = 0,

    @ColumnInfo(name = "created_at") override val createdAt: LocalDateTime,
    @ColumnInfo(name = "updated_at") override val updatedAt: LocalDateTime,
    @ColumnInfo(name = "liked_at") override val likedAt: LocalDateTime? = null,
    @ColumnInfo(name = "bookmarked_at") override val bookmarkedAt: LocalDateTime? = null,

    override val contributors: List<Contributor>,

    // Device-specific fields
    @ColumnInfo(name = "is_installed") override val isInstalled: Boolean = false,
    @ColumnInfo(name = "latest_version") val latestVersion: Version,
    @ColumnInfo(name = "available_version") var availableVersion: Version? = null,
) : Parcelable, CatalogItem