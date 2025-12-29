package com.meninocoiso.bscm.domain.result

/**
 * Generic result wrapper for content operations
 */
sealed class ContentResult<out T> {
    data class Success<T>(val data: T) : ContentResult<T>()
    data class Error(val message: String, val cause: Throwable? = null) : ContentResult<Nothing>()
    data object Loading : ContentResult<Nothing>()
}

/**
 * Content loading/operation state
 */
sealed class ContentState {
    data object Loading : ContentState()
    data object Success : ContentState()
    data object Error : ContentState()
}

/**
 * One-time UI event
 */
sealed class ContentEvent {
    data class Error(val message: String) : ContentEvent()
}

