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
    val chartState by contentViewModel.getDownloadState(chart.id)
        .collectAsStateWithLifecycle()
    val isLoggedIn by authViewModel.isLoggedInFlow
        .collectAsStateWithLifecycle(false)
    val savedCollections by interactionViewModel
        .getContentCollections(chart.contentId ?: "")
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
    val hasPersistedBookmark = hasLiveBookmarkMembership || chart.bookmarkedAt != null

    val isLiked = optimisticLiked ?: (chart.likedAt != null)
    val isBookmarked = optimisticBookmarked ?: hasPersistedBookmark

    // Shared toggle handler used by toolbar action and bottom-sheet auto-bookmark item.
    val toggleBookmarkSelection: (Boolean) -> Unit = { shouldBeBookmarked ->
        chart.contentId?.let { contentId ->
            optimisticBookmarked = shouldBeBookmarked
            interactionViewModel.enqueueBookmarkMutation(chart.id, contentId, shouldBeBookmarked)
            if (!shouldBeBookmarked) {
                snackbarHostState.currentSnackbarData?.dismiss()
            }
        }
    }

    // Clear optimistic state once persistence catches up
    LaunchedEffect(chart.likedAt) {
        if (optimisticLiked != null && chart.likedAt != null) optimisticLiked = null
    }
    LaunchedEffect(savedCollections, chart.bookmarkedAt) {
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
    val noKnownIssuesMsg = stringResource(R.string.no_known_issues)
    val notesAmountText = pluralStringResource(R.plurals.notes_amount, chart.latestVersion.notesAmount, chart.latestVersion.notesAmount)
    val effectsAmountText = pluralStringResource(R.plurals.effects_amount, chart.latestVersion.effectsAmount, chart.latestVersion.effectsAmount)
    val downloadsAmountText = pluralStringResource(R.plurals.downloads_amount, chart.downloadsSum, chart.downloadsSum)
    val savedToCollectionMsg = { name: String -> resources.getString(R.string.saved_to_collection, name) }
    val errorCreatingCollectionMsg = { msg: String -> resources.getString(R.string.error_creating_collection, msg) }
    val collectionNameExistsMsg = stringResource(R.string.collection_name_exists)
    val connectLabel = stringResource(R.string.connect)
    val lastUpdated = StringUtils.toRelativeString(chart.latestVersion.createdAt)

    // -------------------------------------------------------------------------
    // Download events
    // -------------------------------------------------------------------------
    LaunchedEffect(Unit) {
        contentViewModel.checkStatus(chart)
        contentViewModel.events.collect { event ->
            if (event.id != chart.id) return@collect
            when (event) {
                is DownloadEvent.Complete -> snackbarHostState.showSnackbar(downloadCompleteMsg)
                is DownloadEvent.Error -> snackbarHostState.showSnackbar("$errorTitleMsg: ${event.message}")
                else -> {}
            }
        }
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
                contentViewModel.deleteChart(
                    chart,
                    onSuccess = { scope.launch { snackbarHostState.showSnackbar(chartDeletedMsg) } },
                    onError = { scope.launch { snackbarHostState.showSnackbar(failedToDeleteMsg) } }
                )
            }
        )
        ChartDialog.Report -> ReportDialog(
            onSubmit = {},
            onDismiss = { currentDialog = ChartDialog.None }
        )
        ChartDialog.ListenTrack -> ListenTrackDialog(
            streamingLinks = chart.trackUrls,
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
                        if (chart.contentId != null) {
                            DropdownMenuItem(
                                contentPadding = DropdownItemPadding,
                                text = { Text(stringResource(R.string.share)) },
                                leadingIcon = { Icon(Icons.Outlined.Share, contentDescription = null) },
                                onClick = {
                                    dismiss()
                                    LinkingUtils.shareChart(context, chart.contentId)
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
                        }
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
                            isBookmarked,
                            !isLoggedIn,
                            onDisabled = {
                                scope.launch {
                                    snackbarHostState.currentSnackbarData?.dismiss()
                                    val result = snackbarHostState.showSnackbar(
                                        connectToManageFavoritesMsg,
                                        duration = SnackbarDuration.Short,
                                        actionLabel = connectLabel
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
                            val contentId = chart.contentId

                            toggleBookmarkSelection(newValue)

                            if (newValue) {
                                scope.launch {
                                    snackbarHostState.currentSnackbarData?.dismiss()
                                    val result = snackbarHostState.showSnackbar(
                                        addedToFavoritesMsg,
                                        manageMsg,
                                        duration = SnackbarDuration.Short
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        interactionViewModel.flushPendingBookmarkMutation(chart.id, contentId)
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
                                    snackbarHostState.currentSnackbarData?.dismiss()
                                    val result = snackbarHostState.showSnackbar(
                                        connectToManageLikesMsg,
                                        duration = SnackbarDuration.Short,
                                        actionLabel = connectLabel
                                    )
                                    if (result == SnackbarResult.ActionPerformed) onNavigateToSettings()
                                }
                            }
                        ) { newValue ->
                            optimisticLiked = newValue
                            interactionViewModel.enqueueLikeMutation(chart.id, chart.contentId, newValue)
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
            MediaCarousel(
                listOf(
                    CarouselItem.ImageItem(imageUrl = chart.coverUrl),
                    CarouselItem.VideoItem(videoId = chart.latestVersion.previewUrl)
                ),
                isVideoEnabled = isGameplayVideoPreviewEnabled
            )

            if (chart.contributors.isNotEmpty()) {
                PreviewContributors(chart.contributors)
            }

            Section(title = stringResource(R.string.stats)) {
                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    if (chart.latestVersion.duration > 0) {
                        StatListItem(
                            title = "~${StringUtils.toDurationString(chart.latestVersion.duration)}",
                            icon = R.drawable.outline_access_time_24
                        )
                    }
                    if (chart.latestVersion.notesAmount > 0) {
                        StatListItem(title = notesAmountText, icon = R.drawable.rounded_music_note_24)
                    }
                    if (chart.latestVersion.effectsAmount > 0) {
                        StatListItem(title = effectsAmountText, icon = R.drawable.rounded_blur_medium_24)
                    }
                    if (chart.downloadsSum > 0) {
                        StatListItem(title = downloadsAmountText, icon = R.drawable.rounded_download_24)
                    }
                    if (chart.contentId != null) {
                        StatListItem(
                            title = stringResource(R.string.updated_at, lastUpdated),
                            icon = R.drawable.rounded_calendar_today_24
                        )
                    }
                }
            }

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
                                    modifier = Modifier.fillMaxWidth(),
                                    text = noKnownIssuesMsg,
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
                chart.contentId?.let { contentId ->
                    if (shouldBeSelected) {
                        interactionViewModel.addToCollection(chart.id, contentId, collectionId)
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                savedToCollectionMsg(collectionName),
                                duration = SnackbarDuration.Short
                            )
                        }
                    } else {
                        interactionViewModel.removeFromCollection(chart.id, contentId, collectionId)
                    }
                }
            },
            onCreateCollection = { name, isPublic ->
                scope.launch {
                    try {
                        val newCollectionId = collectionViewModel.createCollection(name, isPublic)
                        chart.contentId?.let { interactionViewModel.addToCollection(chart.id, it, newCollectionId) }
                        snackbarHostState.showSnackbar(savedToCollectionMsg(name), duration = SnackbarDuration.Short)
                    } catch (e: ApiException) {
                        Log.e("ChartDetailsScreen", "Error creating collection", e)
                        if (e.status == HttpStatusCode.BadRequest) {
                            snackbarHostState.showSnackbar(collectionNameExistsMsg)
                        } else {
                            snackbarHostState.showSnackbar(errorCreatingCollectionMsg(e.message))
                        }
                    }
                }
            }
        )
    }
}

typealias OnNavigateToDetails = (CatalogItem) -> Unit?