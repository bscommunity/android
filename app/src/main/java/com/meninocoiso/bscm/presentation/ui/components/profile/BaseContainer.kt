package com.meninocoiso.bscm.presentation.ui.components.profile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.CircularProgressIndicator
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
import com.meninocoiso.bscm.presentation.ui.components.StatusMessageUI
import com.meninocoiso.bscm.presentation.ui.components.preview.ChartPreview

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
                isRefreshing = state is ContentState.Loading,
                onRefresh = onRetry
            ) {
                content()
            }
        }
    }
}

fun LazyListScope.contentList(
    items: List<CatalogItem>
) {
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
                    onPress = { /* Navigate to chart details */ }
                )
            }

            else -> { /* Handle other content types if necessary */
            }
        }
    }
}