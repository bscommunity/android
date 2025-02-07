package com.meninocoiso.beatstarcommunity.domain.model

import com.meninocoiso.beatstarcommunity.domain.enums.DifficultyEnum
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
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
    val versions: List<Version> = emptyList(),
    val contributors: List<Contributor> = emptyList()
)