package com.meninocoiso.beatstarcommunity.data

import com.meninocoiso.beatstarcommunity.R
import com.meninocoiso.beatstarcommunity.data.classes.Difficulty
import com.meninocoiso.beatstarcommunity.data.enums.DifficultyEnum

// In the future, in case of featuring multiple languages on the app,
// remove the "name" property in favor of a dictionary for each language
val difficultiesList = listOf(
	Difficulty(DifficultyEnum.NORMAL, "Normal"),
	Difficulty(DifficultyEnum.HARD, "Hard", R.drawable.hard),
	Difficulty(DifficultyEnum.EXTREME, "Extreme", R.drawable.extreme),
)