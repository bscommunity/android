package com.meninocoiso.bscm.presentation.ui.components.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.domain.model.CatalogItem
import com.meninocoiso.bscm.domain.model.Collection
import com.meninocoiso.bscm.domain.result.ContentState
import com.meninocoiso.bscm.presentation.screen.details.OnNavigateToDetails
import com.meninocoiso.bscm.presentation.ui.components.StatusMessageUI
import com.meninocoiso.bscm.presentation.ui.components.preview.CollectionPreview
import kotlinx.coroutines.launch

@Composable
fun ProfileCollections(
    modifier: Modifier = Modifier,
    items: List<Collection>,
    state: ContentState,
    onFetch: () -> Unit,
    onNavigateToDetails: OnNavigateToDetails,
    onNavigateToCollection: (collectionId: String) -> Unit,
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
    val tabItems = listOf("All", "Collections", "Charts", "Tour Passes", "Themes")

    val horizontalPagerState = rememberPagerState { tabItems.size }

    val bookmarksCollection = items.find { it.name == "bookmarks" }

    val customCollections = items.filter {
        it.name != "bookmarks" && it.name != "liked"
    }

    if (bookmarksCollection == null) {
        StatusMessageUI(
            modifier = Modifier.fillMaxSize(),
            message = "No collections found",
            icon = R.drawable.outline_library_music_24
        )
        return
    }

    Column(modifier) {
        val extraTabs = (items.size - 2).coerceAtLeast(0)
        CatalogFilters(
            bookmarksCollection.items,
            extraTabs,
            currentSelected = horizontalPagerState.currentPage,
            onFilterSelected = { index ->
                coroutineScope.launch {
                    // Update pager when a tab is selected
                    horizontalPagerState.animateScrollToPage(index)
                }
            },
        )

        HorizontalPager(
            state = horizontalPagerState,
            key = { it }, // Recompose the pager when the page changes
            beyondViewportPageCount = 1 // Keep the next page in memory
        ) { index ->
            when (index) {
                0 -> {
                    // All
                    ProfileCollectionTabContent(
                        items = bookmarksCollection.items,
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

                1 -> {
                    // Collections
                    ProfileCollectionList(
                        items = customCollections,
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

                2 -> {
                    // Charts
                    ProfileCollectionTabContent(
                        items = bookmarksCollection.items,
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
    items: List<CatalogItem>,
    onFetch: () -> Unit,
    onNavigateToDetails: OnNavigateToDetails,
    listState: LazyListState,
    isLoadingMore: Boolean,
    hasMore: Boolean,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state = ContentState.Success

    OnScrollLoadMore(
        listState = listState,
        hasMore = hasMore,
        isLoadingMore = isLoadingMore,
        onLoadMore = onLoadMore
    )

    BaseContainer(
        isEmpty = items.isEmpty(),
        state = state,
        onRetry = onFetch,
        empty = {
            StatusMessageUI(
                modifier = Modifier.fillMaxSize(),
                message = "No bookmarked content",
                icon = R.drawable.outline_library_music_24
            )
        }
    ) {
        LazyColumn(modifier = modifier, state = listState) {
            contentList(items, onNavigateToDetails)
            pagination(
                isLoadingMore = isLoadingMore,
                message = if (hasMore) "Carregando..." else "Fim da lista"
            )
        }
    }
}

@Composable
fun ProfileCollectionList(
    items: List<Collection>,
    onFetch: () -> Unit,
    onNavigateToCollection: (collectionId: String) -> Unit,
    listState: LazyListState,
    isLoadingMore: Boolean,
    hasMore: Boolean,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state = ContentState.Success

    OnScrollLoadMore(
        listState = listState,
        hasMore = hasMore,
        isLoadingMore = isLoadingMore,
        onLoadMore = onLoadMore
    )

    BaseContainer(
        isEmpty = items.isEmpty(),
        state = state,
        onRetry = onFetch,
        empty = {
            StatusMessageUI(
                modifier = Modifier.fillMaxSize(),
                message = "No content in library",
                icon = R.drawable.outline_library_music_24
            )
        }
    ) {
        LazyColumn(
            modifier = modifier,
            state = listState,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(items.size) { index ->
                val item = items[index]
                CollectionPreview(
                    collection = item,
                    onPress = { onNavigateToCollection(item.id) }
                )
            }
            pagination(
                isLoadingMore = isLoadingMore,
                message = if (hasMore) "Carregando..." else "Fim da lista"
            )
        }
    }
}