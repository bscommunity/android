package com.meninocoiso.beatstarcommunity.presentation.ui.components.dialog

import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.meninocoiso.beatstarcommunity.R

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
                contentDescription = "App not found icon"
            )
        },
        title = {
            Text(text = "Modded app not installed")
        },
        text = {
            Text(text = "We couldn't find the modded app on your device. Visit the 'Installations' section on the Updates page to download the latest version.")
        },
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = onConfirmation) {
                Text("Go to Updates")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Dismiss")
            }
        }
    )
}