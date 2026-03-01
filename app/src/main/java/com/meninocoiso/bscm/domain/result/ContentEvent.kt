package com.meninocoiso.bscm.domain.result

/**
 * One-time UI event
 */
sealed class ContentEvent {
    data class Error(val message: UiText) : ContentEvent()
}