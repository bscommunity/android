package com.meninocoiso.bscm.data.remote.dto.user

import kotlinx.serialization.Serializable

@Serializable
data class UserProfileCounts(
    val library: Triple<Int, Int, Int>? = Triple(0, 0, 0),
    val likes: Triple<Int, Int, Int>? = Triple(0, 0, 0),
    val bookmarks: Triple<Int, Int, Int>? = Triple(0, 0, 0),
    val collections: Int? = 0,
    val followers: Int? = 0,
    val following: Int? = 0
)