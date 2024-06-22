package com.meninocoiso.beatstarcommunity.data.classes

import com.meninocoiso.beatstarcommunity.data.enums.Role
import java.util.Date

data class Author(
	val user: User,
	val role: Role,
	val createdAt: Date
)
