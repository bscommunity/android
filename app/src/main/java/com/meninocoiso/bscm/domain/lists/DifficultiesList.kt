package com.meninocoiso.bscm.domain.lists

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.domain.enums.Difficulty

data class DifficultyItem(
    val id: Difficulty,
    val label: String,
    val icon: Int? = null
)

@Composable
fun getDifficultiesList(): List<DifficultyItem> {
    return listOf(
        DifficultyItem(Difficulty.NORMAL, stringResource(R.string.normal)),
        DifficultyItem(Difficulty.HARD, stringResource(R.string.hard), R.drawable.hard),
        DifficultyItem(Difficulty.EXTREME, stringResource(R.string.extreme), R.drawable.extreme),
        DifficultyItem(Difficulty.EXPERT, stringResource(R.string.expert)),
    )
}