package com.meninocoiso.beatstarcommunity.domain.model

import LocalDateSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class KnownIssue(
    val id: String,
    val description: String,
    @Serializable(with = LocalDateSerializer::class)
    val createdAt: LocalDate
)