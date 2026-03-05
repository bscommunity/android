package com.meninocoiso.bscm.domain.state

import com.meninocoiso.bscm.data.remote.dto.user.SimplifiedUser
import com.meninocoiso.bscm.domain.model.internal.Settings

sealed interface MainActivityState {
    data object Loading : MainActivityState
    data class Success(
        val settings: Settings,
        val latestUpdateVersion: String,
        val user: SimplifiedUser? = null,
    ) : MainActivityState
}