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
import androidx.compose.ui.platform.LocalResources
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
import com.meninocoiso.bscm.presentation.ui.components.details.CollectionCreateBottomSheet
import com.meninocoiso.bscm.presentation.ui.components.details.DownloadButton
import com.meninocoiso.bscm.presentation.ui.components.details.InteractionButton
import com.meninocoiso.bscm.presentation.ui.components.details.StatListItem
import com.meninocoiso.bscm.presentation.ui.components.dialog.ConfirmationDialog
import com.meninocoiso.bscm.presentation.ui.components.dialog.ListenTrackDialog
import com.meninocoiso.bscm.presentation.ui.components.dialog.ReportDialog
import com.meninocoiso.bscm.presentation.ui.components.layout.Section
import com.meninocoiso.bscm.presentation.ui.components.layout.SwipeableSnackbarHost
import com.meninocoiso.bscm.presentation.ui.components.preview.PreviewContributors
import com.meninocoiso.bscm.presentation.ui.utils.showReplacingSnackbar
import com.meninocoiso.bscm.presentation.viewmodel.AuthViewModel
import com.meninocoiso.bscm.presentation.viewmodel.CollectionViewModel
import com.meninocoiso.bscm.presentation.viewmodel.ContentViewModel
import com.meninocoiso.bscm.presentation.viewmodel.InteractionViewModel
import com.meninocoiso.bscm.util.LinkingUtils
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

private enum class ChartDialog { None, Report, DeleteConfirmation, ListenTrack }

