package com.meninocoiso.bscm.presentation.ui.components.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meninocoiso.bscm.presentation.viewmodel.PaginatedContentState

/**
 * Reusable paginated list component that handles all common pagination UI patterns.
 *
 * @param paginatedState The paginated content state managing data and loading
 * @param listState The lazy list state for scroll position
 * @param modifier Modifier for the list
 * @param contentPadding Padding for the list content
 * @param verticalArrangement Vertical arrangement of items
 * @param horizontalAlignment Horizontal alignment of items
 * @param emptyContent Content to show when list is empty
 * @param loadMoreMessage Message to show when loading more items
 * @param endMessage Message to show when all items are loaded
 * @param header Optional header content before items
 * @param items Content of the paginated items
 */
@Composable
fun <T> PaginatedList(
    paginatedState: PaginatedContentState<T>,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    emptyContent: @Composable () -> Unit,
    loadMoreMessage: String = "Loading...",
    endMessage: String = "End of list",
    header: (LazyListScope.() -> Unit)? = null,
    items: LazyListScope.(List<T>) -> Unit
) {
    val data by paginatedState.data.collectAsStateWithLifecycle()
    val contentState by paginatedState.contentState.collectAsStateWithLifecycle()
    val isLoadingMore by paginatedState.isLoadingMore.collectAsStateWithLifecycle()
    val hasMore by paginatedState.hasMore.collectAsStateWithLifecycle()

    // Set up scroll-based pagination
    OnScrollLoadMore(
        listState = listState,
        hasMore = hasMore,
        isLoadingMore = isLoadingMore,
        onLoadMore = { paginatedState.loadMore() }
    )

    BaseContainer(
        isEmpty = data.isEmpty(),
        state = contentState,
        onRetry = { paginatedState.refresh() },
        empty = emptyContent
    ) {
        LazyColumn(
            modifier = modifier,
            state = listState,
            contentPadding = contentPadding,
            verticalArrangement = verticalArrangement,
            horizontalAlignment = horizontalAlignment
        ) {
            // Optional header
            header?.invoke(this)

            // Items
            items(data)

            // Pagination footer
            pagination(
                isLoadingMore = isLoadingMore,
                message = if (hasMore) loadMoreMessage else endMessage
            )
        }
    }
}
