package com.meninocoiso.bscm.domain.model.internal

data class UpdateCache(
    val latestUpdateVersion: String = "",
    val latestCleanedVersion: Int? = null,
)