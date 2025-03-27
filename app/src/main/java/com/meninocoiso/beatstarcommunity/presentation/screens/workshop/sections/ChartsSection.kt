package com.meninocoiso.beatstarcommunity.presentation.screens.workshop.sections

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meninocoiso.beatstarcommunity.R
import com.meninocoiso.beatstarcommunity.data.manager.ChartState
import com.meninocoiso.beatstarcommunity.data.manager.FetchEvent
import com.meninocoiso.beatstarcommunity.presentation.screens.details.OnNavigateToDetails
import com.meninocoiso.beatstarcommunity.presentation.ui.components.StatusMessageUI
import com.meninocoiso.beatstarcommunity.presentation.ui.components.chart.ChartPreview
import com.meninocoiso.beatstarcommunity.presentation.ui.modifiers.fabScrollObserver
import com.meninocoiso.beatstarcommunity.presentation.viewmodel.WorkshopViewModel
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChartsSection(
    nestedScrollConnection: NestedScrollConnection,
    onNavigateToDetails: OnNavigateToDetails,
    onFabStateChange: (Boolean) -> Unit,
    onSnackbar: (String) -> Unit,
    viewModel: WorkshopViewModel
) {
    val charts by viewModel.charts.collectAsStateWithLifecycle(initialValue = emptyList())
    val state by viewModel.state.collectAsStateWithLifecycle(initialValue = ChartState.Loading)
    
    // Collect events for snackbar
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is FetchEvent.Error -> {
                    onSnackbar(event.message)
                }
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        // Show different UI based on whether we have charts to display
        if (charts.isEmpty()) {
            // Empty state - show loading or error
            when (state) {
                is ChartState.Loading -> {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        CircularProgressIndicator(Modifier.width(36.dp))
                    }
                }

                is ChartState.Error -> {
                    StatusMessageUI(
                        title = "Looks like something went wrong...",
                        message = "Please check your connection and try again",
                        icon = R.drawable.rounded_emergency_home_24,
                        onClick = { viewModel.refresh() },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                else -> {}
            }
        } else {
            // We have charts to display - show them with pull-to-refresh
            PullToRefreshBox(
                isRefreshing = state is ChartState.Loading,
                onRefresh = { viewModel.refresh() }
            ) {
                SectionWrapper(
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(nestedScrollConnection)
                        .fabScrollObserver { shouldExtend ->
                            // Update FAB state based on scroll delta
                            onFabStateChange(shouldExtend)
                        },
                    onListEnd = {
                        // Connect to pagination function
                        viewModel.loadMoreCharts()
                    }
                ) {
                    items(charts) { chart ->
                        ChartPreview(
                            chart = chart,
                            onNavigateToDetails = {
                                onNavigateToDetails(chart)
                            },
                        )
                    }

                    if (viewModel.isLoadingMore) {
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
                    } /*else {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "End of charts",
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    }*/
                }
            }
        }
    }
}

@Composable
private fun SectionWrapper(
    modifier: Modifier = Modifier,
    onListEnd: (() -> Unit)? = null,
    content: LazyListScope.() -> Unit
) {
    val listState = rememberLazyListState()

    // Detect when user reaches end of list
    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0

            lastVisibleItem >= totalItems - 3 // Load more when 3 items from end
        }
            .distinctUntilChanged()
            .collect { isAtEnd ->
                if (isAtEnd) {
                    onListEnd?.invoke()
                }
            }
    }

    LazyColumn(
        state = listState,
        modifier = modifier,
        content = content
    )
}