package com.meninocoiso.beatstarcommunity.domain.lists

import com.meninocoiso.beatstarcommunity.R

data class Genre(
	val id: String,
	val name: String,
	val icon: Int,
)

// In the future, in case of featuring multiple languages on the app,
// remove the "name" property in favor of a dictionary for each language
val genresList = listOf(
	Genre("hip_hop", "Hip-hop", R.drawable.hiphop),
	Genre("pop", "Pop", R.drawable.pop),
	Genre("rnb", "R&B", R.drawable.rnb),
	Genre("rock", "Rock", R.drawable.rock),
	Genre("dance", "Dance", R.drawable.electronic),
	Genre("alternative", "Alternative", R.drawable.alternative),
)