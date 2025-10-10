package com.meninocoiso.bscm.presentation.screens.details

import DownloadEvent
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
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.domain.model.Chart
import com.meninocoiso.bscm.presentation.ui.components.AnimatedIcon
import com.meninocoiso.bscm.presentation.ui.components.BurstDotsConfig
import com.meninocoiso.bscm.presentation.ui.components.BurstIconButton
import com.meninocoiso.bscm.presentation.ui.components.CarouselItem
import com.meninocoiso.bscm.presentation.ui.components.DropdownMenuUI
import com.meninocoiso.bscm.presentation.ui.components.MediaCarousel
import com.meninocoiso.bscm.presentation.ui.components.RingConfig
import com.meninocoiso.bscm.presentation.ui.components.chart.ChartContributors
import com.meninocoiso.bscm.presentation.ui.components.details.DownloadButton
import com.meninocoiso.bscm.presentation.ui.components.details.OfflineLikeButton
import com.meninocoiso.bscm.presentation.ui.components.details.StatListItem
import com.meninocoiso.bscm.presentation.ui.components.dialog.ConfirmationDialog
import com.meninocoiso.bscm.presentation.ui.components.dialog.ListenTrackDialog
import com.meninocoiso.bscm.presentation.ui.components.dialog.ReportDialog
import com.meninocoiso.bscm.presentation.ui.components.layout.Section
import com.meninocoiso.bscm.presentation.ui.components.layout.SwipeableSnackbarHost
import com.meninocoiso.bscm.presentation.ui.components.rememberBurstDotsModule
import com.meninocoiso.bscm.presentation.ui.components.rememberIconScaleModule
import com.meninocoiso.bscm.presentation.ui.components.rememberRingModule
import com.meninocoiso.bscm.presentation.viewmodel.ContentState
import com.meninocoiso.bscm.presentation.viewmodel.ContentViewModel
import com.meninocoiso.bscm.util.LinkingUtils.shareChartLink
import com.meninocoiso.bscm.util.StringUtils
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data class ChartDetails(val chart: Chart)

@Serializable
data class DeepLinkChartDetails(val chartId: String)

val DropdownItemPadding = PaddingValues(
    start = 16.dp,
    end = 24.dp,
    top = 8.dp,
    bottom = 8.dp
)

