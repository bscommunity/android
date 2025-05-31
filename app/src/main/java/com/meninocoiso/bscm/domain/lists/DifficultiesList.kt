package com.meninocoiso.bscm.domain.lists

import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.domain.enums.Difficulty

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