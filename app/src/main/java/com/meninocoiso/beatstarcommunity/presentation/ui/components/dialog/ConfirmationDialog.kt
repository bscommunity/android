package com.meninocoiso.beatstarcommunity.presentation.ui.components.dialog

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview

@Preview
@Composable
fun ConfirmationDialogPreview(
) {
    val isOpened = remember { mutableStateOf(false) }
    Button(
        onClick = {
            isOpened.value = true
        }
    ) {
        Text(text = "Open dialog")
    }
    ConfirmationDialog(
        onDismiss = {
            isOpened.value = false
        },
        onConfirm = {}
    )
}

@Composable
fun ConfirmationDialog(
    onDismiss: () -> Unit = {},
    onConfirm: () -> Unit,
    title: String = "Are you sure?",
    message: String = "This action cannot be undone",
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = title)
        },
        text = {
            Text(text = message)
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel")
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm()
                    onDismiss()
                }
            ) {
                Text(text = "Confirm")
            }
        }
    )
}