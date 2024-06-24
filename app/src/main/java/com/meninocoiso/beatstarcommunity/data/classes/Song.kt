package com.meninocoiso.beatstarcommunity.data.classes

data class Song(
	val title: String,
	val duration: Float,
	val artists: List<String>,
	val isExplicit: Boolean,
	val coverArtUrl: String,
	val uploadedBy: User
)
