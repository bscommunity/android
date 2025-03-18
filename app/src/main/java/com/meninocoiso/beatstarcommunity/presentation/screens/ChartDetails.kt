package com.meninocoiso.beatstarcommunity.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meninocoiso.beatstarcommunity.R
import com.meninocoiso.beatstarcommunity.domain.model.Chart
import com.meninocoiso.beatstarcommunity.presentation.ui.components.chart.ChartContributors
import com.meninocoiso.beatstarcommunity.presentation.ui.components.details.DownloadButton
import com.meninocoiso.beatstarcommunity.presentation.ui.components.details.StatListItem
import com.meninocoiso.beatstarcommunity.presentation.ui.components.dialog.ConfirmationDialog
import com.meninocoiso.beatstarcommunity.presentation.ui.components.dialog.ListenTrackDialog
import com.meninocoiso.beatstarcommunity.presentation.ui.components.dialog.ReportDialog
import com.meninocoiso.beatstarcommunity.presentation.ui.components.layout.Section
import com.meninocoiso.beatstarcommunity.presentation.viewmodel.ContentState
import com.meninocoiso.beatstarcommunity.presentation.viewmodel.ContentViewModel
import com.meninocoiso.beatstarcommunity.service.DownloadEvent
import com.meninocoiso.beatstarcommunity.util.DateUtils
import com.meninocoiso.beatstarcommunity.util.LinkingUtils.Companion.shareChartLink
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data class ChartDetails(val chart: Chart)

@Serializable
data class DeepLinkChartDetails(val chartId: String)

private val DropdownItemPadding = PaddingValues(
    start = 16.dp,
    end = 24.dp,
    top = 8.dp,
    bottom = 8.dp
)

