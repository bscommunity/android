package com.meninocoiso.beatstarcommunity.presentation.ui.components.download

import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.meninocoiso.beatstarcommunity.R
import com.meninocoiso.beatstarcommunity.domain.model.Chart
import com.meninocoiso.beatstarcommunity.presentation.viewmodel.ChartDetailsViewModel
import com.meninocoiso.beatstarcommunity.service.DownloadService
import kotlinx.coroutines.launch

private const val PLACEHOLDER_DOWNLOAD_URL = "https://cdn.discordapp.com/attachments/954166390619783268/1343421514770415727/HOT_TO_GO.zip?ex=67c86b08&is=67c71988&hm=74388cc07990dcb67e9efabecfee62b0149a70efe3f3619031712a20b6e4b45e&"

@Composable
fun DownloadButton(
    chart: Chart,
    snackbarHostState: SnackbarHostState,
    viewModel: ChartDetailsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showStoragePermissionDialog by remember { mutableStateOf(false) }
    var hasStoragePermission by remember { mutableStateOf(false) }

    // Check for storage permission
    StoragePermissionHandler(
        onPermissionGranted = { hasStoragePermission = true },
        viewModel = viewModel
    )

    ExtendedFloatingActionButton(
        text = { Text("Download") },
        icon = {
            Icon(
                painter = painterResource(id = R.drawable.rounded_download_24),
                contentDescription = "Download chart"
            )
        },
        containerColor = BottomAppBarDefaults.bottomAppBarFabColor,
        elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation(),
        onClick = {
            if (hasStoragePermission) {
                // Start download service
                DownloadService.startDownload(
                    context = context,
                    chartUrl = PLACEHOLDER_DOWNLOAD_URL,
                    chartName = "${chart.artist} - ${chart.track}"
                )

                scope.launch {
                    snackbarHostState.showSnackbar("Downloading chart...")
                }
            } else {
                // Show storage permission dialog
                showStoragePermissionDialog = true
            }
        }
    )

    // Show storage permission dialog if needed
    if (showStoragePermissionDialog) {
        StoragePermissionDialog(
            viewModel = viewModel,
            onPermissionGranted = {
                hasStoragePermission = true
                showStoragePermissionDialog = false

                // Start download immediately after permission is granted
                DownloadService.startDownload(
                    context = context,
                    chartUrl = PLACEHOLDER_DOWNLOAD_URL,
                    chartName = "${chart.artist} - ${chart.track}"
                )

                scope.launch {
                    snackbarHostState.showSnackbar("Downloading chart...")
                }
            },
            onDismiss = {
                showStoragePermissionDialog = false
            }
        )
    }
}