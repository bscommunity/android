package com.meninocoiso.bscm.data.remote.dto

import com.meninocoiso.bscm.domain.enums.ContentType
import kotlinx.serialization.Serializable

@Serializable
data class LikeRequest(
    val contentType: ContentType,
    val contentId: ULong
)