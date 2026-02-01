package com.meninocoiso.bscm.presentation.screen.details

import DownloadEvent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.domain.model.Chart
import com.meninocoiso.bscm.presentation.ui.components.CarouselItem
import com.meninocoiso.bscm.presentation.ui.components.DropdownMenuUI
import com.meninocoiso.bscm.presentation.ui.components.MediaCarousel
import com.meninocoiso.bscm.presentation.ui.components.details.CollectionBottomSheet
import com.meninocoiso.bscm.presentation.ui.components.details.DownloadButton
import com.meninocoiso.bscm.presentation.ui.components.details.InteractionButton
import com.meninocoiso.bscm.presentation.ui.components.details.StatListItem
import com.meninocoiso.bscm.presentation.ui.components.dialog.ConfirmationDialog
import com.meninocoiso.bscm.presentation.ui.components.dialog.ListenTrackDialog
import com.meninocoiso.bscm.presentation.ui.components.dialog.ReportDialog
import com.meninocoiso.bscm.presentation.ui.components.layout.Section
import com.meninocoiso.bscm.presentation.ui.components.layout.SwipeableSnackbarHost
import com.meninocoiso.bscm.presentation.ui.components.preview.PreviewContributors
import com.meninocoiso.bscm.domain.state.DownloadState
import com.meninocoiso.bscm.presentation.viewmodel.ContentViewModel
import com.meninocoiso.bscm.presentation.viewmodel.CollectionViewModel
import com.meninocoiso.bscm.presentation.viewmodel.InteractionViewModel
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

