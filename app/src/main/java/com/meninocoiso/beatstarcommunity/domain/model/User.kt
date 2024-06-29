package com.meninocoiso.beatstarcommunity.domain.model

import java.util.Date

data class User(
	val username: String,
	val email: String,
	val avatarUrl: String,
	val createdAt: Date,
	val charts: List<Chart>? = null
)
