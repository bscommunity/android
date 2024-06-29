package com.meninocoiso.beatstarcommunity.domain.model

import com.meninocoiso.beatstarcommunity.domain.enums.DifficultyEnum

data class Difficulty(
	val id: DifficultyEnum,
	val name: String,
	val icon: Int? = null,
)
