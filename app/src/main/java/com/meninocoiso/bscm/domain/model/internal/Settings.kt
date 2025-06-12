package com.meninocoiso.bscm.domain.model.internal

import com.meninocoiso.bscm.domain.enums.ThemePreference

data class Settings(
	val allowExplicitContent: Boolean = false,
	val enableGameplayPreviewVideo: Boolean = true,
	val useDynamicColors: Boolean = true,
	val theme: ThemePreference = ThemePreference.SYSTEM,
)