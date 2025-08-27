package com.meninocoiso.bscm.domain.model.auth

import kotlinx.serialization.Serializable

@Serializable
data class AuthRequest(
    val code: String,
    val redirectUri: String,
    val codeVerifier: String
)
