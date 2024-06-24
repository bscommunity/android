package com.meninocoiso.beatstarcommunity.data.classes

import com.meninocoiso.beatstarcommunity.data.enums.DifficultyEnum
import java.util.Date

data class Chart(
	val id: Int,
	val song: Song,
	val notesAmount: Int,
	val knownIssues: List<String>? = null,
	val url: String, // URL to the chart .zip file (including: .chart, .config and song data)
	val difficulty: DifficultyEnum,
	val isFeatured: Boolean? = null,
	val isDeluxe: Boolean? = null,
	val createdAt: Date,
	val lastUpdatedAt: Date,
	val authors: List<User>,
	val versions: List<ChartVersion>? = null
)
