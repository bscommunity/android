package com.meninocoiso.beatstarcommunity.data.classes

import com.meninocoiso.beatstarcommunity.data.enums.RoleEnum
import java.util.Date

data class Author(
	val user: User,
	val role: RoleEnum,
	val createdAt: Date
)
