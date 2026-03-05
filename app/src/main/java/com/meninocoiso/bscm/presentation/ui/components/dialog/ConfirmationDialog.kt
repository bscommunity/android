package com.meninocoiso.bscm.presentation.ui.components.dialog

import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
    onDismiss: (() -> Unit) = {},
    onConfirm: (() -> Unit)? = null,
    title: String = stringResource(R.string.confirmation_title),
    message: String = stringResource(R.string.confirmation_description),
    isLoading: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = {
            Text(text = title)
        },
        text = {
            Text(text = message)
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text(text = stringResource(R.string.cancel))
            }
        },
        confirmButton = {
            if (onConfirm != null) {
                Button(onClick = { onConfirm() }, enabled = !isLoading) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    } else {
                        Text(text = stringResource(R.string.confirm))
                    }
                }
            }
        }
    )
}