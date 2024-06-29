package com.meninocoiso.beatstarcommunity.data

import com.meninocoiso.beatstarcommunity.domain.enums.DifficultyEnum
import com.meninocoiso.beatstarcommunity.domain.model.Chart
import com.meninocoiso.beatstarcommunity.domain.model.Song
import com.meninocoiso.beatstarcommunity.domain.model.User
import com.meninocoiso.beatstarcommunity.util.DateUtils
import kotlinx.coroutines.flow.flowOf
import java.util.Date
import javax.inject.Inject

class WorkspaceRepository @Inject constructor() {
	// In a real app, this would be coming from a data source like a database
	val charts = flowOf(
		listOf(
			Chart(
				id = 1,
				song = Song(
					title = "What You Know Of Me",
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
		)
	)
}