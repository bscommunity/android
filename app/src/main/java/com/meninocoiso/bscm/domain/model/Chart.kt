package com.meninocoiso.bscm.domain.model

import LocalDateSerializer
import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.meninocoiso.bscm.domain.enums.Difficulty
import com.meninocoiso.bscm.domain.enums.Genre
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Entity(tableName = "charts")
@Serializable
@Parcelize
data class Chart(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    val artist: String,
    val track: String,
    val album: String?,
    @ColumnInfo(name = "cover_url") val coverUrl: String,
    @ColumnInfo(name = "track_urls") val trackUrls: List<StreamingLink>,
    @ColumnInfo(name = "track_preview_url") val trackPreviewUrl: String,
    val difficulty: Difficulty,
    val genre: Genre,
    @ColumnInfo(name = "is_deluxe") val isDeluxe: Boolean,
    @ColumnInfo(name = "is_explicit") val isExplicit: Boolean,
    @ColumnInfo(name = "is_featured") val isFeatured: Boolean,
    @ColumnInfo(name = "is_installed") var isInstalled: Boolean? = false,
    @ColumnInfo(name = "downloads_sum") var downloadsSum: Int = 0,
    @Serializable(with = LocalDateSerializer::class)
    @ColumnInfo(name = "latest_published_at") val latestPublishedAt: LocalDate,
    @ColumnInfo(name = "latest_version") val latestVersion: Version,
    @ColumnInfo(name = "available_version") var availableVersion: Version? = null,
    val contributors: List<Contributor>
) : Parcelable