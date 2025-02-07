package com.meninocoiso.beatstarcommunity.domain.lists

import com.meninocoiso.beatstarcommunity.R
import com.meninocoiso.beatstarcommunity.domain.enums.DifficultyEnum

data class Difficulty(
	val difficultyEnum: DifficultyEnum,
	val id: String,
	val icon: Int? = null
)

// In the future, in case of featuring multiple languages on the app,
// remove the "name" property in favor of a dictionary for each language
val difficultiesList = listOf(
	Difficulty(DifficultyEnum.Normal, "Normal"),
	Difficulty(DifficultyEnum.Hard, "Hard", R.drawable.hard),
	Difficulty(DifficultyEnum.Extreme, "Extreme", R.drawable.extreme),
)