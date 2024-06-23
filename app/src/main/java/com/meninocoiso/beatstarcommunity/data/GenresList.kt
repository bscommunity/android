package com.meninocoiso.beatstarcommunity.data

import com.meninocoiso.beatstarcommunity.R
import com.meninocoiso.beatstarcommunity.data.classes.Genre

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