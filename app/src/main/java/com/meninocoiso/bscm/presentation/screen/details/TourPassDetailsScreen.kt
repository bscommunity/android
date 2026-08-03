package com.meninocoiso.bscm.presentation.screen.details

import DownloadEvent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.domain.enums.CollectionKind
import com.meninocoiso.bscm.domain.model.Chart
import com.meninocoiso.bscm.domain.model.Contributor
import com.meninocoiso.bscm.domain.model.TourPass
import com.meninocoiso.bscm.domain.state.DownloadState
import com.meninocoiso.bscm.presentation.ui.components.DropdownMenuUI
import com.meninocoiso.bscm.presentation.ui.components.details.InteractionButton
import com.meninocoiso.bscm.presentation.ui.components.details.StatListItem
import com.meninocoiso.bscm.presentation.ui.components.details.TourPassDownloadButton
import com.meninocoiso.bscm.presentation.ui.components.dialog.ConfirmationDialog
import com.meninocoiso.bscm.presentation.ui.components.layout.CoverArt
import com.meninocoiso.bscm.presentation.ui.components.layout.Section
import com.meninocoiso.bscm.presentation.ui.components.layout.SwipeableSnackbarHost
import com.meninocoiso.bscm.presentation.ui.components.preview.PreviewContributors
import com.meninocoiso.bscm.presentation.ui.components.preview.TourPassTrackPreview
import com.meninocoiso.bscm.presentation.ui.utils.showReplacingSnackbar
import com.meninocoiso.bscm.presentation.viewmodel.AuthViewModel
import com.meninocoiso.bscm.presentation.viewmodel.ContentViewModel
import com.meninocoiso.bscm.presentation.viewmodel.InteractionViewModel
import com.meninocoiso.bscm.util.AudioPreviewPlayer
import com.meninocoiso.bscm.util.StringUtils
import kotlinx.coroutines.launch

private enum class TourPassDialog { None, DeleteConfirmation }

