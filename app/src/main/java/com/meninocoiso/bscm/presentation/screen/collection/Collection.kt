package com.meninocoiso.bscm.presentation.screen.collection

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.domain.enums.ButtonVariant
import com.meninocoiso.bscm.domain.model.SimplifiedCollection
import com.meninocoiso.bscm.presentation.screen.details.OnNavigateToDetails
import com.meninocoiso.bscm.presentation.ui.components.ButtonUI
import com.meninocoiso.bscm.presentation.ui.components.StatusMessageUI
import com.meninocoiso.bscm.presentation.ui.components.profile.BaseContainer
import com.meninocoiso.bscm.presentation.ui.components.profile.CatalogFilters
import com.meninocoiso.bscm.presentation.ui.components.profile.OnScrollLoadMore
import com.meninocoiso.bscm.presentation.ui.components.profile.contentList
import com.meninocoiso.bscm.presentation.ui.components.profile.pagination
import com.meninocoiso.bscm.presentation.viewmodel.CollectionViewModel
import kotlinx.serialization.Serializable

@Serializable
data class Collection(val collection: SimplifiedCollection)

@Serializable
data class DeepLinkCollection(val username: String, val slug: String)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun CollectionScreen(
    collection: SimplifiedCollection,
    onReturn: () -> Unit,
    onNavigateToDetails: OnNavigateToDetails,
    viewModel: CollectionViewModel = hiltViewModel(),
) {
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val items = uiState.items
    val itemCount = collection.itemCount.toList().sum()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    // Wire snackbar events from the ViewModel
    LaunchedEffect(viewModel) {
        viewModel.snackbarEvents.collect { snackbarHostState.showSnackbar(it) }
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
                    IconButton(onClick = {}) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = stringResource(R.string.share),
                        )
                    }
                },
                title = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalAlignment = Alignment.Start,
                    ) {
                        // Name is available immediately from the route parameter —
                        // no loading state needed for the header.
                        Text(collection.name, style = MaterialTheme.typography.headlineSmall)
                        if (itemCount > 0) {
                            Text(
                                "${itemCount} items",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            isEmpty = items.items.isEmpty(),
            state = items.state,
            isRefreshing = items.isRefreshing,
            onRetry = { viewModel.loadItems(collection.id, reset = true) },
            empty = {
                StatusMessageUI(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp),
                    message = "No content in this collection",
                    icon = R.drawable.outline_library_music_24,
                )
            },
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .padding(innerPadding),
                state = listState,
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.Start,
            ) {
                item {
                    ButtonUI(
                        text = "Manage collection",
                        icon = R.drawable.outline_settings_24,
                        onClick = { /* Navigate to edit collection */ },
                        modifier = Modifier.padding(start = 16.dp),
                        variant = ButtonVariant.Tonal
                    )
                }
                item {
                    CatalogFilters(
                        collection.itemCount,
                        onFilterSelected = {},
                    )
                }
                contentList(items = items.items, onNavigateToDetails = onNavigateToDetails)
                pagination(
                    isLoadingMore = items.isLoadingMore,
                    message = if (items.hasMore) "Carregando..." else "Fim da coleção",
                )
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun CollectionScreenPreview() {
    MaterialTheme {
        CollectionScreen(
            collection = SimplifiedCollection(id = "preview", name = "My Collection", itemCount = Triple(5, 3, 2)),
            onNavigateToDetails = {},
            onReturn = {},
        )
    }
}
