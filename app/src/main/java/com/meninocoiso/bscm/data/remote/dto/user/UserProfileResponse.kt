package com.meninocoiso.bscm.data.remote.dto.user

import kotlinx.serialization.Serializable

@Serializable
data class UserProfileResponse(
    val user: SimplifiedUser,
    val isFollowing: Boolean? = false,
    val counts: UserProfileCounts
)