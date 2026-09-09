package com.meninocoiso.bscm.presentation.ui.components.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.domain.model.CatalogItem
import com.meninocoiso.bscm.domain.model.Chart
import com.meninocoiso.bscm.domain.model.Collection
import com.meninocoiso.bscm.domain.model.Theme
import com.meninocoiso.bscm.domain.model.TourPass
import com.meninocoiso.bscm.domain.result.ContentState
import com.meninocoiso.bscm.presentation.screen.details.OnNavigateToDetails
import com.meninocoiso.bscm.presentation.ui.components.StatusMessageSize
import com.meninocoiso.bscm.presentation.ui.components.StatusMessageUI
import com.meninocoiso.bscm.presentation.ui.components.preview.CollectionPreview
import com.meninocoiso.bscm.presentation.viewmodel.profile.PagedSection
import kotlinx.coroutines.launch

@Composable
fun ProfileCollections(
    modifier: Modifier = Modifier,
    bookmarks: PagedSection<CatalogItem>,
    bookmarksCounts: Triple<Int, Int, Int>,
    customCollections: PagedSection<Collection>,
    collectionsCount: Int,
    isRefreshing: Boolean = false,
    onFetch: (reset: Boolean) -> Unit,
    onNavigateToDetails: OnNavigateToDetails,
    onNavigateToCollection: (Collection) -> Unit,
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
    val tabItems = listOf(
        stringResource(R.string.charts),
        stringResource(R.string.tour_passes),
        stringResource(R.string.themes),
        stringResource(R.string.collections)
    )

    val horizontalPagerState = rememberPagerState { tabItems.size }

    // val bookmarksLoaded = bookmarks.state == ContentState.Success
    // val collectionsLoaded = customCollections.state == ContentState.Success

    Column(modifier) {
        if (bookmarks.state == ContentState.Success || customCollections.state == ContentState.Success) {
            CatalogFilters(
                itemsAmount = bookmarksCounts,
                showCollection = true,
                collectionsAmount = collectionsCount,
                currentSelected = horizontalPagerState.currentPage,
                onFilterSelected = { index ->
                    coroutineScope.launch {
                        horizontalPagerState.animateScrollToPage(index)
                    }
                },
            )
        }

        HorizontalPager(
            state = horizontalPagerState,
            key = { it },
            beyondViewportPageCount = 1,
            verticalAlignment = Alignment.Top
        ) { index ->
            when (index) {
                0 -> {
                    ProfileCollectionTabContent(
                        items = bookmarks.items.filterIsInstance<Chart>(),
                        state = bookmarks.state,
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

                1 -> {
                    ProfileCollectionTabContent(
                        items = bookmarks.items.filterIsInstance<TourPass>(),
                        state = bookmarks.state,
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

                2 -> {
                    ProfileCollectionTabContent(
                        items = bookmarks.items.filterIsInstance<Theme>(),
                        state = bookmarks.state,
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
                    ProfileCollectionList(
                        items = customCollections.items,
                        state = customCollections.state,
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
                    if (bookmarks.state == ContentState.Loading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        StatusMessageUI(
                            modifier = Modifier.fillMaxWidth(),
                            message = stringResource(R.string.no_content_available),
                            icon = R.drawable.outline_library_music_24
                        )
                    }
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
    emptyMessageResource: Int = R.string.no_bookmarked_content,
    emptyIconRes: Int = R.drawable.outline_library_music_24,
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
                message = stringResource(emptyMessageResource),
                icon = emptyIconRes
            )
        }
    ) {
        LazyColumn(modifier = modifier, state = listState) {
            contentList(items, false, onNavigateToDetails)
            pagination(isLoadingMore = isLoadingMore, showMessage = !hasMore)
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
    onNavigateToCollection: (Collection) -> Unit,
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
                message = stringResource(R.string.no_custom_collections),
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
                    onPress = { onNavigateToCollection(item) }
                )
            }
            pagination(isLoadingMore = isLoadingMore, showMessage = !hasMore)
        }
    }
}