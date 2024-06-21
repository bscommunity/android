package com.meninocoiso.beatstarcommunity.data

import java.util.Date

data class Chart(
	val id: Int,
	val song: Song,
	val url: String, // URL to the chart .zip file (including: .chart, .config and song data)
	val difficulty: Difficulty,
	val isDeluxe: Boolean,
	val createdAt: Date,
	val lastUpdatedAt: Date,
	val authors: List<User>,
	val versions: List<ChartVersion>
)