/**
 * Details screen for one chart with download controls, reactions, and collection management.
 */
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
    val resources = LocalResources.current

    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

    // -------------------------------------------------------------------------
    // State collection
    // -------------------------------------------------------------------------
    // Merge locally persisted interaction state (like/bookmark/install) into the
    // chart passed by the navigator, which for tour pass charts has no state.
    val effectiveChart by contentViewModel.observeChartState(chart)
        .collectAsStateWithLifecycle()
    val chartState by contentViewModel.getDownloadState(effectiveChart.id)
        .collectAsStateWithLifecycle()
    val isLoggedIn by authViewModel.isLoggedInFlow
        .collectAsStateWithLifecycle(false)
    val savedCollections by interactionViewModel
        .getContentCollections(chart.id)
        .collectAsStateWithLifecycle()
    val isGameplayVideoPreviewEnabled by contentViewModel.isGameplayVideoPreviewEnabled
        .collectAsStateWithLifecycle()

    // -------------------------------------------------------------------------
    // Optimistic UI state (lives in screen: it's purely visual feedback)
    // -------------------------------------------------------------------------
    var optimisticLiked by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var optimisticBookmarked by rememberSaveable { mutableStateOf<Boolean?>(null) }

    val hasLiveBookmarkMembership = savedCollections.any { it.kind == CollectionKind.BOOKMARKS }
    val selectedUserCollectionIds = savedCollections
        .filter { it.kind == CollectionKind.USER }
        .map { it.id }
        .toSet()
    val hasPersistedBookmark = hasLiveBookmarkMembership || effectiveChart.bookmarkedAt != null

    val isLiked = optimisticLiked ?: (effectiveChart.likedAt != null)
    val isBookmarked = optimisticBookmarked ?: hasPersistedBookmark

    // Shared toggle handler used by toolbar action and bottom-sheet auto-bookmark item.
    val toggleBookmarkSelection: (Boolean) -> Unit = { shouldBeBookmarked ->
        optimisticBookmarked = shouldBeBookmarked
        interactionViewModel.enqueueBookmarkMutation(chart.id, chart.id, shouldBeBookmarked)
        if (!shouldBeBookmarked) {
            snackbarHostState.currentSnackbarData?.dismiss()
        }
    }

    // Clear optimistic state once persistence catches up
    LaunchedEffect(effectiveChart.likedAt) {
        if (optimisticLiked != null && effectiveChart.likedAt != null) optimisticLiked = null
    }
    LaunchedEffect(savedCollections, effectiveChart.bookmarkedAt) {
        // Reset optimistic bookmark when local/remote persistence reflects final intent.
        optimisticBookmarked?.let { optimistic ->
            val confirmed = if (optimistic) hasPersistedBookmark else !hasPersistedBookmark
            if (confirmed) optimisticBookmarked = null
        }
    }

    // -------------------------------------------------------------------------
    // Collection sheet state
    // -------------------------------------------------------------------------
    val collectionSheetState = rememberModalBottomSheetState()
    var showCollectionSheet by rememberSaveable { mutableStateOf(false) }
    val collectionUiState by collectionViewModel.uiState.collectAsStateWithLifecycle()
    val userCollections = collectionUiState.userCollections.items
    val isCollectionsLoading = collectionUiState.userCollections.state is ContentState.Loading
    val hasError = collectionUiState.userCollections.state is ContentState.Error
    val errorMessage = if (hasError) stringResource(R.string.failed_to_load_collections) else null

    LaunchedEffect(showCollectionSheet) {
        if (showCollectionSheet) collectionViewModel.fetchUserCollections(reset = true)
    }

    // -------------------------------------------------------------------------
    // Strings (must be read in composition)
    // -------------------------------------------------------------------------
    val downloadCompleteMsg = stringResource(R.string.download_complete)
    val errorTitleMsg = stringResource(R.string.error)
    val chartDeletedMsg = stringResource(R.string.chart_deleted)
    val failedToDeleteMsg = stringResource(R.string.failed_to_delete_chart)
    val connectToManageFavoritesMsg = stringResource(R.string.connect_to_manage_favorites)
    val addedToFavoritesMsg = stringResource(R.string.added_to_favorites)
    val manageMsg = stringResource(R.string.manage)
    val connectToManageLikesMsg = stringResource(R.string.connect_to_manage_likes)
    val notesAmountText = pluralStringResource(R.plurals.notes_amount, chart.notesAmount, chart.notesAmount)
    val effectsAmountText = pluralStringResource(R.plurals.effects_amount, chart.effectsAmount, chart.effectsAmount)
    val downloadsAmountText = pluralStringResource(R.plurals.downloads_amount, chart.downloadsSum, chart.downloadsSum)
    val savedToCollectionMsg = { name: String -> resources.getString(R.string.saved_to_collection, name) }
    val errorCreatingCollectionMsg = { msg: String -> resources.getString(R.string.error_creating_collection, msg) }
    val collectionNameExistsMsg = stringResource(R.string.collection_name_exists)
    val connectLabel = stringResource(R.string.connect)
    val lastUpdated = chart.latestVersion?.let { StringUtils.toRelativeString(it.createdAt) } ?: ""

    // -------------------------------------------------------------------------
    // Download events
    // -------------------------------------------------------------------------
    LaunchedEffect(Unit) {
        contentViewModel.events.collect { event ->
            if (event.id != chart.id) return@collect
            when (event) {
                is DownloadEvent.Complete -> snackbarHostState.showReplacingSnackbar(downloadCompleteMsg)
                is DownloadEvent.Error -> snackbarHostState.showReplacingSnackbar("$errorTitleMsg: ${event.message}")
                else -> {}
            }
        }
    }

    // Check the install state whenever the locally merged chart arrives: the
    // chart passed by the navigator (e.g. from a tour pass) has no install
    // flag, and observeChartState resolves it from the local database.
    LaunchedEffect(effectiveChart.id, effectiveChart.isInstalled) {
        contentViewModel.checkStatus(effectiveChart)
    }

    // -------------------------------------------------------------------------
    // Dialog state
    // -------------------------------------------------------------------------
    var currentDialog by rememberSaveable { mutableStateOf(ChartDialog.None) }

    when (currentDialog) {
        ChartDialog.DeleteConfirmation -> ConfirmationDialog(
            title = stringResource(R.string.delete_chart),
            message = stringResource(R.string.delete_chart_description),
            onDismiss = { currentDialog = ChartDialog.None },
            onConfirm = {
                currentDialog = ChartDialog.None
                contentViewModel.deleteChart(
                    chart,
                    onSuccess = { scope.launch { snackbarHostState.showReplacingSnackbar(chartDeletedMsg) } },
                    onError = { scope.launch { snackbarHostState.showReplacingSnackbar(failedToDeleteMsg) } }
                )
            }
        )
        ChartDialog.Report -> ReportDialog(
            onSubmit = {},
            onDismiss = { currentDialog = ChartDialog.None }
        )
        ChartDialog.ListenTrack -> ListenTrackDialog(
                streamingLinks = chart.track.streamingRefs,
            onDismiss = { currentDialog = ChartDialog.None }
        )
        ChartDialog.None -> {}
    }

    Scaffold(
        snackbarHost = { SwipeableSnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                modifier = Modifier.padding(horizontal = 8.dp),
                navigationIcon = {
                    IconButton(
                        modifier = Modifier.padding(end = 12.dp),
                        onClick = onReturn
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            tint = MaterialTheme.colorScheme.onSurface,
                            contentDescription = stringResource(R.string.return_screen)
                        )
                    }
                },
                actions = {
                    DropdownMenuUI { dismiss ->
                        DropdownMenuItem(
                            contentPadding = DropdownItemPadding,
                            text = { Text(stringResource(R.string.share)) },
                            leadingIcon = { Icon(Icons.Outlined.Share, contentDescription = null) },
                            onClick = {
                                dismiss()
                                LinkingUtils.shareChart(context, chart.id)
                            }
                        )
                        DropdownMenuItem(
                            contentPadding = DropdownItemPadding,
                            text = { Text(stringResource(R.string.report)) },
                            leadingIcon = {
                                Icon(painterResource(R.drawable.rounded_flag_24), contentDescription = null)
                            },
                            onClick = {
                                dismiss()
                                currentDialog = ChartDialog.Report
                            }
                        )
                        if (chartState == DownloadState.Installed(chart.id)) {
                            DropdownMenuItem(
                                contentPadding = DropdownItemPadding,
                                text = { Text(stringResource(R.string.delete_chart)) },
                                leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                                onClick = {
                                    dismiss()
                                    currentDialog = ChartDialog.DeleteConfirmation
                                }
                            )
                        }
                    }
                },
                title = {
                    Column {
                Text(chart.track.title, style = MaterialTheme.typography.titleLarge)
                Text(chart.track.artist, style = MaterialTheme.typography.titleMedium)
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(
                actions = {
                    if (chart.track.streamingRefs.isNotEmpty()) {
                        IconButton(onClick = { currentDialog = ChartDialog.ListenTrack }) {
                            Icon(
                                painter = painterResource(id = R.drawable.baseline_artist_24),
                                contentDescription = stringResource(R.string.listen_to_track),
                            )
                        }
                    }

                    InteractionButton(
                        R.drawable.baseline_bookmark_24,
                        R.drawable.rounded_bookmark_24,
                        isBookmarked,
                        !isLoggedIn,
                        onDisabled = {
                                scope.launch {
                                    val result = snackbarHostState.showReplacingSnackbar(
                                        message = connectToManageFavoritesMsg,
                                        actionLabel = connectLabel,
                                        duration = SnackbarDuration.Short
                                    )
                                    if (result == SnackbarResult.ActionPerformed) onNavigateToSettings()
                                }
                            },
                            beforeToggle = { current, next ->
                                // Unbookmark action is managed through the sheet to allow collection edits.
                                if (current && !next) {
                                    showCollectionSheet = true
                                    false
                                } else {
                                    true
                                }
                            },
                        ) { newValue ->
                            toggleBookmarkSelection(newValue)

                            if (newValue) {
                                scope.launch {
                                    val result = snackbarHostState.showReplacingSnackbar(
                                        message = addedToFavoritesMsg,
                                        actionLabel = manageMsg,
                                        duration = SnackbarDuration.Short
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        interactionViewModel.flushPendingBookmarkMutation(chart.id, chart.id)
                                        showCollectionSheet = true
                                    }
                                }
                            } else {
                                snackbarHostState.currentSnackbarData?.dismiss()
                            }
                        }

                        InteractionButton(
                            R.drawable.baseline_favorite_24,
                            R.drawable.rounded_favorite_24,
                            isLiked,
                            !isLoggedIn,
                            onDisabled = {
                                scope.launch {
                                    val result = snackbarHostState.showReplacingSnackbar(
                                        message = connectToManageLikesMsg,
                                        actionLabel = connectLabel,
                                        duration = SnackbarDuration.Short
                                    )
                                    if (result == SnackbarResult.ActionPerformed) onNavigateToSettings()
                                }
                            }
                        ) { newValue ->
                            optimisticLiked = newValue
                            interactionViewModel.enqueueLikeMutation(chart.id, chart.id, newValue)
                        }
                },
                floatingActionButton = {
                    DownloadButton(
                        chart = effectiveChart,
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
            MediaCarousel(
                listOf(
                    CarouselItem.ImageItem(imageUrl = chart.track.coverUrl ?: ""),
                    CarouselItem.VideoItem(videoId = chart.previewVideoId)
                ),
                isVideoEnabled = isGameplayVideoPreviewEnabled
            )

            if (chart.contributors.isNotEmpty()) {
                PreviewContributors(chart.contributors)
            }

            Section(title = stringResource(R.string.stats)) {
                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    if (chart.notesAmount > 0) {
                        StatListItem(title = notesAmountText, icon = R.drawable.rounded_music_note_24)
                    }
                    if (chart.effectsAmount > 0) {
                        StatListItem(title = effectsAmountText, icon = R.drawable.rounded_blur_medium_24)
                    }
                    if (chart.downloadsSum > 0) {
                        StatListItem(title = downloadsAmountText, icon = R.drawable.rounded_download_24)
                    }
                    StatListItem(
                        title = stringResource(R.string.updated_at, lastUpdated),
                        icon = R.drawable.rounded_calendar_today_24
                    )
                }
            }

        }

        Box(
            modifier = Modifier
                .padding(bottom = innerPadding.calculateBottomPadding())
                .fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            if (chartState is DownloadState.Downloading || chartState is DownloadState.Extracting) {
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
        CollectionCreateBottomSheet(
            sheetState = collectionSheetState,
            onDismissRequest = { showCollectionSheet = false },
            onClose = {
                scope.launch { collectionSheetState.hide() }.invokeOnCompletion {
                    if (!collectionSheetState.isVisible) showCollectionSheet = false
                }
            },
            collections = userCollections,
            checkedCollectionIds = selectedUserCollectionIds,
            isBookmarked = isBookmarked,
            isLoading = isCollectionsLoading,
            isMutating = collectionUiState.isCreating,
            errorMessage = errorMessage,
            onAutoBookmarksToggle = { shouldBeBookmarked ->
                toggleBookmarkSelection(shouldBeBookmarked)
            },
            onCollectionToggled = { collectionId, collectionName, shouldBeSelected ->
                if (shouldBeSelected) {
                    interactionViewModel.addToCollection(chart.id, chart.id, collectionId)
                    scope.launch {
                        snackbarHostState.showReplacingSnackbar(
                            savedToCollectionMsg(collectionName),
                            duration = SnackbarDuration.Short
                        )
                    }
                } else {
                    interactionViewModel.removeFromCollection(chart.id, chart.id, collectionId)
                }
            },
            onCreateCollection = { name, isPublic ->
                scope.launch {
                    try {
                        val newCollectionId = collectionViewModel.createCollection(name, isPublic)
                        interactionViewModel.addToCollection(chart.id, chart.id, newCollectionId)
                        snackbarHostState.showReplacingSnackbar(savedToCollectionMsg(name), duration = SnackbarDuration.Short)
                    } catch (e: ApiException) {
                        Log.e("ChartDetailsScreen", "Error creating collection", e)
                        if (e.status == HttpStatusCode.BadRequest) {
                            snackbarHostState.showReplacingSnackbar(collectionNameExistsMsg)
                        } else {
                            snackbarHostState.showReplacingSnackbar(errorCreatingCollectionMsg(e.message))
                        }
                    }
                }
            }
        )
    }
}

typealias OnNavigateToDetails = (CatalogItem) -> Unit?