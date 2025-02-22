package com.meninocoiso.beatstarcommunity.data.remote.dto

import LocalDateSerializer
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
@Parcelize
data class ContributorUserDto(
    val id: String,
    val username: String,
    val imageUrl: String?,
    @Serializable(with = LocalDateSerializer::class)
    val createdAt: LocalDate? = null,
) : Parcelable