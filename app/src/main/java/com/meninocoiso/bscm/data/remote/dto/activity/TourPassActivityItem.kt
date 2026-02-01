package com.meninocoiso.bscm.data.remote.dto.activity

import com.meninocoiso.bscm.domain.enums.ActivityType
import com.meninocoiso.bscm.domain.model.TourPass
import com.meninocoiso.bscm.domain.serialization.LocalDateTimeSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
data class TourPassActivityItem(
    override val id: String,
    override val type: ActivityType,
    @Serializable(with = LocalDateTimeSerializer::class)
    override val createdAt: LocalDateTime,
    val tourPass: TourPass
) : ActivityItemResponse()