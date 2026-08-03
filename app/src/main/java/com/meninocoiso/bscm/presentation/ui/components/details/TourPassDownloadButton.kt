package com.meninocoiso.bscm.presentation.ui.components.details

import android.util.Log
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.domain.model.TourPass
import com.meninocoiso.bscm.domain.state.DownloadState
import com.meninocoiso.bscm.presentation.ui.components.dialog.StoragePermissionDialog
import com.meninocoiso.bscm.presentation.viewmodel.ContentViewModel
import com.meninocoiso.bscm.util.StorageUtils
import com.meninocoiso.bscm.util.StorageUtils.BEATSTAR_URI
import kotlinx.coroutines.launch

@Composable
fun TourPassDownloadButton(
    tourPass: TourPass,
    downloadState: DownloadState,
    contentViewModel: ContentViewModel,
) {
    var showStoragePermissionDialog by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    fun startDownload() {
        // Check for storage permission
        val hasStoragePermission = StorageUtils.checkStoragePermission(
            context = context,
            folderUri = BEATSTAR_URI
        )

        if (!hasStoragePermission) {
            showStoragePermissionDialog = true
            return
        }

        contentViewModel.downloadTourPass(tourPass = tourPass)
    }

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
        enabled = downloadState is DownloadState.Idle ||
                downloadState is DownloadState.Error,
        onClick = {
            coroutineScope.launch {
                startDownload()
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
                    contentDescription = stringResource(R.string.download_tour_pass)
                )

                is DownloadState.Downloading, is DownloadState.Extracting -> CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )

                is DownloadState.Installed -> Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = stringResource(R.string.download_complete)
                )

                is DownloadState.Error -> Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = stringResource(R.string.download_failed)
                )
            }
            Text(
                text = when (downloadState) {
                    is DownloadState.Idle -> stringResource(R.string.download)
                    is DownloadState.Downloading -> stringResource(R.string.downloading)
                    is DownloadState.Extracting -> stringResource(R.string.extracting)
                    is DownloadState.Installed -> stringResource(R.string.installed)
                    is DownloadState.Error -> stringResource(R.string.try_again)
                }
            )
        }
    }

    // Show storage permission dialog if needed
    if (showStoragePermissionDialog) {
        StoragePermissionDialog(
            onPermissionGranted = {
                Log.d("TourPassDownloadButton", "Storage permission granted")

                showStoragePermissionDialog = false

                // Start download immediately after permission is granted
                contentViewModel.downloadTourPass(tourPass = tourPass)

                Log.d("TourPassDownloadButton", "Starting download after permission granted")
            },
            onDismiss = {
                showStoragePermissionDialog = false
            }
        )
    }
}
