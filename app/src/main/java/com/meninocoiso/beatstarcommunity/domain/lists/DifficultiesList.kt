package com.meninocoiso.beatstarcommunity.domain.lists

import com.meninocoiso.beatstarcommunity.R
import com.meninocoiso.beatstarcommunity.domain.enums.DifficultyEnum
import com.meninocoiso.beatstarcommunity.domain.model.Difficulty

// In the future, in case of featuring multiple languages on the app,
// remove the "name" property in favor of a dictionary for each language
val difficultiesList = listOf(
	Difficulty(DifficultyEnum.NORMAL, "Normal"),
	Difficulty(DifficultyEnum.HARD, "Hard", R.drawable.hard),
	Difficulty(DifficultyEnum.EXTREME, "Extreme", R.drawable.extreme),
)