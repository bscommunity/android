package com.meninocoiso.beatstarcommunity.domain.lists

import com.meninocoiso.beatstarcommunity.R
import com.meninocoiso.beatstarcommunity.domain.enums.Difficulty

data class Difficulty(
	val difficulty: Difficulty,
	val id: String,
	val icon: Int? = null
)

// In the future, in case of featuring multiple languages on the app,
// remove the "name" property in favor of a dictionary for each language
val difficultiesList = listOf(
	Difficulty(Difficulty.NORMAL, "Normal"),
	Difficulty(Difficulty.HARD, "Hard", R.drawable.hard),
	Difficulty(Difficulty.EXTREME, "Extreme", R.drawable.extreme),
	Difficulty(Difficulty.EXPERT, "Expert"),
)