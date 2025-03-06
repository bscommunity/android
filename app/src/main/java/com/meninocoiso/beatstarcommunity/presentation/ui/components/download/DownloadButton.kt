package com.meninocoiso.beatstarcommunity.presentation.ui.components.download

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
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
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
import com.meninocoiso.beatstarcommunity.service.DownloadService
import com.meninocoiso.beatstarcommunity.util.DownloadState
import com.meninocoiso.beatstarcommunity.util.DownloadUtils

private const val PLACEHOLDER_DOWNLOAD_URL = "https://cdn.discordapp.com/attachments/954166390619783268/1343421514770415727/HOT_TO_GO.zip?ex=67c86b08&is=67c71988&hm=74388cc07990dcb67e9efabecfee62b0149a70efe3f3619031712a20b6e4b45e&"

@Composable
fun DownloadButton(
    chart: Chart,
    downloadState: DownloadState,
    downloadUtils: DownloadUtils,
) {
    val context = LocalContext.current

    var showStoragePermissionDialog by remember { mutableStateOf(false) }
    var hasStoragePermission by remember { mutableStateOf(false) }

    // Check for storage permission
    StoragePermissionHandler(
        onPermissionGranted = { hasStoragePermission = true },
        downloadUtils = downloadUtils
    )

    Button(
        shape = FloatingActionButtonDefaults.extendedFabShape,
        colors = ButtonColors(
            containerColor = BottomAppBarDefaults.bottomAppBarFabColor,
            disabledContainerColor = when (downloadState) {
                is DownloadState.Error -> MaterialTheme.colorScheme.error
                else -> ButtonDefaults.buttonColors().disabledContainerColor
            },
            contentColor = contentColorFor(BottomAppBarDefaults.bottomAppBarFabColor),
            disabledContentColor = ButtonDefaults.buttonColors().disabledContentColor,
        ),
        modifier = Modifier
            .sizeIn(minWidth = 56.dp, minHeight = 56.dp),
        enabled = downloadState is DownloadState.Idle,
        onClick = {
            if (hasStoragePermission) {
                // Start download service
                DownloadService.startDownload(
                    context = context,
                    chartId = chart.id,
                    chartUrl = chart.latestVersion.chartUrl,
                    chartName = "${chart.artist} - ${chart.track}"
                )
            } else {
                // Show storage permission dialog
                showStoragePermissionDialog = true
            }
        }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (downloadState) {
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
                text = when (downloadState) {
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
                DownloadService.startDownload(
                    context = context,
                    chartId = chart.id,
                    chartUrl = chart.latestVersion.chartUrl,
                    chartName = "${chart.artist} - ${chart.track}"
                )
            },
            onDismiss = {
                showStoragePermissionDialog = false
            }
        )
    }
}