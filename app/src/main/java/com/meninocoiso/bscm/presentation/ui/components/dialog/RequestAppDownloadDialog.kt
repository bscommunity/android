package com.meninocoiso.bscm.presentation.ui.components.dialog

import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.meninocoiso.bscm.R

@Composable
fun RequestAppDownloadDialog(
    onDismissRequest: () -> Unit,
    onConfirmation: () -> Unit
) {
    AlertDialog(
        icon = {
            Icon(
                painter = painterResource(
                    R.drawable.baseline_device_unknown_24
                ),
                modifier = Modifier.size(24.dp),
                contentDescription = null
            )
        },
        title = {
            Text(text = stringResource(R.string.mod_not_installed))
        },
        text = {
            Text(text = stringResource(R.string.mod_not_installed_description))
        },
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = onConfirmation) {
                Text(stringResource(R.string.go_to_updates))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.dismiss))
            }
        }
    )
}