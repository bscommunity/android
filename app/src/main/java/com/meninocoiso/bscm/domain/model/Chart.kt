package com.meninocoiso.bscm.domain.model

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.meninocoiso.bscm.domain.enums.Genre
import com.meninocoiso.bscm.domain.serialization.LocalDateTimeSerializer
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Entity(tableName = "charts")
@Serializable
@Parcelize
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
    // Charts without a content_id were downloaded outside bscm's workshop
    
    @ColumnInfo(name = "cover_url") override val coverUrl: String,
    @ColumnInfo(name = "is_featured") override val isFeatured: Boolean,
    @ColumnInfo(name = "downloads_sum") override val downloadsSum: Int = 0,

    @Serializable(with = LocalDateTimeSerializer::class)
    @ColumnInfo(name = "latest_published_at") override val latestPublishedAt: LocalDateTime,

    @ColumnInfo(name = "is_liked") override val isLiked: Boolean = false,
    @ColumnInfo(name = "is_bookmarked") override val isBookmarked: Boolean = false,
    @ColumnInfo(name = "is_installed") override val isInstalled: Boolean? = false,

    @ColumnInfo(name = "latest_version") val latestVersion: Version,
    @ColumnInfo(name = "available_version") var availableVersion: Version? = null,
    val contributors: List<Contributor>
) : Parcelable, CatalogItem