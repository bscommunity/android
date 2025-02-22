package com.meninocoiso.beatstarcommunity.domain.model

import android.os.Parcelable
import com.meninocoiso.beatstarcommunity.domain.enums.DifficultyEnum
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class Chart(
    val id: String,
    val artist: String,
    val track: String,
    val album: String?,
    val coverUrl: String,
    val difficulty: DifficultyEnum,
    val isDeluxe: Boolean,
    val isExplicit: Boolean,
    val isFeatured: Boolean,
    val versions: List<Version>,
    val contributors: List<Contributor>
) : Parcelable