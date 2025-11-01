package com.meninocoiso.bscm.domain.model.internal

import kotlinx.serialization.Serializable

@Serializable
data class ProjectContributor(
    val name: String,
    val role: String
)