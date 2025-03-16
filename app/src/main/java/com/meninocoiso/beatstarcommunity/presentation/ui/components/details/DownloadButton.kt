package com.meninocoiso.beatstarcommunity.presentation.ui.components.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.meninocoiso.beatstarcommunity.R
import com.meninocoiso.beatstarcommunity.domain.model.Chart
import com.meninocoiso.beatstarcommunity.presentation.ui.components.dialog.StoragePermissionDialog
import com.meninocoiso.beatstarcommunity.service.DownloadService
import com.meninocoiso.beatstarcommunity.util.DownloadState
import com.meninocoiso.beatstarcommunity.util.DownloadUtils
import com.meninocoiso.beatstarcommunity.util.PermissionUtils.Companion.StoragePermissionHandler
import kotlinx.coroutines.flow.collectLatest

@Composable
fun DownloadButton(
    chart: Chart,
    downloadState: MutableState<DownloadState>,
    downloadUtils: DownloadUtils,
    onSnackbar: suspend (message: String, actionLabel: String?) -> SnackbarResult
) {
    val context = LocalContext.current

    var showStoragePermissionDialog by remember { mutableStateOf(false) }
    var hasStoragePermission by remember { mutableStateOf(false) }

    fun startDownload(checkForPermission: Boolean) {
        if (checkForPermission && !hasStoragePermission) {
            showStoragePermissionDialog = true
            return
        }

        DownloadService.startDownload(
            context = context,
            chartId = chart.id,
            chartUrl = chart.latestVersion.chartUrl,
            chartName = "${chart.track} - ${chart.artist}"
        )
    }

    // Check for storage permission
    StoragePermissionHandler(
        onPermissionGranted = { hasStoragePermission = true },
        downloadUtils = downloadUtils
    )

    LaunchedEffect(Unit) {
        // Mark chart as installed if it is already installed
        if (chart.isInstalled == true) {
            downloadUtils.markAsInstalled()
        }

        // Observing download state from DownloadUtils
        downloadUtils.downloadState.collectLatest { state ->
            downloadState.value = state
            if (state is DownloadState.Completed) {
                onSnackbar("Download complete", null)
            } else if (state is DownloadState.Error) {
                // Show error message
                val result = onSnackbar(
                    (downloadState.value as DownloadState.Error).message,
                    "Try again",
                )
                when (result) {
                    SnackbarResult.ActionPerformed -> {
                        startDownload(true)
                    }

                    SnackbarResult.Dismissed -> {}
                }
            }
        }
    }

    Button(
        shape = FloatingActionButtonDefaults.extendedFabShape,
        colors = ButtonColors(
            containerColor = BottomAppBarDefaults.bottomAppBarFabColor,
            disabledContainerColor = when (downloadState.value) {
                is DownloadState.Error -> MaterialTheme.colorScheme.errorContainer
                else -> ButtonDefaults.buttonColors().disabledContainerColor
            },
            contentColor = contentColorFor(BottomAppBarDefaults.bottomAppBarFabColor),
            disabledContentColor = ButtonDefaults.buttonColors().disabledContentColor,
        ),
        modifier = Modifier
            .sizeIn(minWidth = 56.dp, minHeight = 56.dp),
        enabled = downloadState.value is DownloadState.Idle,
        onClick = {startDownload(true)}
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (downloadState.value) {
                is DownloadState.Idle -> Icon(
                    painter = painterResource(id = R.drawable.rounded_download_24),
                    contentDescription = "Download chart"
                )
                is DownloadState.Downloading, is DownloadState.Extracting -> CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
                is DownloadState.Completed, is DownloadState.Installed -> Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Download complete"
                )
                is DownloadState.Error -> Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Download failed"
                )
            }
            Text(
                text = when (downloadState.value) {
                    is DownloadState.Idle -> "Download"
                    is DownloadState.Downloading -> "Downloading..."
                    is DownloadState.Extracting -> "Extracting..."
                    is DownloadState.Completed, is DownloadState.Installed -> "Installed"
                    is DownloadState.Error -> "Failed"
                }
            )
        }
    }

    // Show storage permission dialog if needed
    if (showStoragePermissionDialog) {
        StoragePermissionDialog(
            downloadUtils = downloadUtils,
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