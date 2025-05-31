package com.meninocoiso.bscm.domain.model

import LocalDateSerializer
import android.os.Parcelable
import androidx.room.Entity
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Entity(tableName = "known_issues")
@Serializable
@Parcelize
data class KnownIssue(
    val id: String,
    val description: String,
    @Serializable(with = LocalDateSerializer::class)
    val createdAt: LocalDate
) : Parcelable