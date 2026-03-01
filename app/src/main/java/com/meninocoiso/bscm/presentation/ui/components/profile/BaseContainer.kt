package com.meninocoiso.bscm.presentation.ui.components.profile

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.domain.model.CatalogItem
import com.meninocoiso.bscm.domain.model.Chart
import com.meninocoiso.bscm.domain.result.ContentState
import com.meninocoiso.bscm.presentation.screen.details.OnNavigateToDetails
import com.meninocoiso.bscm.presentation.ui.components.StatusMessageSize
import com.meninocoiso.bscm.presentation.ui.components.StatusMessageUI
import com.meninocoiso.bscm.presentation.ui.components.preview.ChartPreview
import com.meninocoiso.bscm.util.DateUtils.DateFormat

@Composable
fun BaseContainer(
    isEmpty: Boolean,
    state: ContentState,
    modifier: Modifier = Modifier,
    pullToRefreshState: PullToRefreshState = rememberPullToRefreshState(),
    isRefreshing: Boolean = false,
    onRetry: (reset: Boolean) -> Unit,
    empty: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    when {
        isEmpty -> {
            when (state) {
                ContentState.Loading -> {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        CircularProgressIndicator(Modifier.size(24.dp))
                    }
                }

                ContentState.Error -> {
                    StatusMessageUI(
                        modifier = Modifier.fillMaxSize(),
                        size = StatusMessageSize.Small,
                        title = stringResource(R.string.something_went_wrong),
                        message = stringResource(R.string.check_connection),
                        icon = R.drawable.rounded_emergency_home_24,
                        onClick = { onRetry(true) }
                    )
                }

                else -> empty()
            }
        }

        else -> {
            PullToRefreshBox(
                modifier = modifier,
                state = pullToRefreshState,
                isRefreshing = isRefreshing,
                onRefresh = { onRetry(false) }
            ) {
                content()
            }
        }
    }
}

@Composable
private fun PaginationLoadingIndicator(
    isLoadingMore: Boolean,
    message: String
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (isLoadingMore) {
            CircularProgressIndicator()
        } else if (message.isNotEmpty()) {
            Text(
                text = message,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

fun LazyListScope.pagination(
    isLoadingMore: Boolean,
    showMessage: Boolean?
) {
    item {
        val message = stringResource(R.string.end_of_list)
        PaginationLoadingIndicator(
            isLoadingMore = isLoadingMore,
            message = if (showMessage == true) message else ""
        )
    }
}

fun LazyGridScope.pagination(
    isLoadingMore: Boolean,
    showMessage: Boolean?
) {
    item(span = { GridItemSpan(maxLineSpan) }) {
        val message = stringResource(R.string.end_of_list)
        PaginationLoadingIndicator(
            isLoadingMore = isLoadingMore,
            message = if (showMessage == true) message else ""
        )
    }
}

fun LazyListScope.contentList(
    items: List<CatalogItem>,
    onNavigateToDetails: OnNavigateToDetails,
    context: Context? = null,
    vararg formats: DateFormat = arrayOf(DateFormat.DAY)
) {
    /*val groupedItems = if (context != null) DateUtils.groupItemsByDate(
        context = context,
        items = items,
        getDate = { it.createdAt.toString() },
        *formats
    ) else null*/

    items(items.size) { index ->
        when (val item = items[index]) {
            is Chart -> {
                ChartPreview(
                    modifier = Modifier.padding(
                        start = 16.dp,
                        end = 16.dp,
                        bottom = 12.dp
                    ),
                    chart = item,
                    isSecondary = true,
                    onPress = { onNavigateToDetails(item) }
                )
            }

            else -> { /* Handle other content types if necessary */
            }
        }
    }
}