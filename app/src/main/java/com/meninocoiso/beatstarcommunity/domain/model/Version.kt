package com.meninocoiso.beatstarcommunity.domain.model

import LocalDateSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class Version(
    val id: Int,
    val chartId: String,
    val index: Int,
    val duration: Float,
    val notesAmount: Int,
    val effectsAmount: Int,
    val bpm: Int,
    val chartUrl: String,
    val downloadsAmount: Int = 0,
    val knownIssues: List<KnownIssue> = emptyList(),
    @Serializable(with = LocalDateSerializer::class)
    val publishedAt: LocalDate,
)