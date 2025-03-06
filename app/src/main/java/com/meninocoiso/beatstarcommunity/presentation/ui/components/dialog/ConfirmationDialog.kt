package com.meninocoiso.beatstarcommunity.presentation.ui.components.dialog

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
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
        isOpened,
        onConfirm = {}
    )
}

@Composable
fun ConfirmationDialog(
    isOpened: MutableState<Boolean>,
    onConfirm: () -> Unit,
    title: String = "Are you sure?",
    message: String = "This action cannot be undone",
) {
    when {
        isOpened.value -> {
            AlertDialog(
                onDismissRequest = { isOpened.value = false },
                title = {
                    Text(text = title)
                },
                text = {
                    Text(text = message)
                },
                dismissButton = {
                    TextButton(onClick = {
                        isOpened.value = false
                    }) {
                        Text(text = "Cancel")
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onConfirm()
                            isOpened.value = false
                        }
                    ) {
                        Text(text = "Confirm")
                    }
                }
            )
        }
    }
}