package com.meninocoiso.beatstarcommunity.data

data class Song(
	val title: String,
	val artist: String,
	val isExplicit: Boolean,
	val coverArtUrl: String,
	val uploadedBy: User
)
