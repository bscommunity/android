package com.meninocoiso.bscm.domain.model.collection

import com.meninocoiso.bscm.domain.enums.ContentType
import kotlinx.serialization.Serializable

@Serializable
data class Collection(
    val id: ULong,
    val name: String,
    val isPublic: Boolean,
    val ownerId: String,
    val items: List<CollectionItem> = emptyList()
)

