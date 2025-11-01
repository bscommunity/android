package com.meninocoiso.bscm.domain.model.internal

import kotlinx.serialization.Serializable

@Serializable
data class ContributionCategory(
    val name: String,
    val contributors: List<ProjectContributor>
)