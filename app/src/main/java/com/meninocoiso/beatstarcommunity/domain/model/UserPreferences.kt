package com.meninocoiso.beatstarcommunity.domain.model

import com.meninocoiso.beatstarcommunity.domain.enums.ThemePreference

data class UserPreferences(
	val allowExplicitContent: Boolean,
	val useDynamicColors: Boolean,
	val theme: ThemePreference
)