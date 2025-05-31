package com.meninocoiso.beatstarcommunity.domain.model.internal

import com.meninocoiso.beatstarcommunity.domain.enums.ThemePreference

data class Settings(
	val allowExplicitContent: Boolean = false,
	val useDynamicColors: Boolean = true,
	val theme: ThemePreference = ThemePreference.SYSTEM,
)