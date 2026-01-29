package com.meninocoiso.bscm.domain.result

/**
 * Content loading/operation state
 */
sealed class ContentState {
    data object Loading : ContentState()
    data object Success : ContentState()
    data object Error : ContentState()
}