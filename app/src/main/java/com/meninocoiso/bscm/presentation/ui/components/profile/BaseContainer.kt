package com.meninocoiso.bscm.presentation.ui.components.profile

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import com.meninocoiso.bscm.presentation.ui.components.StatusMessageUI
import com.meninocoiso.bscm.presentation.ui.components.preview.ChartPreview
import com.meninocoiso.bscm.util.DateUtils
import com.meninocoiso.bscm.util.DateUtils.DateFormat

@Composable
fun BaseContainer(
    isEmpty: Boolean,
    state: ContentState,
    onRetry: () -> Unit,
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
                        title = stringResource(R.string.something_went_wrong),
                        message = stringResource(R.string.check_connection),
                        icon = R.drawable.rounded_emergency_home_24,
                        onClick = onRetry
                    )
                }

                else -> empty()
            }
        }

        else -> {
            PullToRefreshBox(
                modifier = Modifier.fillMaxSize(),
                isRefreshing = state is ContentState.Loading,
                onRefresh = onRetry
            ) {
                content()
            }
        }
    }
}

fun LazyListScope.pagination(
    isLoadingMore: Boolean,
    message: String
) {
    if (isLoadingMore) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 36.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        }
    } else {
        item {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = message,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

fun LazyListScope.contentList(
    items: List<CatalogItem>,
    onNavigateToDetails: OnNavigateToDetails,
    context: Context? = null,
    vararg formats: DateFormat = arrayOf(DateFormat.DAY)
) {
    val groupedItems = if (context != null) DateUtils.groupItemsByDate(
        context = context,
        items = items,
        getDate = { it.createdAt.toString() },
        *formats
    ) else null

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