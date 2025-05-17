package com.meninocoiso.beatstarcommunity.domain.model

import com.meninocoiso.beatstarcommunity.domain.enums.ThemePreference

data class Settings(
	val allowExplicitContent: Boolean = false,
	val useDynamicColors: Boolean = true,
	val theme: ThemePreference = ThemePreference.SYSTEM,
	val folderUri: String? = null,
	val latestUpdateVersion: String = "",
	val latestCleanedVersion: Int? = null
)