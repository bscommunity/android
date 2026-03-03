package com.meninocoiso.bscm.presentation.ui.components.profile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
import com.meninocoiso.bscm.presentation.viewmodel.profile.PagedSection
import kotlinx.coroutines.launch

@Composable
fun ProfileLibrary(
    items: List<CatalogItem>,
    state: ContentState,
    isRefreshing: Boolean,
    customCollections: PagedSection<Collection>,
    counts: Triple<Int, Int, Int>,
    onFetch: (reset: Boolean) -> Unit,
    onNavigateToDetails: OnNavigateToDetails,
    onNavigateToCollection: (Collection) -> Unit,
    listState: LazyListState,
    collectionsListState: LazyListState,
    isLoadingMore: Boolean,
    hasMore: Boolean,
    onLoadMore: () -> Unit,
    isLoadingMoreCollections: Boolean,
    hasMoreCollections: Boolean,
    onLoadMoreCollections: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val tabItems = listOf(
        stringResource(R.string.charts),
        stringResource(R.string.tour_passes),
        stringResource(R.string.themes),
        stringResource(R.string.collections)
    )

    val horizontalPagerState = rememberPagerState { tabItems.size }

    // val libraryLoaded = state == ContentState.Success
    // val collectionsLoaded = customCollections.state == ContentState.Success

    Column(modifier) {
        CatalogFilters(
            itemsAmount = counts,
            showCollection = true,
            collectionsAmount = customCollections.total ?: customCollections.items.size,
            currentSelected = horizontalPagerState.currentPage,
            onFilterSelected = { index ->
                coroutineScope.launch {
                    horizontalPagerState.animateScrollToPage(index)
                }
            },
        )

        HorizontalPager(
            state = horizontalPagerState,
            key = { it },
            beyondViewportPageCount = 1,
            verticalAlignment = Alignment.Top
        ) { index ->
            when (index) {
                0 -> {
                    OnScrollLoadMore(
                        listState = listState,
                        hasMore = hasMore,
                        isLoadingMore = isLoadingMore,
                        onLoadMore = onLoadMore
                    )

                    BaseContainer(
                        isEmpty = items.filterIsInstance<Chart>().isEmpty(),
                        state = state,
                        isRefreshing = isRefreshing,
                        onRetry = onFetch,
                        empty = {
                            StatusMessageUI(
                                modifier = Modifier.fillMaxSize(),
                                size = StatusMessageSize.Medium,
                                message = stringResource(R.string.no_charts_in_library),
                                icon = R.drawable.outline_library_music_24
                            )
                        }
                    ) {
                        LazyColumn(modifier = Modifier.fillMaxWidth(), state = listState) {
                            contentList(items.filterIsInstance<Chart>(), onNavigateToDetails)
                            pagination(isLoadingMore = isLoadingMore, showMessage = !hasMore)
                        }
                    }
                }

                1 -> {
                    OnScrollLoadMore(
                        listState = listState,
                        hasMore = hasMore,
                        isLoadingMore = isLoadingMore,
                        onLoadMore = onLoadMore
                    )

                    BaseContainer(
                        isEmpty = items.filterIsInstance<TourPass>().isEmpty(),
                        state = state,
                        onRetry = onFetch,
                        empty = {
                            StatusMessageUI(
                                modifier = Modifier.fillMaxSize(),
                                size = StatusMessageSize.Medium,
                                message = stringResource(R.string.no_tour_passes_in_library),
                                icon = R.drawable.outline_library_music_24
                            )
                        }
                    ) {
                        LazyColumn(modifier = Modifier.fillMaxWidth(), state = listState) {
                            contentList(items.filterIsInstance<TourPass>(), onNavigateToDetails)
                            pagination(isLoadingMore = isLoadingMore, showMessage = !hasMore)
                        }
                    }
                }

                2 -> {
                    OnScrollLoadMore(
                        listState = listState,
                        hasMore = hasMore,
                        isLoadingMore = isLoadingMore,
                        onLoadMore = onLoadMore
                    )

                    BaseContainer(
                        isEmpty = items.filterIsInstance<Theme>().isEmpty(),
                        state = state,
                        onRetry = onFetch,
                        isRefreshing = isRefreshing,
                        empty = {
                            StatusMessageUI(
                                modifier = Modifier.fillMaxSize(),
                                size = StatusMessageSize.Medium,
                                message = stringResource(R.string.no_themes_in_library),
                                icon = R.drawable.outline_library_music_24
                            )
                        }
                    ) {
                        LazyColumn(modifier = Modifier.fillMaxWidth(), state = listState) {
                            contentList(items.filterIsInstance<Theme>(), onNavigateToDetails)
                            pagination(isLoadingMore = isLoadingMore, showMessage = !hasMore)
                        }
                    }
                }

                3 -> {
                    ProfileCollectionList(
                        items = customCollections.items,
                        state = customCollections.state,
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
                    if (state == ContentState.Loading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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