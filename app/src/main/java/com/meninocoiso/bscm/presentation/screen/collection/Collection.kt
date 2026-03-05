package com.meninocoiso.bscm.presentation.screen.collection

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTopAppBarState
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.domain.enums.ButtonVariant
import com.meninocoiso.bscm.domain.model.SimplifiedCollection
import com.meninocoiso.bscm.presentation.screen.details.DropdownItemPadding
import com.meninocoiso.bscm.presentation.screen.details.OnNavigateToDetails
import com.meninocoiso.bscm.presentation.ui.components.ButtonUI
import com.meninocoiso.bscm.presentation.ui.components.DropdownMenuUI
import com.meninocoiso.bscm.presentation.ui.components.StatusMessageUI
import com.meninocoiso.bscm.presentation.ui.components.details.CollectionEditBottomSheet
import com.meninocoiso.bscm.presentation.ui.components.dialog.ConfirmationDialog
import com.meninocoiso.bscm.presentation.ui.components.layout.Avatar
import com.meninocoiso.bscm.presentation.ui.components.profile.BaseContainer
import com.meninocoiso.bscm.presentation.ui.components.profile.CatalogFilters
import com.meninocoiso.bscm.presentation.ui.components.profile.OnScrollLoadMore
import com.meninocoiso.bscm.presentation.ui.components.profile.contentList
import com.meninocoiso.bscm.presentation.ui.components.profile.pagination
import com.meninocoiso.bscm.presentation.ui.utils.resolve
import com.meninocoiso.bscm.presentation.viewmodel.CollectionViewModel
import com.meninocoiso.bscm.util.LinkingUtils
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data class Collection(val collection: SimplifiedCollection)

@Serializable
data class DeepLinkCollection(val username: String, val slug: String)

