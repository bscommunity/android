package com.meninocoiso.bscm.data.remote.dto.activity

import com.meninocoiso.bscm.domain.enums.ActivityType
import com.meninocoiso.bscm.domain.serialization.LocalDateTimeSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
sealed class ActivityItemResponse {
    abstract val id: String
    abstract val type: ActivityType
    @Serializable(with = LocalDateTimeSerializer::class)
    abstract val createdAt: LocalDateTime
}
