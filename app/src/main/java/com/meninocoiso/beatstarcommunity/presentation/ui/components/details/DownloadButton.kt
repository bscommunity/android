package com.meninocoiso.beatstarcommunity.presentation.ui.components.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.meninocoiso.beatstarcommunity.R
import com.meninocoiso.beatstarcommunity.domain.model.Chart
import com.meninocoiso.beatstarcommunity.presentation.ui.components.dialog.StoragePermissionDialog
import com.meninocoiso.beatstarcommunity.presentation.viewmodel.ContentDownloadState
import com.meninocoiso.beatstarcommunity.presentation.viewmodel.ContentViewModel
import com.meninocoiso.beatstarcommunity.util.PermissionUtils.Companion.StoragePermissionHandler

@Composable
fun DownloadButton(
    chart: Chart,
    downloadState: State<ContentDownloadState?>,
    contentViewModel: ContentViewModel,
    onSnackbar: suspend (message: String, actionLabel: String?) -> SnackbarResult
) {
    val scope = rememberCoroutineScope()

    var showStoragePermissionDialog by remember { mutableStateOf(false) }
    var hasStoragePermission by remember { mutableStateOf(false) }

    fun startDownload(checkForPermission: Boolean) {
        if (checkForPermission && !hasStoragePermission) {
            showStoragePermissionDialog = true
            return
        }

        contentViewModel.downloadChart(
            chart = chart,
            /*onSuccess = {
                scope.launch {
                    onSnackbar("Download complete", null)
                }
            },
            onError = {
                scope.launch {
                    onSnackbar("Download failed: $it", null)
                }
            }*/
        )
    }

    // Check for storage permission
    StoragePermissionHandler(
        onPermissionGranted = { hasStoragePermission = true },
        getFolderUri = (contentViewModel::getFolderUri)
    )

    Button(
        shape = FloatingActionButtonDefaults.extendedFabShape,
        colors = ButtonColors(
            containerColor = BottomAppBarDefaults.bottomAppBarFabColor,
            contentColor = contentColorFor(BottomAppBarDefaults.bottomAppBarFabColor),
            disabledContainerColor = ButtonDefaults.buttonColors().disabledContainerColor,
            disabledContentColor = ButtonDefaults.buttonColors().disabledContentColor,
        ),
        modifier = Modifier
            .sizeIn(minWidth = 56.dp, minHeight = 56.dp),
        enabled = downloadState.value is ContentDownloadState.Idle ||
                downloadState.value is ContentDownloadState.Error,
        onClick = {startDownload(true)}
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (downloadState.value) {
                is ContentDownloadState.Idle -> Icon(
                    painter = painterResource(id = R.drawable.rounded_download_24),
                    contentDescription = "Download chart"
                )
                is ContentDownloadState.Downloading, is ContentDownloadState.Extracting -> CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
                is ContentDownloadState.Installed -> Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Download complete"
                )
                is ContentDownloadState.Error -> Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Download failed"
                )
                null -> { /* Do nothing */}
            }
            Text(
                text = when (downloadState.value) {
                    is ContentDownloadState.Idle -> "Download"
                    is ContentDownloadState.Downloading -> "Downloading..."
                    is ContentDownloadState.Extracting -> "Extracting..."
                    is ContentDownloadState.Installed -> "Installed"
                    is ContentDownloadState.Error -> "Try again"
                    null -> "Download"
                }
            )
        }
    }

    // Show storage permission dialog if needed
    if (showStoragePermissionDialog) {
        StoragePermissionDialog(
            setFolderUri = (contentViewModel::setFolderUri),
            onPermissionGranted = {
                hasStoragePermission = true
                showStoragePermissionDialog = false

                // Start download immediately after permission is granted
                startDownload(false)
            },
            onDismiss = {
                showStoragePermissionDialog = false
            }
        )
    }
}