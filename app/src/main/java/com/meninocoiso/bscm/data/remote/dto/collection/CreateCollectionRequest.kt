package com.meninocoiso.bscm.data.remote.dto.collection

import kotlinx.serialization.Serializable

@Serializable
data class CreateCollectionRequest(
    val name: String,
    val isPublic: Boolean = false
)