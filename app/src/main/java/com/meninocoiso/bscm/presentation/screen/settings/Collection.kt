package com.meninocoiso.bscm.presentation.screen.settings

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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meninocoiso.bscm.R
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
data class Collection(val collectionId: String)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun CollectionScreen(
    collectionId: String,
    onNavigateToDetails: OnNavigateToDetails,
    onReturn: () -> Unit,
    collectionViewModel: CollectionViewModel = hiltViewModel()
) {
    val listState = rememberLazyListState()

    val collectionItems by collectionViewModel.collectionItems.collectAsStateWithLifecycle()
    val collectionState by collectionViewModel.collectionItemsContentState.collectAsStateWithLifecycle()
    val isLoadingMore by collectionViewModel.isLoadingMoreItems.collectAsStateWithLifecycle()
    val hasMoreItems by collectionViewModel.hasMoreItems.collectAsStateWithLifecycle()

    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    LaunchedEffect(collectionId) {
        collectionViewModel.loadCollectionItems(collectionId, reset = true)
    }

    OnScrollLoadMore(
        listState = listState,
        hasMore = hasMoreItems,
        isLoadingMore = isLoadingMore,
        onLoadMore = { collectionViewModel.loadCollectionItems(collectionId, reset = false) }
    )

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                ),
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
                    IconButton(onClick = { }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = stringResource(R.string.share)
                        )
                    }
                },
                title = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text("Coleção", style = MaterialTheme.typography.headlineSmall)
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        BaseContainer(
            isEmpty = collectionItems.isEmpty(),
            state = collectionState,
            onRetry = { collectionViewModel.loadCollectionItems(collectionId, reset = true) },
            empty = {
                StatusMessageUI(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp),
                    message = "No content in this collection",
                    icon = R.drawable.outline_library_music_24
                )
            }
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
                        modifier = Modifier
                            .padding(start = 16.dp)
                    )
                }
                item {
                    CatalogFilters(
                        items = collectionItems,
                        onFilterSelected = { /* Handle filter selection */ })
                }
                contentList(items = collectionItems, onNavigateToDetails = onNavigateToDetails)
                pagination(
                    isLoadingMore = isLoadingMore,
                    message = if (hasMoreItems) "Carregando..." else "Fim da coleção"
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
            collectionId = "collectionId",
            onNavigateToDetails = { },
            onReturn = {}
        )
    }
}

