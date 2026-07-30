package com.meninocoiso.bscm.domain.model

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.meninocoiso.bscm.domain.enums.CatalogItemStatus
import com.meninocoiso.bscm.domain.enums.CatalogItemType
import com.meninocoiso.bscm.domain.enums.Visibility
import com.meninocoiso.bscm.domain.serialization.LocalDateTimeSerializer
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
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
    val description: String? = null,
    val artist: String? = null,
    val charts: List<Chart>,
    @ColumnInfo(name = "likes_count") val likesCount: Int = 0,
    @ColumnInfo(name = "bookmarks_count") val bookmarksCount: Int = 0,

    override val type: @RawValue CatalogItemType = CatalogItemType.TOUR_PASS,
    override val status: @RawValue CatalogItemStatus = CatalogItemStatus.PUBLISHED,
    override val visibility: Visibility = Visibility.PUBLIC,

    @PrimaryKey @ColumnInfo(name = "id") override val id: String,
    @ColumnInfo(name = "cover_url") val coverUrl: String? = null,
    @ColumnInfo(name = "is_featured") override val isFeatured: Boolean = false,
    @ColumnInfo(name = "downloads_sum") override val downloadsSum: Int = 0,

    @Serializable(with = LocalDateTimeSerializer::class)
    @ColumnInfo(name = "created_at") override val createdAt: LocalDateTime,
    @Serializable(with = LocalDateTimeSerializer::class)
    @ColumnInfo(name = "published_at") override val publishedAt: LocalDateTime? = null,
    @Serializable(with = LocalDateTimeSerializer::class)
    @ColumnInfo(name = "updated_at") override val updatedAt: LocalDateTime? = null,
    @Serializable(with = LocalDateTimeSerializer::class)
    @ColumnInfo(name = "liked_at") override val likedAt: LocalDateTime? = null,
    @Serializable(with = LocalDateTimeSerializer::class)
    @ColumnInfo(name = "bookmarked_at") override val bookmarkedAt: LocalDateTime? = null,

    override val contributors: List<Contributor>,

    @ColumnInfo(name = "is_installed") override val isInstalled: Boolean? = false,

    @ColumnInfo(name = "preview_video_id") override val previewVideoId: String? = null,
    @ColumnInfo(name = "discord_channel_id") override val discordChannelId: String? = null,
    @ColumnInfo(name = "discord_message_id") override val discordMessageId: String? = null,
    @ColumnInfo(name = "author_id") override val authorId: String? = null,
) : Parcelable, CatalogItem
