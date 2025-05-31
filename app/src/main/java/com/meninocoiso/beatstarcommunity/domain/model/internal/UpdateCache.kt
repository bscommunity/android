package com.meninocoiso.beatstarcommunity.domain.model.internal

data class UpdateCache(
    val latestUpdateVersion: String = "",
    val latestCleanedVersion: Int? = null,
)