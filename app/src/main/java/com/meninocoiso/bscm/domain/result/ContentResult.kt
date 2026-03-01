package com.meninocoiso.bscm.domain.result

/**
 * Generic result wrapper for content operations
 */
sealed class ContentResult<out T> {
    data class Success<T>(val data: T) : ContentResult<T>()
    data class Error(val message: UiText, val cause: Throwable? = null) : ContentResult<Nothing>()
    data object Loading : ContentResult<Nothing>()
}