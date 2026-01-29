package com.meninocoiso.bscm.data.remote.dto

import android.os.Parcelable
import androidx.room.Entity
import com.meninocoiso.bscm.domain.serialization.LocalDateTimeSerializer
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Entity(tableName = "contributors_users")
@Serializable
@Parcelize
data class ContributorUserDto(
    val id: String,
    val username: String,
    val imageUrl: String?,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime? = null,
) : Parcelable