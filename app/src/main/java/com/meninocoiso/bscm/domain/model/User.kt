package com.meninocoiso.bscm.domain.model

import LocalDateTimeSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
data class User(
    val id: String,
    val username: String,
    val email: String?,
    val imageUrl: String?,
    val discordId: String?,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime
)