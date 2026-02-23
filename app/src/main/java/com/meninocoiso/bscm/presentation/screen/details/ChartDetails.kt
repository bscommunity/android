package com.meninocoiso.bscm.presentation.screen.details

import DownloadEvent
import android.util.Log
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
import com.meninocoiso.bscm.data.remote.ApiException
import com.meninocoiso.bscm.domain.enums.CollectionKind
import com.meninocoiso.bscm.domain.model.CatalogItem
import com.meninocoiso.bscm.domain.model.Chart
import com.meninocoiso.bscm.domain.result.ContentState
import com.meninocoiso.bscm.domain.state.DownloadState
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
import com.meninocoiso.bscm.presentation.viewmodel.AuthViewModel
import com.meninocoiso.bscm.presentation.viewmodel.CollectionViewModel
import com.meninocoiso.bscm.presentation.viewmodel.ContentViewModel
import com.meninocoiso.bscm.presentation.viewmodel.InteractionViewModel
import com.meninocoiso.bscm.util.LinkingUtils.shareChartLink
import com.meninocoiso.bscm.util.StringUtils
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data class ChartDetails(val chart: Chart)

@Serializable
data class DeepLinkChartDetails(val contentId: String)

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
    onNavigateToSettings: () -> Unit,
    contentViewModel: ContentViewModel = hiltViewModel(),
    interactionViewModel: InteractionViewModel = hiltViewModel(),
    collectionViewModel: CollectionViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Use the chart parameter directly as the source of truth
    val currentChart = chart

    // Combine states to reduce recompositions
    val chartState by contentViewModel.getDownloadState(currentChart.id)
        .collectAsStateWithLifecycle()

    // UI State
    val isGameplayVideoPreviewEnabled = contentViewModel.isGameplayVideoPreviewEnabled
        .collectAsStateWithLifecycle(initialValue = true)

    // Simplified dialog state management
    var currentDialog by rememberSaveable { mutableStateOf(ChartDialog.None) }

    // Optimistic UI state - for instant feedback while local database is being updated
    var optimisticLiked by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var optimisticBookmarked by rememberSaveable { mutableStateOf<Boolean?>(null) }

    // Source-of-truth is the currentChart's local database fields (updated via observation)
    // Optimistic state takes priority for instant UI feedback, then falls back to chart's actual state
    val isLiked = optimisticLiked ?: (currentChart.likedAt != null)
    val isBookmarked = optimisticBookmarked ?: (currentChart.bookmarkedAt != null)

    // Clear optimistic state when currentChart updates with the persisted value
    LaunchedEffect(currentChart.likedAt) {
        if (optimisticLiked != null && currentChart.likedAt != null) {
            optimisticLiked = null
        }
    }

    LaunchedEffect(currentChart.bookmarkedAt) {
        if (optimisticBookmarked != null && currentChart.bookmarkedAt != null) {
            optimisticBookmarked = null
        }
    }
    // Prepare string resources for use in LaunchedEffect
    val downloadCompleteMsg = stringResource(R.string.download_complete)
    val errorTitleMsg = stringResource(R.string.error)
    val chartDeletedMsg = stringResource(R.string.chart_deleted)
    val failedToDeleteMsg = stringResource(R.string.failed_to_delete_chart)

    // Collection sheet state
    val collectionSheetState = rememberModalBottomSheetState()
    var showCollectionSheet by rememberSaveable {
        mutableStateOf(false)
    }

    val collectionUiState by collectionViewModel.uiState.collectAsStateWithLifecycle()
    val userCollections = collectionUiState.userCollections.items
    val isCollectionsLoading = collectionUiState.userCollections.state is ContentState.Loading

    val isLoggedIn by authViewModel.isLoggedInFlow.collectAsStateWithLifecycle(false)
    val contentCollection by interactionViewModel
        .getContentCollection(currentChart.contentId ?: "")
        .collectAsStateWithLifecycle()

    // Load the user's collections whenever the sheet opens
    LaunchedEffect(showCollectionSheet) {
        if (showCollectionSheet) {
            collectionViewModel.fetchUserCollections(reset = true)
        }
    }

    // Manage download events
    LaunchedEffect(Unit) {
        // Check status on first load
        contentViewModel.checkStatus(currentChart)

        contentViewModel.events.collect { event ->
            when (event) {
                is DownloadEvent.Complete ->
                    snackbarHostState.showSnackbar(downloadCompleteMsg)

                is DownloadEvent.Error ->
                    snackbarHostState.showSnackbar("$errorTitleMsg: ${event.message}")

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
                        currentChart,
                        onSuccess = {
                            scope.launch {
                                snackbarHostState.showSnackbar(chartDeletedMsg)
                            }
                        },
                        onError = {
                            scope.launch {
                                snackbarHostState.showSnackbar(failedToDeleteMsg)
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
                streamingLinks = currentChart.trackUrls,
                onDismiss = { currentDialog = ChartDialog.None }
            )
        }

        ChartDialog.None -> { /* No dialog shown */
        }
    }

    val lastUpdated = StringUtils.toRelativeString(currentChart.latestVersion.createdAt)

    val onUnauthenticated = { message: String ->
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss() // Dismiss any existing snackbar before showing a new one
            val result = snackbarHostState.showSnackbar(
                message,
                duration = SnackbarDuration.Short,
                actionLabel = "Connect"
            )

            if (result == SnackbarResult.ActionPerformed) {
                onNavigateToSettings()
            }
        }
    }

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
                        if (currentChart.contentId != null) {
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
                                    shareChartLink(context, currentChart.contentId)
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
                        if (chartState == DownloadState.Installed(currentChart.id)) {
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
                        Text(currentChart.track, style = MaterialTheme.typography.titleLarge)
                        Text(currentChart.artist, style = MaterialTheme.typography.titleMedium)
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(
                actions = {
                    if (currentChart.trackUrls.isNotEmpty()) {
                        IconButton(onClick = { currentDialog = ChartDialog.ListenTrack }) {
                            Icon(
                                painter = painterResource(id = R.drawable.baseline_artist_24),
                                contentDescription = stringResource(R.string.listen_to_track),
                            )
                        }
                    }

                    if (currentChart.contentId != null) {
                        InteractionButton(
                            R.drawable.baseline_bookmark_24,
                            R.drawable.rounded_bookmark_24,
                            isBookmarked,
                            !isLoggedIn,
                            onDisabled = { onUnauthenticated("Connect to manage favorites") }
                        ) { newValue ->
                            // Optimistic UI update
                            optimisticBookmarked = newValue

                            if (newValue) {
                                scope.launch {
                                    // Queue/send the bookmark interaction
                                    // This ensures it's registered locally even if app closes
                                    interactionViewModel.bookmarkContent(currentChart.id, currentChart.contentId)

                                    // Show snackbar after interaction is queued
                                    val result = snackbarHostState.showSnackbar(
                                        "Added to Favorites",
                                        "Manage",
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
                                // If unbookmarking, we need to check if it's in a user collection or just bookmarked
                                when (contentCollection?.kind) {
                                    CollectionKind.USER -> interactionViewModel.removeFromCollection(
                                        contentId = currentChart.contentId,
                                        collectionId = contentCollection!!.id
                                    )
                                    else -> interactionViewModel.unbookmarkContent(
                                        currentChart.id,
                                        currentChart.contentId
                                    )
                                }
                                snackbarHostState.currentSnackbarData?.dismiss()
                            }
                        }
                        InteractionButton(
                            R.drawable.baseline_favorite_24,
                            R.drawable.rounded_favorite_24,
                            isLiked,
                            !isLoggedIn,
                            onDisabled = { onUnauthenticated("Connect to manage likes") }
                        ) { newValue ->
                            // Optimistic UI update
                            optimisticLiked = newValue

                            if (newValue) {
                                interactionViewModel.likeContent(currentChart.id, currentChart.contentId)
                            } else {
                                interactionViewModel.unlikeContent(currentChart.id, currentChart.contentId)
                            }
                        }
                    }
                },
                floatingActionButton = {
                    DownloadButton(
                        chart = currentChart,
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
                        imageUrl = currentChart.coverUrl,
                    ),
                    CarouselItem.VideoItem(
                        videoId = currentChart.latestVersion.previewUrl
                    )
                ),
                isVideoEnabled = isGameplayVideoPreviewEnabled.value
            )

            // Credits
            if (currentChart.contributors.isNotEmpty()) {
                PreviewContributors(currentChart.contributors)
            }

            // Stats
            Section(title = stringResource(R.string.stats)) {
                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    if (currentChart.latestVersion.duration > 0) {
                        StatListItem(
                            title = "~${StringUtils.toDurationString(currentChart.latestVersion.duration)}",
                            icon = R.drawable.outline_access_time_24
                        )
                    }
                    if (currentChart.latestVersion.notesAmount > 0) {
                        StatListItem(
                            title = pluralStringResource(
                                R.plurals.notes_amount,
                                currentChart.latestVersion.notesAmount,
                                currentChart.latestVersion.notesAmount
                            ),
                            icon = R.drawable.rounded_music_note_24
                        )
                    }
                    if (currentChart.latestVersion.effectsAmount > 0) {
                        StatListItem(
                            title = pluralStringResource(
                                R.plurals.effects_amount,
                                currentChart.latestVersion.effectsAmount,
                                currentChart.latestVersion.effectsAmount
                            ),
                            icon = R.drawable.rounded_blur_medium_24
                        )
                    }
                    if (currentChart.downloadsSum > 0) {
                        StatListItem(
                            title = pluralStringResource(
                                R.plurals.downloads_amount,
                                currentChart.downloadsSum,
                                currentChart.downloadsSum
                            ),
                            icon = R.drawable.rounded_download_24
                        )
                    }
                    if (currentChart.contentId != null) {
                        StatListItem(
                            title = stringResource(R.string.updated_at, lastUpdated),
                            icon = R.drawable.rounded_calendar_today_24
                        )
                    }
                }
            }

            // Known Issues
            if (currentChart.contentId != null) {
                Section(title = stringResource(R.string.known_issues)) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp),
                            horizontalAlignment = Alignment.Start,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (currentChart.latestVersion.knownIssues.isEmpty()) {
                                Text(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.CenterHorizontally),
                                    text = stringResource(R.string.no_known_issues),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            } else {
                                currentChart.latestVersion.knownIssues.forEach {
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
            isLoading = isCollectionsLoading,
            onCollectionSelected = { collectionId ->
                scope.launch {
                    snackbarHostState.showSnackbar(
                        "Saved to collection!",
                        duration = SnackbarDuration.Short
                    )
                }
                currentChart.contentId?.let { contentId ->
                    // When user selects a collection from the bottom sheet after bookmarking,
                    // we need to remove the bookmark interaction and add to the custom collection
                    if (isBookmarked) {
                        interactionViewModel.changeContentCollection(
                            contentId = contentId,
                            targetCollectionId = collectionId,
                            targetCollectionKind = CollectionKind.USER
                        )
                    } else {
                        // If not bookmarked, just add to collection normally
                        interactionViewModel.addToCollection(contentId, collectionId)
                    }
                }
                showCollectionSheet = false
            },
            onCreateCollection = { name, isPublic ->
                try {
                    val newCollectionId = collectionViewModel.createCollection(name, isPublic)
                    Log.d("ChartDetailsScreen", "Created collection with ID: $newCollectionId")
                    currentChart.contentId?.let { contentId ->
                        interactionViewModel.addToCollection(contentId, newCollectionId)
                    }
                    scope.launch {
                        snackbarHostState.showSnackbar("Saved to \"$name\"!")
                    }
                } catch (e: ApiException) {
                    Log.e("ChartDetailsScreen", "Error creating collection", e)
                    scope.launch {
                        if (e.status == HttpStatusCode.BadRequest) {
                            snackbarHostState.showSnackbar("A collection with that name already exists. Please choose a different name.")
                        } else {
                            snackbarHostState.showSnackbar("Error creating collection: ${e.message}")
                        }
                    }
                }
            }
        )
    }
}

typealias OnNavigateToDetails = (CatalogItem) -> Unit?