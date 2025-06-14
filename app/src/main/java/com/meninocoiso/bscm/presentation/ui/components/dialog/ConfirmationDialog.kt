package com.meninocoiso.bscm.presentation.ui.components.dialog

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.meninocoiso.bscm.R

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
        Text(text = stringResource(R.string.open_dialog))
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
    title: String = stringResource(R.string.confirmation_title),
    message: String = stringResource(R.string.confirmation_description),
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
                Text(text = stringResource(R.string.cancel))
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm()
                    onDismiss()
                }
            ) {
                Text(text = stringResource(R.string.confirm))
            }
        }
    )
}