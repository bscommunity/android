package com.meninocoiso.bscm.domain.model

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.meninocoiso.bscm.domain.serialization.LocalDateTimeSerializer
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Entity(tableName = "themes")
@Serializable
@Parcelize
data class Theme(
    val name: String,
    val replaces: String,
    @ColumnInfo(name = "preview_url") val previewUrl: String,

    @PrimaryKey @ColumnInfo(name = "id") override val id: String,
    @ColumnInfo(name = "content_id") override val contentId: String?,
    @ColumnInfo(name = "cover_url") override val coverUrl: String,
    @ColumnInfo(name = "is_featured") override val isFeatured: Boolean,
    @ColumnInfo(name = "downloads_sum") override val downloadsSum: Int = 0,

    @Serializable(with = LocalDateTimeSerializer::class)
    @ColumnInfo(name = "latest_published_at") override val latestPublishedAt: LocalDateTime,

    @ColumnInfo(name = "is_liked") override val isLiked: Boolean = false,
    @ColumnInfo(name = "is_bookmarked") override val isBookmarked: Boolean = false,
    @ColumnInfo(name = "is_installed") override val isInstalled: Boolean? = false,

    val contributors: List<Contributor>,
) : Parcelable, CatalogItem