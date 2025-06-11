package com.meninocoiso.bscm.domain.model

import LocalDateSerializer
import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Entity(tableName = "versions")
@Serializable
@Parcelize
data class Version(
    @PrimaryKey val id: String,
    val index : Int,
    @ColumnInfo(name = "chart_id") val chartId: String,
    val duration: Float,
    @ColumnInfo(name = "notes_amount") val notesAmount: Int,
    @ColumnInfo(name = "effects_amount") val effectsAmount: Int,
    val bpm: Int,
    @ColumnInfo(name = "chart_url") val chartUrl: String,
    @ColumnInfo(name = "chart_preview_url") val chartPreviewUrl: String? = null,
    @ColumnInfo(name = "downloads_amount") val downloadsAmount: Int = 0,
    @ColumnInfo(name = "known_issues") val knownIssues: List<KnownIssue> = emptyList(),
    @Serializable(with = LocalDateSerializer::class)
    @ColumnInfo(name = "published_at") val publishedAt: LocalDate,
) : Parcelable