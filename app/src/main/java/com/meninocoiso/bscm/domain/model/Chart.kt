package com.meninocoiso.bscm.domain.model

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.meninocoiso.bscm.domain.enums.CatalogItemStatus
import com.meninocoiso.bscm.domain.enums.CatalogItemType
import com.meninocoiso.bscm.domain.enums.Difficulty
import com.meninocoiso.bscm.domain.enums.Visibility
import com.meninocoiso.bscm.domain.serialization.LocalDateTimeSerializer
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.time.LocalDateTime

@Entity(tableName = "charts")
@Serializable
@Parcelize
@SerialName("chart")
data class Chart(
    val track: Track,

    val difficulty: Difficulty,
    @ColumnInfo(name = "notes_amount") val notesAmount: Int,
    @ColumnInfo(name = "effects_amount") val effectsAmount: Int,
    @ColumnInfo(name = "is_deluxe") val isDeluxe: Boolean = false,
    @ColumnInfo(name = "is_explicit") val isExplicit: Boolean = false,

    override val type: @RawValue CatalogItemType = CatalogItemType.CHART,
    override val status: @RawValue CatalogItemStatus = CatalogItemStatus.PUBLISHED,
    override val visibility: Visibility = Visibility.PUBLIC,

    @ColumnInfo(name = "versions_count") val versionsCount: Int = 0,
    @ColumnInfo(name = "bundle_hash") val bundleHash: String? = null,

    @PrimaryKey @ColumnInfo(name = "id") override val id: String,
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

    val changelog: List<Changelog> = emptyList(),

    @ColumnInfo(name = "is_installed") override val isInstalled: Boolean? = false,
    @ColumnInfo(name = "latest_version") val latestVersion: Version? = null,
    @ColumnInfo(name = "available_version") var availableVersion: Version? = null,

    @ColumnInfo(name = "preview_video_id") override val previewVideoId: String? = null,
    @ColumnInfo(name = "discord_channel_id") override val discordChannelId: String? = null,
    @ColumnInfo(name = "discord_message_id") override val discordMessageId: String? = null,
    @ColumnInfo(name = "author_id") override val authorId: String? = null,
) : Parcelable, CatalogItem
