package com.meninocoiso.bscm.domain.state

import com.meninocoiso.bscm.domain.model.User

data class AuthState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val user: User? = null
)