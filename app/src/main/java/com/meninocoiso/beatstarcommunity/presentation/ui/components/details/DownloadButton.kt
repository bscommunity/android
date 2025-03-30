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
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.meninocoiso.beatstarcommunity.R
import com.meninocoiso.beatstarcommunity.domain.model.Chart
import com.meninocoiso.beatstarcommunity.presentation.ui.components.dialog.StoragePermissionDialog
import com.meninocoiso.beatstarcommunity.presentation.viewmodel.ContentState
import com.meninocoiso.beatstarcommunity.presentation.viewmodel.ContentViewModel
import com.meninocoiso.beatstarcommunity.util.PermissionUtils.Companion.StoragePermissionHandler

@Composable
fun DownloadButton(
    chart: Chart,
    contentState: ContentState,
    contentViewModel: ContentViewModel,
) {
    var showStoragePermissionDialog by remember { mutableStateOf(false) }
    var hasStoragePermission by remember { mutableStateOf(false) }

    fun startDownload(checkForPermission: Boolean) {
        if (checkForPermission && !hasStoragePermission) {
            showStoragePermissionDialog = true
            return
        }

        contentViewModel.downloadChart(chart = chart)
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
        enabled = contentState is ContentState.Idle ||
                contentState is ContentState.Error ||
                (contentState is ContentState.Installed && chart.availableVersion != null),
        onClick = { startDownload(true) }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (contentState) {
                is ContentState.Idle -> Icon(
                    painter = painterResource(id = R.drawable.rounded_download_24),
                    contentDescription = "Download chart"
                )
                is ContentState.Downloading, is ContentState.Extracting -> CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
                is ContentState.Installed -> {
                    if (chart.availableVersion != null) {
                        Icon(
                            painter = painterResource(id = R.drawable.rounded_download_24),
                            contentDescription = "Update chart"
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Download complete"
                        )
                    }
                }
                is ContentState.Error -> Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Download failed"
                )
            }
            Text(
                text = when (contentState) {
                    is ContentState.Idle -> "Download"
                    is ContentState.Downloading -> "Downloading..."
                    is ContentState.Extracting -> "Extracting..."
                    is ContentState.Installed -> {
                        if (chart.availableVersion != null) {
                            "Update"
                        } else {
                            "Installed"
                        }
                    }
                    is ContentState.Error -> "Try again"
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