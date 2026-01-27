package com.meninocoiso.bscm.presentation.screen.updates.sections

import DownloadEvent
import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.presentation.navigation.OnSnackbar
import com.meninocoiso.bscm.presentation.navigation.show
import com.meninocoiso.bscm.presentation.screen.details.OnNavigateToDetails
import com.meninocoiso.bscm.presentation.ui.components.StatusMessageUI
import com.meninocoiso.bscm.presentation.ui.components.dialog.ConfirmationDialog
import com.meninocoiso.bscm.presentation.ui.components.updates.localContentSection
import com.meninocoiso.bscm.presentation.ui.components.updates.remoteSection
import com.meninocoiso.bscm.presentation.ui.modifiers.fabScrollObserver
import com.meninocoiso.bscm.presentation.viewmodel.ContentViewModel
import com.meninocoiso.bscm.presentation.viewmodel.UpdatesViewModel
import com.meninocoiso.bscm.util.StorageUtils
import com.meninocoiso.bscm.util.StorageUtils.BEATSTAR_URI
import com.meninocoiso.bscm.util.StorageUtils.INITIAL_URL

@Composable
internal fun ContentSection(
    viewModel: UpdatesViewModel,
    onNavigateToDetails: OnNavigateToDetails,
    onSnackbar: OnSnackbar,
    onFabStateChange: (Boolean) -> Unit,
    nestedScrollConnection: NestedScrollConnection,
    contentViewModel: ContentViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    // TODO: We don't take into account revocation of permissions while the app is running
    var hasStoragePermission by remember {
        mutableStateOf(context.contentResolver.persistedUriPermissions.any {
            it.uri == BEATSTAR_URI
        })
    }

    val folderPickerLauncher = StorageUtils.folderPickerLauncher(
        context,
        validate = { uri -> uri == BEATSTAR_URI },
        onPermissionGranted = {
            Log.d("WorkshopSection", "Storage permission granted for Beatstar folder")
            hasStoragePermission = true
            // Rescan local charts after permission is granted
            viewModel.scanLocalCharts()
        },
        onInvalidSelection = { onSnackbar.show(context.getString(R.string.incorrect_storage_permission)) }
    )

    // Collect the direct flows as states
    val pendingUpdateCharts by viewModel.pendingUpdateCharts.collectAsStateWithLifecycle(initialValue = emptyList())
    val installedCharts by viewModel.installedCharts.collectAsStateWithLifecycle(initialValue = emptyList())
    val duplicateInstalledIds by viewModel.duplicateInstalledIds.collectAsStateWithLifecycle(initialValue = emptySet())

    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    val cacheState by viewModel.cacheState.collectAsStateWithLifecycle()

    val itemsUpdating = remember { mutableStateListOf<String>() }
    val showLocalItemDialog = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        contentViewModel.events.collect { event ->
            when (event) {
                is DownloadEvent.Complete -> {
                    itemsUpdating.remove(event.id)
                    onSnackbar.show(context.getString(R.string.update_complete))
                }

                is DownloadEvent.Error -> onSnackbar.show(
                    context.getString(R.string.error, event.message)
                )

                else -> {}
            }
        }
    }

    if (!hasStoragePermission) {
        Column() {
            StatusMessageUI(
                title = stringResource(R.string.storage_permission_required),
                message = stringResource(R.string.storage_permission_required_description),
                icon = R.drawable.rounded_folder_limited_24,
                buttonLabel = stringResource(R.string.select_folder),
                onClick = {
                    folderPickerLauncher.launch(INITIAL_URL)
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 36.dp)
            )
        }
    } else if (pendingUpdateCharts.isNotEmpty() || installedCharts.isNotEmpty()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .fabScrollObserver { shouldExtend ->
                    // Update FAB state based on scroll delta
                    onFabStateChange(shouldExtend)
                },
        ) {
            if (installedCharts.isNotEmpty()) {
                remoteSection(
                    state = updateState,
                    charts = pendingUpdateCharts,
                    onFetchUpdates = { viewModel.checkForUpdates() },
                    itemsUpdating = itemsUpdating,
                    contentViewModel = contentViewModel,
                )
            }

            localContentSection(
                state = cacheState,
                charts = installedCharts,
                duplicateChartsIds = duplicateInstalledIds,
                onNavigateToDetails = onNavigateToDetails,
                onShowLocalItemDialog = { showLocalItemDialog.value = true },
            )
        }
    } else {
        StatusMessageUI(
            title = stringResource(R.string.empty_downloads),
            message = stringResource(R.string.empty_downloads_description),
            icon = R.drawable.rounded_box_24,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 36.dp)
        )
    }

    if (showLocalItemDialog.value) {
        ConfirmationDialog(
            onDismiss = { showLocalItemDialog.value = false },
            title = "Local item",
            message = "This chart was added manually and is not managed by the app.\nYou'll need to update or remove it manually."
        )
    }
}