// Remove DialogState in favor of a single enum controlling which dialog is open
private enum class CurrentDialog { None, Report, DeleteConfirmation, ListenTrack }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartDetailsScreen(
    chart: Chart,
    onReturn: () -> Unit,
    contentViewModel: ContentViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Combine states to reduce recompositions
    val chartState by contentViewModel.getContentState(chart.id)
        .collectAsStateWithLifecycle()

    // UI State
    val isGameplayVideoPreviewEnabled = contentViewModel.isGameplayVideoPreviewEnabled
        .collectAsStateWithLifecycle(initialValue = true)

    // Single source of truth for dialogs, saved across config changes
    var currentDialog by rememberSaveable { mutableStateOf(CurrentDialog.None) }

    // Manage download events
    LaunchedEffect(Unit) {
        // Check status on first load
        contentViewModel.checkStatus(chart)

        contentViewModel.events.collect { event ->
            when (event) {
                is DownloadEvent.Complete ->
                    snackbarHostState.showSnackbar(context.getString(R.string.download_complete))

                is DownloadEvent.Error ->
                    snackbarHostState.showSnackbar(context.getString(R.string.error, event.message))

                else -> { /* Other events don't need UI feedback */ }
            }
        }
    }

    if (currentDialog == CurrentDialog.DeleteConfirmation) {
        ConfirmationDialog(
            title = stringResource(R.string.delete_chart),
            message = stringResource(R.string.delete_chart_description),
            onDismiss = { currentDialog = CurrentDialog.None },
            onConfirm = {
                contentViewModel.deleteChart(
                    chart,
                    onSuccess = {
                        scope.launch {
                            snackbarHostState.showSnackbar(context.getString(R.string.chart_deleted))
                        }
                    },
                    onError = {
                        scope.launch {
                            snackbarHostState.showSnackbar(context.getString(R.string.failed_to_delete_chart))
                        }
                    }
                )
            }
        )
    }

    if (currentDialog == CurrentDialog.Report) {
        ReportDialog(
            onSubmit = {
                // Implement report functionality
            },
            onDismiss = { currentDialog = CurrentDialog.None },
        )
    }

    if (currentDialog == CurrentDialog.ListenTrack) {
        ListenTrackDialog(
            streamingLinks = chart.trackUrls,
            onDismiss = { currentDialog = CurrentDialog.None }
        )
    }

    val lastUpdated = StringUtils.toRelativeString(chart.latestVersion.publishedAt)
    println("Chart last updated: $lastUpdated")

    var isFavorite by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = {
            SwipeableSnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.imePadding()
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
                            contentDescription = stringResource(R.string.return_screen)
                        )
                    }
                },
                actions = {
                    // Dropdown menu for more options
                    DropdownMenuUI {
                        DropdownMenuItem(
                            contentPadding = DropdownItemPadding,
                            text = { Text(stringResource(R.string.share)) },
                            leadingIcon = { Icon(Icons.Outlined.Share, contentDescription = null) },
                            onClick = {
                                shareChartLink(context, chart.id)
                            }
                        )
                        DropdownMenuItem(
                            contentPadding = DropdownItemPadding,
                            text = { Text(stringResource(R.string.report)) },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.rounded_flag_24),
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                currentDialog = CurrentDialog.Report
                            }
                        )
                        if (chartState == ContentState.Installed(chart.id)) {
                            DropdownMenuItem(
                                contentPadding = DropdownItemPadding,
                                text = { Text(stringResource(R.string.delete_chart)) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Outlined.Delete,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    currentDialog = CurrentDialog.DeleteConfirmation
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
                    IconButton(onClick = { currentDialog = CurrentDialog.ListenTrack }) {
                        Icon(
                            painter = painterResource(id = R.drawable.baseline_artist_24),
                            contentDescription = stringResource(R.string.listen_to_track),
                        )
                    }
                    
                    val (burstAnimation, burstVisual) = rememberBurstDotsModule(
                        config = BurstDotsConfig(color = MaterialTheme.colorScheme.primary)
                    )
                    val (ringAnimation, ringVisual) = rememberRingModule(
                        config = RingConfig(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    )
                    val (iconScale, iconScaleAnimation) = rememberIconScaleModule()

                    BurstIconButton(
                        isActive = isFavorite,
                        onClick = { isFavorite = !isFavorite },
                        animations = listOfNotNull(
                            burstAnimation,
                            ringAnimation,
                            iconScaleAnimation
                        ),
                        visuals = listOfNotNull(
                            burstVisual,
                            ringVisual
                        ),
                    ) {
                        AnimatedIcon(
                            isActive = isFavorite,
                            activeIconResId = R.drawable.baseline_bookmark_24,
                            inactiveIconResId = R.drawable.rounded_bookmark_24,
                            activeColor = MaterialTheme.colorScheme.primary,
                            inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            iconScale = iconScale
                        )
                    }

                    OfflineLikeButton(chart.id)
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
            MediaCarousel(
                listOf(
                    CarouselItem.ImageItem(
                        imageUrl = chart.coverUrl,
                    ),
                    CarouselItem.VideoItem(
                        videoId = chart.latestVersion.previewUrl
                    )
                ),
                isVideoEnabled = isGameplayVideoPreviewEnabled.value
            )

            // Credits
            ChartContributors(chart.contributors)

            // Stats
            Section(title = stringResource(R.string.stats)) {
                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    StatListItem(
                        title = "~${StringUtils.toDurationString(chart.latestVersion.duration)}",
                        icon = R.drawable.outline_access_time_24
                    )
                    StatListItem(
                        title = pluralStringResource(
                            R.plurals.notes_amount,
                            chart.latestVersion.notesAmount,
                            chart.latestVersion.notesAmount
                        ),
                        icon = R.drawable.rounded_music_note_24
                    )
                    /*StatListItem(
                        title = pluralStringResource(
                            R.plurals.effects_amount,
                            chart.latestVersion.effectsAmount,
                            chart.latestVersion.effectsAmount
                        ),
                        icon = R.drawable.rounded_blur_medium_24
                    )*/
                    StatListItem(
                        title = pluralStringResource(
                            R.plurals.downloads_amount,
                            chart.latestVersion.downloadsAmount,
                            chart.latestVersion.downloadsAmount
                        ),
                        icon = R.drawable.rounded_download_24
                    )
                    StatListItem(
                        title = stringResource(R.string.updated_at, lastUpdated),
                        icon = R.drawable.rounded_calendar_today_24
                    )
                }
            }

            // Known Issues
            Section(title = stringResource(R.string.known_issues)) {
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
                                text = stringResource(R.string.no_known_issues),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        } else {
                            chart.latestVersion.knownIssues.forEach {
                                Text(
                                    text = "•   ${it.description}",
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
                chartState is ContentState.Extracting
            ) {
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