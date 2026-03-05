package com.meninocoiso.bscm.domain.model

import android.os.Parcelable
import com.meninocoiso.bscm.data.remote.dto.user.SimplifiedUser
import com.meninocoiso.bscm.domain.serialization.LocalDateTimeSerializer
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

// User to SimplifiedUser mapping extension function
fun User.toSimplifiedUser(): SimplifiedUser {
    return SimplifiedUser(
        id = this.id,
        username = this.username,
        avatarUrl = this.avatarUrl,
        bannerUrl = this.bannerUrl,
        accentColor = this.accentColor,
    )
}