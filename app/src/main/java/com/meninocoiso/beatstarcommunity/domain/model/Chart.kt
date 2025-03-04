package com.meninocoiso.beatstarcommunity.domain.model

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.meninocoiso.beatstarcommunity.domain.enums.DifficultyEnum
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Entity(tableName = "charts")
@Serializable
@Parcelize
data class Chart(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    val artist: String,
    val track: String,
    val album: String?,
    @ColumnInfo(name = "cover_url") val coverUrl: String,
    val difficulty: DifficultyEnum,
    @ColumnInfo(name = "is_deluxe") val isDeluxe: Boolean,
    @ColumnInfo(name = "is_explicit") val isExplicit: Boolean,
    @ColumnInfo(name = "is_featured") val isFeatured: Boolean,
    val versions: List<Version>,
    val contributors: List<Contributor>
) : Parcelable