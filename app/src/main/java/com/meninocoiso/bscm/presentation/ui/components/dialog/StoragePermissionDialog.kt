package com.meninocoiso.bscm.presentation.ui.components.dialog

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.meninocoiso.bscm.R
import kotlinx.coroutines.launch

private val DESIRED_URI = "content://com.android.externalstorage.documents/tree/primary%3Abeatstar".toUri()

@Composable
fun StoragePermissionDialog(
    onPermissionGranted: () -> Unit,
    onDismiss: () -> Unit,
    setFolderUri: suspend (Uri) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var incorrectPermissionState by remember {
        mutableStateOf(false)
    }

    // Register file picker launcher
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        println("Selected URI: $uri")
        
        if (uri != DESIRED_URI) {
            // If the selected URI is not the desired one, show an error and return
            println("Selected URI does not match the desired URI.")
            incorrectPermissionState = true
            return@rememberLauncherForActivityResult
        }

        // Take persistent permission
        val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION

        context.contentResolver.takePersistableUriPermission(uri, takeFlags)

        // Save the URI
        scope.launch {
            setFolderUri(uri)
            onPermissionGranted()
        }
    }

    AlertDialog(
        icon = {
            Icon(
                painter = painterResource(
                    R.drawable.rounded_folder_limited_24
                ),
                modifier = Modifier.size(24.dp),
                contentDescription = "Storage permission icon"
            )
        },
        title = {
            Text(
                text = stringResource(if (incorrectPermissionState) {
                    R.string.incorrect_storage_permission
                } else {
                    R.string.storage_permission_required
                }),
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Text(
                text = stringResource(if (incorrectPermissionState) {
                    R.string.incorrect_storage_permission_description
                } else {
                    R.string.storage_permission_required_description
                }
            ))
        },
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val initialUri =
                        // Try to find external storage - typically /storage/emulated/0
                        "content://com.android.externalstorage.documents/document/primary:".toUri()
                    folderPickerLauncher.launch(initialUri)
                }
            ) {
                Text(stringResource(if (incorrectPermissionState) {
                    R.string.try_again
                } else {
                    R.string.select_folder
                }))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}