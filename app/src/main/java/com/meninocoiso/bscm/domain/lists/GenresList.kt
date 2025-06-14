package com.meninocoiso.bscm.domain.lists

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.domain.enums.Genre

data class GenreItem(
	val id: Genre,
	val label: String,
	val icon: Int,
)

@Composable
fun getGenresList(): List<GenreItem> {
	return listOf(
		GenreItem(Genre.HIP_HOP, stringResource(R.string.hip_hop), R.drawable.hiphop),
		GenreItem(Genre.POP, stringResource(R.string.pop), R.drawable.pop),
		GenreItem(Genre.RNB, stringResource(R.string.r_b), R.drawable.rnb),
		GenreItem(Genre.ROCK, stringResource(R.string.rock), R.drawable.rock),
		GenreItem(Genre.DANCE, stringResource(R.string.dance), R.drawable.electronic),
		GenreItem(Genre.ALTERNATIVE, stringResource(R.string.alternative), R.drawable.alternative),
	)
}