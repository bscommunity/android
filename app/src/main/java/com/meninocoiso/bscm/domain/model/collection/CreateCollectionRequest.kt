package com.meninocoiso.bscm.domain.model.collection

import kotlinx.serialization.Serializable

@Serializable
data class CreateCollectionRequest(
    val name: String,
    val isPublic: Boolean = false
)

