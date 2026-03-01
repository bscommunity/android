package com.meninocoiso.bscm.domain.result

/**
 * Wrapper for UI text that can be either a raw string (dynamic) or a string resource id.
 */
sealed class UiText {
    data class DynamicString(val value: String) : UiText()
    data class StringResource(val resId: Int, val args: List<Any> = emptyList()) : UiText()
}
