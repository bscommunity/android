package com.meninocoiso.bscm.domain.model

import com.meninocoiso.bscm.domain.enums.ContentType
import kotlinx.serialization.Serializable

@Serializable
data class LikedContent(
    val contentType: ContentType,
    val contentId: ULong
)