// Extracted helper class for dialog state management
private class DialogState {
    var showReportDialog by mutableStateOf(false)
    var showDeleteConfirmation by mutableStateOf(false)
    var showListenTrackDialog by mutableStateOf(false)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartDetailsScreen(
    chart: Chart,
    onReturn: () -> Unit,
    contentViewModel: ContentViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Combine states to reduce recompositions
    val chartState by contentViewModel.getContentState(chart.id)
        .collectAsStateWithLifecycle()

    // UI State
    var isMoreOptionsExpanded by remember { mutableStateOf(false) }
    val dialogs = remember { DialogState() }

    // Check installation status only once
    LaunchedEffect(chart.id) {
        contentViewModel.checkInstallationStatus(chart)
    }

    // Manage download events
    LaunchedEffect(Unit) {
        contentViewModel.events.collect { event ->
            when (event) {
                is DownloadEvent.Complete ->
                    snackbarHostState.showSnackbar("Download complete")
                is DownloadEvent.Error ->
                    snackbarHostState.showSnackbar("Error: ${event.message}")
                else -> { /* Other events don't need UI feedback */ }
            }
        }
    }

    if (dialogs.showDeleteConfirmation) {
        ConfirmationDialog(
            title = "Delete chart",
            message = "Are you sure you want to delete this chart?\nYou'll be able to download it again later.",
            onDismiss = { dialogs.showDeleteConfirmation = false },
            onConfirm = {
                contentViewModel.deleteChart(
                    chart,
                    onSuccess = {
                        scope.launch {
                            snackbarHostState.showSnackbar("Chart deleted")
                        }
                    },
                    onError = {
                        scope.launch {
                            snackbarHostState.showSnackbar("Failed to delete chart")
                        }
                    }
                )
            }
        )
    }

    if (dialogs.showReportDialog) {
        ReportDialog(
            onSubmit = {
                // Implement report functionality
            },
            onDismiss = { dialogs.showReportDialog = false }
        )
    }

    if (dialogs.showReportDialog) {
        ReportDialog(
            onSubmit = {
                // Implement report functionality
            },
            onDismiss = { dialogs.showReportDialog = false },
        )
    }

    if (dialogs.showListenTrackDialog) {
        ListenTrackDialog(
            streamingLinks = chart.trackUrls,
            onDismiss = { dialogs.showListenTrackDialog = false }
        )
    }

    // Dismiss snackbar on swipe
    val dismissSnackbarState = rememberSwipeToDismissBoxState(confirmValueChange = { value ->
        if (value != SwipeToDismissBoxValue.Settled) {
            snackbarHostState.currentSnackbarData?.dismiss()
            true
        } else {
            false
        }
    })

    // Reset dismiss state when snackbar is dismissed
    LaunchedEffect(dismissSnackbarState.currentValue) {
        if (dismissSnackbarState.currentValue != SwipeToDismissBoxValue.Settled) {
            dismissSnackbarState.reset()
        }
    }

    Scaffold(
        snackbarHost = {
            SwipeToDismissBox(
                state = dismissSnackbarState,
                backgroundContent = {},
                content = {
                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier.imePadding()
                    )
                },
            )
       },
        topBar = {
            TopAppBar(
                modifier = Modifier.padding(horizontal = 8.dp),
                navigationIcon = {
                    IconButton(
                        modifier = Modifier
                            .padding(end = 12.dp),
                        onClick = { onReturn() }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            tint = MaterialTheme.colorScheme.onSurface,
                            contentDescription = "Return"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { isMoreOptionsExpanded = !isMoreOptionsExpanded }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options menu"
                        )
                    }
                    DropdownMenu(
                        expanded = isMoreOptionsExpanded,
                        onDismissRequest = { isMoreOptionsExpanded = false },
                    ) {
                        DropdownMenuItem(
                            contentPadding = DropdownItemPadding,
                            text = { Text("Share") },
                            leadingIcon = { Icon(Icons.Outlined.Share, contentDescription = null) },
                            onClick = {
                                isMoreOptionsExpanded = false
                                shareChartLink(context, chart.id)
                            }
                        )
                        DropdownMenuItem(
                            contentPadding = DropdownItemPadding,
                            text = { Text("Report") },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.rounded_flag_24),
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                isMoreOptionsExpanded = false
                                dialogs.showReportDialog = true
                            }
                        )
                        if (chartState == ContentState.Installed(chart.id)) {
                            DropdownMenuItem(
                                contentPadding = DropdownItemPadding,
                                text = { Text("Delete chart") },
                                leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                                onClick = {
                                    isMoreOptionsExpanded = false
                                    dialogs.showDeleteConfirmation = true
                                }
                            )
                        }
                    }
                },
                title = {
                    Column {
                        Text(chart.track, style = MaterialTheme.typography.titleLarge)
                        Text(chart.artist, style = MaterialTheme.typography.titleMedium)
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(
                actions = {
                    IconButton(onClick = { dialogs.showListenTrackDialog = true }) {
                        Icon(
                            painter = painterResource(id = R.drawable.baseline_artist_24),
                            contentDescription = "Track link"
                        )
                    }
                    IconButton(onClick = {
                        scope.launch {
                            snackbarHostState.showSnackbar("Like feature not yet implemented")
                        }
                    }) {
                        Icon(
                            Icons.Default.FavoriteBorder,
                            contentDescription = "Like chart",
                        )
                    }
                },
                floatingActionButton = {
                    DownloadButton(
                        chart = chart,
                        contentState = chartState,
                        contentViewModel = contentViewModel,
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(innerPadding),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Media carousel
            // TestCarousel()

            // Credits
            ChartContributors(chart.contributors)

            // Stats
            Section(title = "Stats") {
                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    StatListItem(
                        title = "~${DateUtils.toDurationString(chart.latestVersion.duration)}",
                        icon = R.drawable.outline_access_time_24
                    )
                    StatListItem(
                        title = "${chart.latestVersion.notesAmount} notes",
                        icon = R.drawable.rounded_music_note_24
                    )
                    /*StatListItem(
                        title = "${chart.latestVersion.effectsAmount} effects",
                        icon = R.drawable.rounded_blur_medium_24
                    )*/
                    StatListItem(
                        title = "+${chart.latestVersion.notesAmount} downloads",
                        icon = R.drawable.rounded_download_24
                    )
                    StatListItem(
                        title = "Updated ${DateUtils.toRelativeString(chart.latestVersion.publishedAt)}",
                        icon = R.drawable.rounded_calendar_today_24
                    )
                }
            }

            // Known Issues
            Section(title = "Known Issues") {
                Box(modifier = Modifier.padding(16.dp)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp),
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (chart.latestVersion.knownIssues.isEmpty()) {
                            Text(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.CenterHorizontally),
                                text = "No known issues",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        } else {
                            chart.latestVersion.knownIssues.forEach {
                                Text(
                                    text = "•   $it",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .padding(
                    bottom = innerPadding.calculateBottomPadding()
                )
                .fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
        if (chartState is ContentState.Downloading ||
            chartState is ContentState.Extracting) {
                LinearProgressIndicator(
                    progress = {
                        when (val state = chartState) {
                            is ContentState.Downloading -> state.progress
                            is ContentState.Extracting -> state.progress
                            else -> 100f
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

typealias OnNavigateToDetails = (chart: Chart) -> Unit