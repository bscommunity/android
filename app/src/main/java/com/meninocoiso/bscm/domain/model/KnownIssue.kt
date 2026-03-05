package com.meninocoiso.bscm.domain.model

import android.os.Parcelable
import androidx.room.Entity
import com.meninocoiso.bscm.domain.serialization.LocalDateTimeSerializer
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Entity(tableName = "known_issues")
@Serializable
@Parcelize
data class KnownIssue(
    val id: String,
    val description: String,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime
) : Parcelable