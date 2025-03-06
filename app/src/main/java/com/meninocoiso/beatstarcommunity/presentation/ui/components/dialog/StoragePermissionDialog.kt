package com.meninocoiso.beatstarcommunity.presentation.ui.components.dialog

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.meninocoiso.beatstarcommunity.util.DownloadUtils
import kotlinx.coroutines.launch

@Composable
fun StoragePermissionDialog(
    onPermissionGranted: () -> Unit,
    onDismiss: () -> Unit,
    downloadUtils: DownloadUtils
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Register file picker launcher
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            // Take persistent permission
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION

            context.contentResolver.takePersistableUriPermission(uri, takeFlags)

            // Save the URI
            scope.launch {
                downloadUtils.setFolderUri(uri.toString())
                onPermissionGranted()
            }
        } else {
            onDismiss()
        }
    }

    AlertDialog(
        /*icon = {
            Icon(
                painter = painterResource(
                    R.drawable.rounded_folder_limited_24
                ),
                modifier = Modifier.size(24.dp),
                contentDescription = "Storage permission icon"
            )
        },*/
        title = {
            Text(
                text = "Storage Permission Required",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Text(
                text = "To download charts, the app needs permission to access your storage. " +
                        "Please select the root folder or create a 'beatstar' folder."
            )
        },
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val initialUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        // Try to find external storage - typically /storage/emulated/0
                        Uri.parse("content://com.android.externalstorage.documents/document/primary:")
                    } else {
                        null
                    }
                    folderPickerLauncher.launch(initialUri)
                }
            ) {
                Text("Select folder")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}