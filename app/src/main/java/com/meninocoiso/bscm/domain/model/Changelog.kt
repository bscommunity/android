package com.meninocoiso.bscm.domain.model

import android.os.Parcelable
import com.meninocoiso.bscm.domain.serialization.LocalDateTimeSerializer
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.time.LocalDateTime

@Parcelize
@Serializable
data class Changelog(
    val id: String,
    val description: String,
    @Serializable(with = LocalDateTimeSerializer::class) val createdAt: LocalDateTime,
) : Parcelable
