package com.meninocoiso.bscm.domain.model

import LocalDateSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class User(
    val id: String,
    val username: String,
    val email: String,
    val imageUrl: String?,
    val discordId: String?,
    @Serializable(with = LocalDateSerializer::class)
    val createdAt: LocalDate
)