package com.meninocoiso.bscm.domain.model.auth

import com.meninocoiso.bscm.domain.model.User
import kotlinx.serialization.Serializable

@Serializable
data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
    val tokenType: String = "Bearer",
    val user: User? = null
)
