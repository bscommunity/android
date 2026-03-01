package com.meninocoiso.bscm.domain.result

/**
 * Wrapper for UI text that can be either a raw string (dynamic) or a string resource id.
 */
sealed class UiText {
    data class Plain(val value: String) : UiText()
    data class Res(val resId: Int, val args: List<Any> = emptyList()) : UiText() {
        companion object {
            operator fun invoke(resId: Int, vararg args: Any) = Res(resId, args.toList())
        }
    }
}
