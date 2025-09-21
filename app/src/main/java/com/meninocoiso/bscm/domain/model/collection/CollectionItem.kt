package com.meninocoiso.bscm.domain.model.collection

import com.meninocoiso.bscm.domain.enums.ContentType
import kotlinx.serialization.Serializable

@Serializable
data class CollectionItem(
    val contentType: ContentType,
    val contentId: ULong
)