private enum class CollectionDialog { None, DeleteConfirmation }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun CollectionScreen(
    collection: SimplifiedCollection,
    isOwner: Boolean,
    onReturn: () -> Unit,
    onNavigateToDetails: OnNavigateToDetails,
    viewModel: CollectionViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var currentCollection by remember { mutableStateOf(collection) }
    var isDeleting by remember { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val items = uiState.items
    val itemCount = collection.itemCount.toList().sum()

    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }

    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val pullToRefreshState = rememberPullToRefreshState()

    // Wire snackbar events from the ViewModel
    LaunchedEffect(viewModel) {
        viewModel.snackbarEvents.collect { snackbarHostState.showSnackbar(it.resolve(context)) }
    }

    // Initial load — resets automatically when collection.id changes
    LaunchedEffect(collection.id) {
        viewModel.loadItems(collection.id, reset = true)
    }

    // Scroll-driven pagination
    OnScrollLoadMore(
        listState = listState,
        hasMore = items.hasMore,
        isLoadingMore = items.isLoadingMore,
        onLoadMore = { viewModel.loadMoreItems(collection.id) },
    )

    // -------------------------------------------------------------------------
    // Dialog management
    // -------------------------------------------------------------------------
    var currentDialog by rememberSaveable { mutableStateOf(CollectionDialog.None) }

    when (currentDialog) {
        CollectionDialog.DeleteConfirmation -> {
            ConfirmationDialog(
                title = stringResource(R.string.collection_delete),
                message = stringResource(R.string.collection_delete_description),
                onDismiss = {
                    currentDialog = CollectionDialog.None
                    isDeleting = false
                },
                onConfirm = {
                    scope.launch {
                        isDeleting = true
                        val result = viewModel.deleteCollection(collection.id)
                        isDeleting = false
                        if (result.isSuccess) {
                            currentDialog = CollectionDialog.None
                            onReturn()
                        }
                    }
                },
                isLoading = isDeleting
            )
        }

        CollectionDialog.None -> {}
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            LargeTopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background,
                ),
                navigationIcon = {
                    IconButton(
                        modifier = Modifier.padding(end = 12.dp),
                        onClick = onReturn,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            tint = MaterialTheme.colorScheme.onSurface,
                            contentDescription = stringResource(R.string.return_screen),
                        )
                    }
                },
                actions = {
                    if (isOwner) {
                        DropdownMenuUI { dismiss ->
                            if (currentCollection.slug != null) {
                                DropdownMenuItem(
                                    contentPadding = DropdownItemPadding,
                                    text = { Text(stringResource(R.string.share)) },
                                    leadingIcon = {
                                        Icon(Icons.Outlined.Share, contentDescription = null)
                                    },
                                    onClick = {
                                        dismiss()
                                        LinkingUtils.shareCollection(
                                            context = context,
                                            username = currentCollection.owner.username,
                                            slug = currentCollection.slug!!,
                                        )
                                    }
                                )
                            }

                            DropdownMenuItem(
                                contentPadding = DropdownItemPadding,
                                text = { Text(stringResource(R.string.collection_delete)) },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Delete, contentDescription = null)
                                },
                                onClick = {
                                    dismiss()
                                    currentDialog = CollectionDialog.DeleteConfirmation
                                }
                            )
                        }
                    } else if (currentCollection.slug != null) {
                        IconButton(onClick = {
                            LinkingUtils.shareCollection(
                                context = context,
                                username = currentCollection.owner.username,
                                slug = currentCollection.slug,
                            )
                        }) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = stringResource(R.string.share),
                            )
                        }
                    }
                },
                title = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalAlignment = Alignment.Start,
                    ) {
                        // Name is available immediately from the route parameter —
                        // no loading state needed for the header.
                        Text(currentCollection.name, style = MaterialTheme.typography.headlineSmall)


                        if (itemCount > 0) {
                            Text(
                                // Use plurals resource for proper localization
                                pluralStringResource(R.plurals.items_count, itemCount, itemCount),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        BaseContainer(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            pullToRefreshState = pullToRefreshState,
            state = items.state,
            isRefreshing = items.isRefreshing,
            onRetry = { viewModel.refreshItems(collection.id) },
            isEmpty = items.items.isEmpty(),
            empty = {
                StatusMessageUI(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp),
                    message = stringResource(R.string.no_content_in_collection),
                    icon = R.drawable.outline_library_music_24,
                )
            }
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize(),
                state = listState,
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.Start,
            ) {
                item {
                    if (isOwner) {
                        ButtonUI(
                            text = stringResource(R.string.manage_collection),
                            icon = R.drawable.outline_settings_24,
                            onClick = {
                                showBottomSheet = true
                            },
                            modifier = Modifier.padding(start = 16.dp),
                            variant = ButtonVariant.Tonal
                        )
                    } else {
                        Row(
                            modifier = Modifier
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Avatar(
                                url = collection.owner.avatarUrl,
                                alt = collection.owner.username.first().toString(),
                                size = 16.dp
                            )
                            Text(
                                style = MaterialTheme.typography.bodySmall,
                                text = stringResource(
                                    R.string.collection_by,
                                    collection.owner.username
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                        }
                    }
                }

                item {
                    CatalogFilters(
                        collection.itemCount,
                        onFilterSelected = {},
                    )
                }
                contentList(
                    items = items.items,
                    showInteractions = !isOwner,
                    onNavigateToDetails = onNavigateToDetails
                )
                pagination(
                    isLoadingMore = items.isLoadingMore,
                    showMessage = !items.hasMore
                )
                // TODO: Workaround to avoid bugging the scroll when the list has few items
                item {
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1200.dp)
                    )
                }
            }
        }

        if (showBottomSheet) {
            CollectionEditBottomSheet(
                sheetState = sheetState,
                onDismissRequest = { showBottomSheet = false },
                onClose = { showBottomSheet = false },
                collection = currentCollection,
                onSaveChanges = { name, isPublic ->
                    val result = viewModel.updateCollection(collection.id, name, isPublic)
                    if (result.isSuccess) {
                        val slug = result.getOrNull()
                        currentCollection =
                            currentCollection.copy(name = name, isPublic = isPublic, slug = slug)
                    }
                },
                isLoading = uiState.isUpdating
            )
        }
    }
}