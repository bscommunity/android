package com.meninocoiso.bscm.presentation.ui.components.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.domain.enums.CollectionKind
import com.meninocoiso.bscm.domain.model.CatalogItem
import com.meninocoiso.bscm.domain.model.Chart
import com.meninocoiso.bscm.domain.model.Collection
import com.meninocoiso.bscm.domain.model.SimplifiedCollection
import com.meninocoiso.bscm.domain.model.Theme
import com.meninocoiso.bscm.domain.model.TourPass
import com.meninocoiso.bscm.domain.model.toSimplifiedCollection
import com.meninocoiso.bscm.domain.result.ContentState
import com.meninocoiso.bscm.presentation.screen.details.OnNavigateToDetails
import com.meninocoiso.bscm.presentation.ui.components.StatusMessageSize
import com.meninocoiso.bscm.presentation.ui.components.StatusMessageUI
import com.meninocoiso.bscm.presentation.ui.components.preview.CollectionPreview
import kotlinx.coroutines.launch

@Composable
fun ProfileCollections(
    modifier: Modifier = Modifier,
    items: List<Collection>,
    state: ContentState,
    isRefreshing: Boolean = false,
    onFetch: (reset: Boolean) -> Unit,
    onNavigateToDetails: OnNavigateToDetails,
    onNavigateToCollection: (SimplifiedCollection) -> Unit,
    bookmarksListState: LazyListState,
    collectionsListState: LazyListState,
    isLoadingMoreBookmarks: Boolean,
    hasMoreBookmarks: Boolean,
    onLoadMoreBookmarks: () -> Unit,
    isLoadingMoreCollections: Boolean,
    hasMoreCollections: Boolean,
    onLoadMoreCollections: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val tabItems = listOf("Charts", "Tour Passes", "Themes", "Collections")

    val horizontalPagerState = rememberPagerState { tabItems.size }

    val bookmarksCollection = items.find { it.kind == CollectionKind.BOOKMARKS }
    val customCollections = items.filter { it.kind == CollectionKind.USER }

    Column(modifier) {
        // We subtract bookmarks from the total count to get the number of custom ones
        val collectionsAmount = (items.size - 1).coerceAtLeast(0)

        if (items.isNotEmpty()) {
            CatalogFilters(
                // TODO: Get these counts from the API instead of estimating them here,
                //  since we might not be fetching all items at once
                itemsAmount = Triple(
                    bookmarksCollection?.items?.count { it is Chart } ?: 0,
                    bookmarksCollection?.items?.count { it is TourPass } ?: 0,
                    bookmarksCollection?.items?.count { it is Theme } ?: 0
                ),
                collectionsAmount = collectionsAmount,
                currentSelected = horizontalPagerState.currentPage,
                onFilterSelected = { index ->
                    coroutineScope.launch {
                        // Update pager when a tab is selected
                        horizontalPagerState.animateScrollToPage(index)
                    }
                },
            )
        }

        HorizontalPager(
            state = horizontalPagerState,
            key = { it }, // Recompose the pager when the page changes
            beyondViewportPageCount = 1 // Keep the next page in memory
        ) { index ->
            when (index) {
                0 -> {
                    // Charts
                    ProfileCollectionTabContent(
                        items = bookmarksCollection?.items ?: emptyList(),
                        state = state,
                        isRefreshing = isRefreshing,
                        onFetch = onFetch,
                        onNavigateToDetails = onNavigateToDetails,
                        listState = bookmarksListState,
                        isLoadingMore = isLoadingMoreBookmarks,
                        hasMore = hasMoreBookmarks,
                        onLoadMore = onLoadMoreBookmarks,
                        modifier = Modifier
                            .fillMaxWidth()
                    )
                }

                3 -> {
                    // Collections
                    ProfileCollectionList(
                        items = customCollections,
                        state = state,
                        isRefreshing = isRefreshing,
                        onFetch = onFetch,
                        onNavigateToCollection = onNavigateToCollection,
                        listState = collectionsListState,
                        isLoadingMore = isLoadingMoreCollections,
                        hasMore = hasMoreCollections,
                        onLoadMore = onLoadMoreCollections,
                        modifier = Modifier
                            .fillMaxWidth()
                    )
                }

                else -> {
                    // Other tabs (Tour Passes, Themes) - Placeholder
                    StatusMessageUI(
                        modifier = Modifier.fillMaxWidth(),
                        message = "No content available",
                        icon = R.drawable.outline_library_music_24
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileCollectionTabContent(
    modifier: Modifier = Modifier,
    items: List<CatalogItem>,
    state: ContentState,
    isRefreshing: Boolean = false,
    onFetch: (reset: Boolean) -> Unit,
    onNavigateToDetails: OnNavigateToDetails,
    listState: LazyListState,
    isLoadingMore: Boolean,
    hasMore: Boolean,
    onLoadMore: () -> Unit,
) {
    OnScrollLoadMore(
        listState = listState,
        hasMore = hasMore,
        isLoadingMore = isLoadingMore,
        onLoadMore = onLoadMore
    )

    BaseContainer(
        isEmpty = items.isEmpty(),
        state = state,
        isRefreshing = isRefreshing,
        onRetry = onFetch,
        empty = {
            StatusMessageUI(
                modifier = Modifier.fillMaxSize(),
                size = StatusMessageSize.Medium,
                message = "No bookmarked content",
                icon = R.drawable.outline_library_music_24
            )
        }
    ) {
        LazyColumn(modifier = modifier, state = listState) {
            contentList(items, onNavigateToDetails)
            pagination(
                isLoadingMore = isLoadingMore,
                message = if (!hasMore) "Fim da lista" else ""
            )
        }
    }
}

@Composable
fun ProfileCollectionList(
    modifier: Modifier = Modifier,
    items: List<Collection>,
    state: ContentState,
    isRefreshing: Boolean = false,
    onFetch: (reset: Boolean) -> Unit,
    onNavigateToCollection: (SimplifiedCollection) -> Unit,
    listState: LazyListState,
    isLoadingMore: Boolean,
    hasMore: Boolean,
    onLoadMore: () -> Unit,
) {
    OnScrollLoadMore(
        listState = listState,
        hasMore = hasMore,
        isLoadingMore = isLoadingMore,
        onLoadMore = onLoadMore
    )

    BaseContainer(
        isEmpty = items.isEmpty(),
        state = state,
        isRefreshing = isRefreshing,
        onRetry = onFetch,
        empty = {
            StatusMessageUI(
                modifier = Modifier.fillMaxSize(),
                message = "No content in library",
                icon = R.drawable.outline_library_music_24
            )
        }
    ) {
        LazyVerticalGrid(
            modifier = modifier,
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(items.size) { index ->
                val item = items[index]
                CollectionPreview(
                    collection = item,
                    onPress = { onNavigateToCollection(item.toSimplifiedCollection()) }
                )
            }
            pagination(
                isLoadingMore = isLoadingMore,
                message = if (!hasMore) "End of list" else ""
            )
        }
    }
}