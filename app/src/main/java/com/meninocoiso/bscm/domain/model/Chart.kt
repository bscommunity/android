package com.meninocoiso.bscm.domain.model

import LocalDateTimeSerializer
import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.meninocoiso.bscm.domain.enums.Genre
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Entity(tableName = "charts")
@Serializable
@Parcelize
data class Chart(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    val contentId: String,
    val artist: String,
    val track: String,
    val album: String?,
    val genre: Genre?,
    @ColumnInfo(name = "cover_url") val coverUrl: String,
    @ColumnInfo(name = "track_urls") val trackUrls: List<StreamingLink>,
    @ColumnInfo(name = "track_preview_url") val trackPreviewUrl: String,
    @ColumnInfo(name = "is_featured") val isFeatured: Boolean,
    @ColumnInfo(name = "is_liked") var isLiked: Boolean = false,
    @ColumnInfo(name = "is_favorited") var isFavorited: Boolean = false,
    @ColumnInfo(name = "is_installed") var isInstalled: Boolean? = false,
    @ColumnInfo(name = "downloads_sum") var downloadsSum: Int = 0,
    @Serializable(with = LocalDateTimeSerializer::class)
    @ColumnInfo(name = "latest_published_at") val latestPublishedAt: LocalDateTime,
    @ColumnInfo(name = "latest_version") val latestVersion: Version,
    @ColumnInfo(name = "available_version") var availableVersion: Version? = null,
    val contributors: List<Contributor>
) : Parcelable