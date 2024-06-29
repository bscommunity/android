package com.meninocoiso.beatstarcommunity.domain.model

import com.meninocoiso.beatstarcommunity.domain.enums.RoleEnum
import java.util.Date

data class Author(
	val user: User,
	val role: RoleEnum,
	val createdAt: Date
)
