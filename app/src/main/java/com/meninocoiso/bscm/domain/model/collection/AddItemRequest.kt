package com.meninocoiso.bscm.domain.model.collection

import com.meninocoiso.bscm.domain.enums.ContentType
import kotlinx.serialization.Serializable

@Serializable
data class AddItemRequest(
    val contentType: ContentType,
    val contentId: ULong
)

