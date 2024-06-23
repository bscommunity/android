package com.meninocoiso.beatstarcommunity.data.classes

import com.meninocoiso.beatstarcommunity.data.enums.DifficultyEnum

data class Difficulty(
	val id: DifficultyEnum,
	val name: String,
	val icon: Int? = null,
)