// Dialog state management
private enum class ChartDialog { None, Report, DeleteConfirmation, ListenTrack }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartDetailsScreen(
    chart: Chart,
    onReturn: () -> Unit,
    contentViewModel: ContentViewModel = hiltViewModel(),
    interactionViewModel: InteractionViewModel = hiltViewModel(),
    collectionViewModel: CollectionViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Combine states to reduce recompositions
    val chartState by contentViewModel.getDownloadState(chart.id)
        .collectAsStateWithLifecycle()

    // UI State
    val isGameplayVideoPreviewEnabled = contentViewModel.isGameplayVideoPreviewEnabled
        .collectAsStateWithLifecycle(initialValue = true)

    // Simplified dialog state management
    var currentDialog by rememberSaveable { mutableStateOf(ChartDialog.None) }

    var isBookmarked by rememberSaveable { mutableStateOf(chart.isFavorited) }
    var isLiked by rememberSaveable { mutableStateOf(chart.isLiked) }

    LaunchedEffect(chart.id) {
        isBookmarked = chart.isFavorited
        isLiked = chart.isLiked
    }

    // Collection sheet state
    val collectionSheetState = rememberModalBottomSheetState()
    var showCollectionSheet by rememberSaveable {
        mutableStateOf(false)
    }

    val userCollections by collectionViewModel.collections.collectAsStateWithLifecycle()
    val collectionsState by collectionViewModel.collectionsContentState.collectAsStateWithLifecycle()

    LaunchedEffect(showCollectionSheet) {
        if (showCollectionSheet) {
            collectionViewModel.fetchUserCollections(reset = true)
        }
    }

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

                else -> { /* Other events don't need UI feedback */
                }
            }
        }
    }

    // Dialog management
    when (currentDialog) {
        ChartDialog.DeleteConfirmation -> {
            ConfirmationDialog(
                title = stringResource(R.string.delete_chart),
                message = stringResource(R.string.delete_chart_description),
                onDismiss = { currentDialog = ChartDialog.None },
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

        ChartDialog.Report -> {
            ReportDialog(
                onSubmit = {
                    // Implement report functionality
                },
                onDismiss = { currentDialog = ChartDialog.None },
            )
        }

        ChartDialog.ListenTrack -> {
            ListenTrackDialog(
                streamingLinks = chart.trackUrls,
                onDismiss = { currentDialog = ChartDialog.None }
            )
        }

        ChartDialog.None -> { /* No dialog shown */
        }
    }

    val lastUpdated = StringUtils.toRelativeString(chart.latestVersion.publishedAt)

    Scaffold(
        snackbarHost = { SwipeableSnackbarHost(snackbarHostState) },
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
                        if (chart.contentId != null) {
                            DropdownMenuItem(
                                contentPadding = DropdownItemPadding,
                                text = { Text(stringResource(R.string.share)) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Outlined.Share,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    shareChartLink(context, chart.contentId)
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
                                    currentDialog = ChartDialog.Report
                                }
                            )
                        }
                        if (chartState == DownloadState.Installed(chart.id)) {
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
                                    currentDialog = ChartDialog.DeleteConfirmation
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
                    if (chart.trackUrls.isNotEmpty()) {
                        IconButton(onClick = { currentDialog = ChartDialog.ListenTrack }) {
                            Icon(
                                painter = painterResource(id = R.drawable.baseline_artist_24),
                                contentDescription = stringResource(R.string.listen_to_track),
                            )
                        }
                    }

                    if (chart.contentId != null) {
                        InteractionButton(
                            R.drawable.baseline_bookmark_24,
                            R.drawable.rounded_bookmark_24,
                            isBookmarked
                        ) { newValue ->
                            isBookmarked = newValue
                            if (newValue) {
                                interactionViewModel.bookmarkContent(chart.contentId)
                                scope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        "Adicionado aos bookmarks",
                                        "Mover",
                                        duration = SnackbarDuration.Short
                                    )

                                    when (result) {
                                        SnackbarResult.ActionPerformed -> {
                                            showCollectionSheet = true
                                        }

                                        SnackbarResult.Dismissed -> Unit
                                    }
                                }
                            } else {
                                interactionViewModel.unbookmarkContent(chart.contentId)
                                snackbarHostState.currentSnackbarData?.dismiss()
                            }
                        }
                        InteractionButton(
                            R.drawable.baseline_favorite_24,
                            R.drawable.rounded_favorite_24,
                            isLiked
                        ) { newValue ->
                            isLiked = newValue
                            if (newValue) {
                                interactionViewModel.likeContent(chart.contentId)
                            } else {
                                interactionViewModel.unlikeContent(chart.contentId)
                            }
                        }
                    }
                },
                floatingActionButton = {
                    DownloadButton(
                        chart = chart,
                        downloadState = chartState,
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
            if (chart.contributors.isNotEmpty()) {
                PreviewContributors(chart.contributors)
            }

            // Stats
            Section(title = stringResource(R.string.stats)) {
                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    if (chart.latestVersion.duration > 0) {
                        StatListItem(
                            title = "~${StringUtils.toDurationString(chart.latestVersion.duration)}",
                            icon = R.drawable.outline_access_time_24
                        )
                    }
                    if (chart.latestVersion.notesAmount > 0) {
                        StatListItem(
                            title = pluralStringResource(
                                R.plurals.notes_amount,
                                chart.latestVersion.notesAmount,
                                chart.latestVersion.notesAmount
                            ),
                            icon = R.drawable.rounded_music_note_24
                        )
                    }
                    if (chart.latestVersion.effectsAmount > 0) {
                        StatListItem(
                            title = pluralStringResource(
                                R.plurals.effects_amount,
                                chart.latestVersion.effectsAmount,
                                chart.latestVersion.effectsAmount
                            ),
                            icon = R.drawable.rounded_blur_medium_24
                        )
                    }
                    if (chart.downloadsSum > 0) {
                        StatListItem(
                            title = pluralStringResource(
                                R.plurals.downloads_amount,
                                chart.downloadsSum,
                                chart.downloadsSum
                            ),
                            icon = R.drawable.rounded_download_24
                        )
                    }
                    if (chart.contentId != null) {
                        StatListItem(
                            title = stringResource(R.string.updated_at, lastUpdated),
                            icon = R.drawable.rounded_calendar_today_24
                        )
                    }
                }
            }

            // Known Issues
            if (chart.contentId != null) {
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
        }

        Box(
            modifier = Modifier
                .padding(
                    bottom = innerPadding.calculateBottomPadding()
                )
                .fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            if (chartState is DownloadState.Downloading ||
                chartState is DownloadState.Extracting
            ) {
                LinearProgressIndicator(
                    progress = {
                        when (val state = chartState) {
                            is DownloadState.Downloading -> state.progress
                            is DownloadState.Extracting -> state.progress
                            else -> 100f
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    if (showCollectionSheet) {
        CollectionBottomSheet(
            sheetState = collectionSheetState,
            onDismissRequest = { showCollectionSheet = false },
            onClose = {
                scope.launch { collectionSheetState.hide() }.invokeOnCompletion {
                    if (!collectionSheetState.isVisible) {
                        showCollectionSheet = false
                    }
                }
            },
            collections = userCollections,
            isLoading = collectionsState is com.meninocoiso.bscm.domain.result.ContentState.Loading,
            onCollectionSelected = { collectionId ->
                chart.contentId?.let { contentId ->
                    interactionViewModel.addToCollection(contentId, collectionId)
                }
                showCollectionSheet = false
            },
            onCreateCollection = { name, isPublic ->
                collectionViewModel.createCollection(name, isPublic)
            }
        )
    }
}

typealias OnNavigateToDetails = (chart: Chart) -> Unit