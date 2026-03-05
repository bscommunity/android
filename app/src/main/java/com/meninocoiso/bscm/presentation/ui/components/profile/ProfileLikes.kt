package com.meninocoiso.bscm.presentation.ui.components.profile

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.domain.model.CatalogItem
import com.meninocoiso.bscm.domain.result.ContentState
import com.meninocoiso.bscm.presentation.screen.details.OnNavigateToDetails
import com.meninocoiso.bscm.presentation.ui.components.SegmentedButtonUI
import com.meninocoiso.bscm.presentation.ui.components.StatusMessageSize
import com.meninocoiso.bscm.presentation.ui.components.StatusMessageUI

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
                message = stringResource(R.string.no_liked_content),
                size = StatusMessageSize.Medium,
                icon = R.drawable.rounded_favorite_24
            )
        }
    ) {
        LazyColumn(
            modifier = modifier,
            state = listState,
            contentPadding = PaddingValues(vertical = 16.dp),
        ) {
            item {
                SegmentedButtonUI(
                    options = listOf(
                        stringResource(R.string.charts) + " (${counts.first})",
                        stringResource(R.string.tour_passes) + " (${counts.second})",
                        stringResource(R.string.themes) + " (${counts.third})"
                    ),
                    disabled = true,
                    onSelected = {}
                )
            }
            contentList(items, false, onNavigateToDetails)
            pagination(isLoadingMore = isLoadingMore, showMessage = !hasMore)
        }
    }
}