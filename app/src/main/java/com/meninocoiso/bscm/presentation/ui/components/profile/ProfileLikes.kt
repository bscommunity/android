package com.meninocoiso.bscm.presentation.ui.components.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.domain.model.CatalogItem
import com.meninocoiso.bscm.domain.model.Chart
import com.meninocoiso.bscm.domain.model.Theme
import com.meninocoiso.bscm.domain.model.TourPass
import com.meninocoiso.bscm.domain.result.ContentState
import com.meninocoiso.bscm.presentation.screen.details.OnNavigateToDetails
import kotlinx.coroutines.launch

@Composable
fun ProfileLikes(
    modifier: Modifier = Modifier,
    items: List<CatalogItem>,
    counts: Triple<Int, Int, Int>,
    state: ContentState,
    isRefreshing: Boolean,
    onFetch: (reset: Boolean) -> Unit,
    onNavigateToDetails: OnNavigateToDetails,
    listState: LazyListState,
    isLoadingMore: Boolean,
    hasMore: Boolean,
    onLoadMore: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState { 3 }

    Column(modifier) {
        if (state == ContentState.Success) {
            CatalogFilters(
                itemsAmount = counts,
                currentSelected = pagerState.currentPage,
                onFilterSelected = { index ->
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(index)
                    }
                },
            )
        }

        HorizontalPager(
            state = pagerState,
            key = { it },
            beyondViewportPageCount = 1,
            verticalAlignment = Alignment.Top
        ) { index ->
            val filteredItems = when (index) {
                0 -> items.filterIsInstance<Chart>()
                1 -> items.filterIsInstance<TourPass>()
                else -> items.filterIsInstance<Theme>()
            }

            ProfileCollectionTabContent(
                items = filteredItems,
                state = state,
                isRefreshing = isRefreshing,
                onFetch = onFetch,
                onNavigateToDetails = onNavigateToDetails,
                listState = listState,
                isLoadingMore = isLoadingMore,
                hasMore = hasMore,
                onLoadMore = onLoadMore,
                emptyMessageResource = R.string.no_liked_content,
                emptyIconRes = R.drawable.rounded_favorite_24,
            )
        }
    }
}
