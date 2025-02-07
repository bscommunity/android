package com.meninocoiso.beatstarcommunity.data.remote.dto

import LocalDateSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class ContributorUserDto(
    val id: String,
    val username: String,
    val imageUrl: String?,
    @Serializable(with = LocalDateSerializer::class)
    val createdAt: LocalDate,
)