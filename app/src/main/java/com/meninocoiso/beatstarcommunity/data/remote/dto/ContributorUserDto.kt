package com.meninocoiso.beatstarcommunity.data.remote.dto

import LocalDateSerializer
import android.os.Parcelable
import androidx.room.Entity
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Entity(tableName = "contributors_users")
@Serializable
@Parcelize
data class ContributorUserDto(
    val id: String,
    val username: String,
    val imageUrl: String?,
    @Serializable(with = LocalDateSerializer::class)
    val createdAt: LocalDate? = null,
) : Parcelable