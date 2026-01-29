package com.meninocoiso.bscm.domain.model

import LocalDateTimeSerializer
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
@Parcelize
data class User(
    val id: String,
    val username: String,
    val email: String?,
    val avatarUrl: String?,
    val bannerUrl: String?,
    val accentColor: Long?,
    val discordId: String?,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime
) : Parcelable
