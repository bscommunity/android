package com.meninocoiso.beatstarcommunity.domain.model

import LocalDateSerializer
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
@Parcelize
data class KnownIssue(
    val id: String,
    val description: String,
    @Serializable(with = LocalDateSerializer::class)
    val createdAt: LocalDate
) : Parcelable