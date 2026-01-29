package com.meninocoiso.bscm.domain.model

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.meninocoiso.bscm.domain.enums.Difficulty
import com.meninocoiso.bscm.domain.serialization.LocalDateTimeSerializer
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Entity(tableName = "versions")
@Serializable
@Parcelize
data class Version(
    @PrimaryKey @ColumnInfo(name = "id") val id: Long,
    @ColumnInfo(name = "chart_id") val chartId: String,
    val index: Int,
    val duration: Float,
    @ColumnInfo(name = "notes_amount") val notesAmount: Int,
    @ColumnInfo(name = "effects_amount") val effectsAmount: Int,
    val bpm: Int,
    val difficulty: Difficulty,
    @ColumnInfo(name = "is_deluxe") val isDeluxe: Boolean,
    @ColumnInfo(name = "isExplicit") val isExplicit: Boolean,
    @ColumnInfo(name = "bundle_url") val bundleUrl: String,
    @ColumnInfo(name = "preview_url") val previewUrl: String? = null,
    @ColumnInfo(name = "downloads_amount") val downloadsAmount: Int = 0,
    @ColumnInfo(name = "known_issues") val knownIssues: List<KnownIssue> = emptyList(),
    @Serializable(with = LocalDateTimeSerializer::class)
    @ColumnInfo(name = "published_at") val publishedAt: LocalDateTime,
) : Parcelable