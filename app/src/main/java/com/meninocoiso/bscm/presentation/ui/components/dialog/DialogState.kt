package com.meninocoiso.bscm.presentation.ui.components.dialog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

/**
 * Generic dialog state manager that handles showing/hiding dialogs in a type-safe way.
 * Automatically saves state across configuration changes.
 *
 * Usage:
 * ```
 * val dialogState = rememberDialogState<MyDialogs>()
 *
 * dialogState.show(MyDialogs.DeleteConfirmation)
 * dialogState.show(MyDialogs.Report)
 *
 * dialogState.ShowDialog { dialog ->
 *     when (dialog) {
 *         MyDialogs.DeleteConfirmation -> DeleteDialog(...)
 *         MyDialogs.Report -> ReportDialog(...)
 *     }
 * }
 * ```
 */
class DialogState<T> internal constructor(
    private var currentDialog: T? = null,
    private val onDismiss: () -> Unit
) {
    fun show(dialog: T) {
        currentDialog = dialog
    }

    fun dismiss() {
        onDismiss()
    }

    fun isShowing(dialog: T): Boolean = currentDialog == dialog

    val isAnyDialogShowing: Boolean
        get() = currentDialog != null

    @Composable
    fun ShowDialog(content: @Composable (T) -> Unit) {
        currentDialog?.let { dialog ->
            content(dialog)
        }
    }
}

/**
 * Remember a dialog state that persists across configuration changes.
 *
 * @param T The type representing your dialog options (typically an enum or sealed class)
 */
@Composable
fun <T> rememberDialogState(): DialogState<T> {
    var currentDialog by rememberSaveable { mutableStateOf<T?>(null) }

    return DialogState(
        currentDialog = currentDialog,
        onDismiss = { currentDialog = null }
    ).also { state ->
        // Update internal state when external state changes
        state.show(currentDialog ?: return@also)
    }
}

/**
 * Simple enum-based dialog state for basic use cases
 */
@Composable
inline fun <reified T : Enum<T>> rememberEnumDialogState(initialValue: T? = null): Pair<T?, (T?) -> Unit> {
    var currentDialog by rememberSaveable { mutableStateOf(initialValue) }
    return Pair(currentDialog) { currentDialog = it }
}
