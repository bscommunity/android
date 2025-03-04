package com.meninocoiso.beatstarcommunity.presentation.ui.components.download

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.meninocoiso.beatstarcommunity.presentation.viewmodel.ChartDetailsViewModel
import kotlinx.coroutines.launch

@Composable
fun StoragePermissionDialog(
    onPermissionGranted: () -> Unit,
    onDismiss: () -> Unit,
    viewModel: ChartDetailsViewModel
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
                viewModel.setFolderUri(uri.toString())
                onPermissionGranted()
            }
        } else {
            onDismiss()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Storage Permission Required",
                    style = MaterialTheme.typography.headlineSmall
                )

                Text(
                    text = "To download charts, the app needs permission to access your storage. " +
                            "Please select the root folder or create a 'beatstar' folder."
                )

                Button(
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
                    Text("Select Folder")
                }

                Button(
                    onClick = onDismiss
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

@Composable
fun StoragePermissionHandler(
    onPermissionGranted: () -> Unit,
    viewModel: ChartDetailsViewModel
) {
    val context = LocalContext.current

    // Check if we already have a valid folder URI
    LaunchedEffect(Unit) {
        val folderUri = viewModel.getFolderUri()
        if (!folderUri.isNullOrEmpty()) {
            try {
                val uri = Uri.parse(folderUri)

                // Check if the URI is still valid
                val flags = context.contentResolver.persistedUriPermissions
                    .find { it.uri == uri }?.let { it.isReadPermission && it.isWritePermission }
                    ?: false

                if (flags) {
                    onPermissionGranted()
                    return@LaunchedEffect
                }
            } catch (e: Exception) {
                Log.e("StoragePermission", "Error checking URI permissions", e)
            }
        }
    }
}