package com.meninocoiso.beatstarcommunity.data

import com.meninocoiso.beatstarcommunity.data.classes.Chart
import com.meninocoiso.beatstarcommunity.data.classes.Song
import com.meninocoiso.beatstarcommunity.data.classes.User
import com.meninocoiso.beatstarcommunity.data.enums.DifficultyEnum
import com.meninocoiso.beatstarcommunity.utils.DateUtils
import java.util.Date

val placeholderChart = Chart(
	id = 1,
	song = Song(
		title = "Overdrive",
		duration = 1532.25f,
		artists = listOf("Metrik", "Grafix"),
		isExplicit = false,
		coverArtUrl = "https://picsum.photos/76",
		uploadedBy = User(
			username = "meninocoiso",
			email = "william.henry.moody@my-own-personal-domain.com",
			avatarUrl = "https://github.com/theduardomaciel.png",
			createdAt = DateUtils.getRandomDateInYear(2023),
		)
	),
	createdAt = DateUtils.getRandomDateInYear(2023),
	lastUpdatedAt = DateUtils.getRandomDateInYear(2023),
	url = "",
	difficulty = DifficultyEnum.EXTREME,
	notesAmount = 435,
	knownIssues = listOf(
		"Missing clap effects on bridge",
		"Wrong direction swipe effect",
		"Unsyncronized section after drop"
	),
	authors = listOf(
		User(
			username = "meninocoiso",
			email = "teste@gmail.com",
			avatarUrl = "https://github.com/theduardomaciel.png",
			createdAt = Date(),
			charts = null
		),
		User(
			username = "oCosmo55",
			email = "teste@gmail.com",
			avatarUrl = "https://github.com/oCosmo55.png",
			createdAt = Date(),
		)
	)
)