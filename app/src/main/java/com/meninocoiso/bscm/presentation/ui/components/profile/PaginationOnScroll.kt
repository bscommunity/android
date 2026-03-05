package com.meninocoiso.bscm.presentation.ui.components.profile

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

@Composable
fun OnScrollLoadMore(
    listState: LazyListState,
    hasMore: Boolean,
    isLoadingMore: Boolean,
    buffer: Int = 3,
    onLoadMore: () -> Unit
) {
    LaunchedEffect(listState, hasMore, isLoadingMore, buffer) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleItem >= totalItems - buffer
        }
            .distinctUntilChanged()
            .filter { it }
            .collectLatest {
                if (hasMore && !isLoadingMore) {
                    onLoadMore()
                }
            }
    }
}
