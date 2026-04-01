package com.meninocoiso.bscm.presentation.screen

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.domain.enums.CollectionKind
import com.meninocoiso.bscm.domain.model.Chart
import com.meninocoiso.bscm.domain.result.ContentState
import com.meninocoiso.bscm.presentation.ui.components.dialog.ConfirmationDialog
import com.meninocoiso.bscm.presentation.ui.components.dialog.ListenTrackDialog
import com.meninocoiso.bscm.presentation.ui.components.dialog.ReportDialog
import com.meninocoiso.bscm.presentation.viewmodel.AuthViewModel
import com.meninocoiso.bscm.presentation.viewmodel.CollectionViewModel
import com.meninocoiso.bscm.presentation.viewmodel.ContentViewModel
import com.meninocoiso.bscm.presentation.viewmodel.InteractionViewModel
import com.meninocoiso.bscm.util.StringUtils
import kotlinx.coroutines.launch

private enum class ChartDialog { None, Report, DeleteConfirmation, ListenTrack }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun DebugScreen(
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
    val hasLiveUserCollectionMembership = savedCollections.any { it.kind == CollectionKind.USER }
    val hasPersistedBookmark = hasLiveBookmarkMembership || hasLiveUserCollectionMembership || chart.bookmarkedAt != null

    val isLiked = optimisticLiked ?: (chart.likedAt != null)
    val isBookmarked = optimisticBookmarked ?: hasPersistedBookmark

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

    Box(modifier = Modifier.padding(top = 128.dp)) {
        Text(text = "Debug Screen bbb")
    }
}