/**
 * Details screen for one tour pass: cover art, credits, stats and a
 * tracklist grid with audio previews. The bottom bar exposes like, bookmark
 * and download actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TourPassDetailsScreen(
    tourPass: TourPass,
    onReturn: () -> Unit,
    onNavigateToChart: (Chart) -> Unit,
    onNavigateToSettings: () -> Unit,
    contentViewModel: ContentViewModel = hiltViewModel(),
    interactionViewModel: InteractionViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    var playingUrl by remember { mutableStateOf<String?>(null) }
    val audioPreviewPlayer = remember { AudioPreviewPlayer { playingUrl = it } }

    DisposableEffect(Unit) {
        onDispose { audioPreviewPlayer.stop() }
    }

    val totalMinutesText = pluralStringResource(
        R.plurals.tour_pass_minutes_total,
        (tourPass.charts.sumOf { it.track.duration.toLong() } / 60).toInt(),
        (tourPass.charts.sumOf { it.track.duration.toLong() } / 60).toInt()
    )
    val songsText = pluralStringResource(
        R.plurals.songs_count,
        tourPass.charts.size,
        tourPass.charts.size
    )
    val downloadsText = pluralStringResource(
        R.plurals.downloads_amount,
        tourPass.downloadsSum,
        tourPass.downloadsSum
    )
    val uploadedText = stringResource(
        R.string.uploaded_at,
        StringUtils.toRelativeString(tourPass.updatedAt ?: tourPass.createdAt)
    )

    val (tourPassAuthors, customSubtitles) = remember(tourPass) {
        buildTourPassContributorList(tourPass)
    }

    // -------------------------------------------------------------------------
    // Interactions (like / bookmark)
    // -------------------------------------------------------------------------
    val isLoggedIn by authViewModel.isLoggedInFlow
        .collectAsStateWithLifecycle(false)
    val savedCollections by interactionViewModel
        .getContentCollections(tourPass.id)
        .collectAsStateWithLifecycle()

    var optimisticLiked by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var optimisticBookmarked by rememberSaveable { mutableStateOf<Boolean?>(null) }

    val hasLiveBookmarkMembership = savedCollections.any { it.kind == CollectionKind.BOOKMARKS }

    val isLiked = optimisticLiked ?: (tourPass.likedAt != null)
    val isBookmarked = optimisticBookmarked ?: (hasLiveBookmarkMembership || tourPass.bookmarkedAt != null)

    // Clear optimistic state once persistence catches up
    LaunchedEffect(tourPass.likedAt) {
        if (optimisticLiked != null && tourPass.likedAt != null) optimisticLiked = null
    }
    LaunchedEffect(savedCollections, tourPass.bookmarkedAt) {
        optimisticBookmarked?.let { optimistic ->
            val confirmed = if (optimistic) hasLiveBookmarkMembership else !hasLiveBookmarkMembership
            if (confirmed) optimisticBookmarked = null
        }
    }

    // -------------------------------------------------------------------------
    // Download state
    // -------------------------------------------------------------------------
    val tourPassState by contentViewModel.getTourPassDownloadState(tourPass)
        .collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val downloadCompleteMsg = stringResource(R.string.download_complete)
    val errorTitleMsg = stringResource(R.string.error)
    val connectToManageFavoritesMsg = stringResource(R.string.connect_to_manage_favorites)
    val connectToManageLikesMsg = stringResource(R.string.connect_to_manage_likes)
    val connectLabel = stringResource(R.string.connect)
    val tourPassDeletedMsg = stringResource(R.string.tour_pass_deleted)
    val failedToDeleteMsg = stringResource(R.string.failed_to_delete_chart)

    var currentDialog by rememberSaveable { mutableStateOf(TourPassDialog.None) }

    when (currentDialog) {
        TourPassDialog.DeleteConfirmation -> ConfirmationDialog(
            title = stringResource(R.string.delete_tour_pass),
            message = stringResource(R.string.delete_tour_pass_description),
            onDismiss = { currentDialog = TourPassDialog.None },
            onConfirm = {
                currentDialog = TourPassDialog.None
                contentViewModel.uninstallTourPass(
                    tourPass,
                    onSuccess = {
                        scope.launch { snackbarHostState.showReplacingSnackbar(tourPassDeletedMsg) }
                    },
                    onError = {
                        scope.launch { snackbarHostState.showReplacingSnackbar(failedToDeleteMsg) }
                    }
                )
            }
        )
        TourPassDialog.None -> {}
    }

    LaunchedEffect(tourPass.id) {
        contentViewModel.checkTourPassStatus(tourPass)
    }

    LaunchedEffect(tourPass.id) {
        contentViewModel.events.collect { event ->
            if (event.id != tourPass.id) return@collect
            when (event) {
                is DownloadEvent.Complete ->
                    snackbarHostState.showReplacingSnackbar(downloadCompleteMsg)

                is DownloadEvent.Error ->
                    snackbarHostState.showReplacingSnackbar("$errorTitleMsg: ${event.message}")

                else -> {}
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
                title = {
                    Column {
                        Text(tourPass.name, style = MaterialTheme.typography.titleLarge)
                        tourPass.artist?.let {
                            Text(it, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                },
                actions = {
                    if (tourPassState == DownloadState.Installed(tourPass.id)) {
                        DropdownMenuUI { dismiss ->
                            DropdownMenuItem(
                                contentPadding = DropdownItemPadding,
                                text = { Text(stringResource(R.string.delete_tour_pass)) },
                                leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                                onClick = {
                                    dismiss()
                                    currentDialog = TourPassDialog.DeleteConfirmation
                                }
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(
                actions = {
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
                        }
                    ) { newValue ->
                        optimisticBookmarked = newValue
                        interactionViewModel.enqueueBookmarkMutation(
                            tourPass.id,
                            tourPass.id,
                            newValue
                        )
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
                        interactionViewModel.enqueueLikeMutation(
                            tourPass.id,
                            tourPass.id,
                            newValue
                        )
                    }
                },
                floatingActionButton = {
                    TourPassDownloadButton(
                        tourPass = tourPass,
                        downloadState = tourPassState,
                        contentViewModel = contentViewModel,
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(modifier = Modifier.padding(top = 16.dp, bottom = 8.dp, start = 16.dp, end = 16.dp)) {
                CoverArt(
                    url = tourPass.coverUrl ?: "",
                    width = Dp.Unspecified,
                    height = 96.dp,
                    borderRadius = 16.dp
                )
            }

            if (tourPassAuthors.isNotEmpty()) {
                PreviewContributors(
                    authors = tourPassAuthors,
                    description = stringResource(R.string.tour_pass_contributors_list_title),
                    customSubtitles = customSubtitles
                )
            }

            Section(title = stringResource(R.string.stats)) {
                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    StatListItem(title = totalMinutesText, icon = R.drawable.rounded_hourglass_24)
                    StatListItem(title = songsText, icon = R.drawable.rounded_music_note_24)
                    StatListItem(title = downloadsText, icon = R.drawable.rounded_download_24)
                    StatListItem(title = uploadedText, icon = R.drawable.rounded_calendar_today_24)
                }
            }

            Section(title = stringResource(R.string.tracklist)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(),
                ) {
                    tourPass.charts.chunked(3).forEach { rowCharts ->
                        Row {
                            rowCharts.forEach { chart ->
                                Column(Modifier.weight(1f)) {
                                    TourPassTrackPreview(
                                        chart = chart,
                                        isPlaying = playingUrl != null && playingUrl == chart.track.previewUrl,
                                        onTogglePlay = {
                                            chart.track.previewUrl?.let { url ->
                                                audioPreviewPlayer.toggle(url)
                                            }
                                        },
                                        onClick = { onNavigateToChart(chart) }
                                    )
                                }
                            }
                            // Keep every row at 3 columns so a partial last row
                            // leaves a blank cell instead of stretching its items.
                            repeat(3 - rowCharts.size) {
                                Spacer(modifier = Modifier.weight(1f))
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
            if (tourPassState is DownloadState.Downloading ||
                tourPassState is DownloadState.Extracting
            ) {
                LinearProgressIndicator(
                    progress = {
                        when (val state = tourPassState) {
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
}

/**
 * Builds the credits list of a tour pass: tour pass contributors first (they
 * keep their roles), followed by chart-only contributors. Returns the list of
 * [Contributor]s for [PreviewContributors] plus a map from user id to the
 * chart titles that user participated in, used as the subtitle for users that
 * have no role in the tour pass itself.
 */
private fun buildTourPassContributorList(
    tourPass: TourPass
): Pair<List<Contributor>, Map<String, String>> {
    val tourPassContributorIds = tourPass.contributors.map { it.user.id }.toSet()

    val chartOnlyContributions = tourPass.charts
        .flatMap { chart ->
            chart.contributors.map { contributor ->
                contributor to chart.track.title
            }
        }
        .filter { (contributor, _) -> contributor.user.id !in tourPassContributorIds }
        .groupBy { it.first.user.id }

    val customSubtitles = chartOnlyContributions.mapValues { (_, contributions) ->
        contributions.map { it.second }.distinct().joinToString(", ")
    }

    val chartOnlyContributors = chartOnlyContributions.map { (_, contributions) ->
        contributions.first().first
    }

    return (tourPass.contributors + chartOnlyContributors) to customSubtitles
}
