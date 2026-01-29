package com.meninocoiso.bscm.domain.state

import com.meninocoiso.bscm.domain.model.User
import com.meninocoiso.bscm.domain.model.internal.Settings

sealed interface MainActivityState {
    data object Loading : MainActivityState
    data class Success(
        val settings: Settings,
        val latestUpdateVersion: String,
        val cacheUser: User? = null,
    ) : MainActivityState
}