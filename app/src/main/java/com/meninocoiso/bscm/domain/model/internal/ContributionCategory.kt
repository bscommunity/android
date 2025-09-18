package com.meninocoiso.bscm.domain.model.internal

import kotlinx.serialization.Serializable

@Serializable
data class Contributor(
    val name: String,
    val role: String
)

@Serializable
data class ContributionCategory(
    val name: String,
    val contributors: List<Contributor>
)