package com.meninocoiso.bscm.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import LocalDateTimeSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
@Parcelize
data class User(
    val id: String,
    val username: String,
    val email: String?,
    val imageUrl: String?,
    val discordId: String?,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime
) : Parcelable
