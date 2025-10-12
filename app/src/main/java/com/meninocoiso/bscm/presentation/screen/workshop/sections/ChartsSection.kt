package com.meninocoiso.bscm.presentation.screen.workshop.sections

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.data.manager.ChartState
import com.meninocoiso.bscm.data.manager.FetchEvent
import com.meninocoiso.bscm.presentation.screen.details.OnNavigateToDetails
import com.meninocoiso.bscm.presentation.ui.components.StatusMessageUI
import com.meninocoiso.bscm.presentation.ui.components.preview.ChartPreview
import com.meninocoiso.bscm.presentation.ui.components.layout.SectionWrapper
import com.meninocoiso.bscm.presentation.ui.components.workshop.WorkshopChips
import com.meninocoiso.bscm.presentation.ui.modifiers.fabScrollObserver
import com.meninocoiso.bscm.presentation.viewmodel.WorkshopViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChartsSection(
    nestedScrollConnection: NestedScrollConnection,
    listState: LazyListState,
    onNavigateToDetails: OnNavigateToDetails,
    onFabStateChange: (Boolean) -> Unit,
    onSnackbar: (String) -> Unit,
    viewModel: WorkshopViewModel
) {
    val context = LocalContext.current

    val searchCharts by viewModel.searchCharts.collectAsStateWithLifecycle(initialValue = emptyList())
    val feedCharts by viewModel.feedCharts.collectAsStateWithLifecycle(initialValue = emptyList())

    var searchFieldState by remember { mutableStateOf(viewModel.searchFieldState) }
    val hasActiveQuery = searchFieldState.text.isNotEmpty()

    val charts = if (hasActiveQuery) {
        searchCharts
    } else {
        feedCharts
    }

    val workshopState by viewModel.workshopState.collectAsStateWithLifecycle(initialValue = ChartState.Loading)

    // Collect events for snackbar
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is FetchEvent.Error -> {
                    println("Triggering snackbar: ${event.message}")
                    onSnackbar(event.message)
                }
            }
        }
    }

    val isExplicitAllowed =
        viewModel.isExplicitAllowed.collectAsStateWithLifecycle(initialValue = false)

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        // If no cache or on search mode, show loading or error status on full page
        if (feedCharts.isEmpty() || hasActiveQuery && workshopState !is ChartState.Success) {
            when (workshopState) {
                is ChartState.Loading -> {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        CircularProgressIndicator(Modifier.size(36.dp))
                    }
                }

                is ChartState.Error -> {
                    StatusMessageUI(
                        title = stringResource(R.string.something_went_wrong),
                        message = stringResource(R.string.check_connection),
                        icon = R.drawable.rounded_emergency_home_24,
                        onClick = { viewModel.fetchFeedCharts() },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                else -> {}
            }
        } else if (hasActiveQuery && searchCharts.isEmpty()) {
            // No charts to display - show empty state
            StatusMessageUI(
                title = stringResource(R.string.no_charts_found),
                message = stringResource(R.string.no_charts_found_description),
                icon = R.drawable.outline_filter_alt_24,
                onClick = { viewModel.clearSearch() },
                buttonLabel = stringResource(R.string.clear_search),
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // We have charts to display - show them with pull-to-refresh
            PullToRefreshBox(
                isRefreshing = workshopState is ChartState.Loading,
                onRefresh = { viewModel.fetchFeedCharts() }
            ) {
                SectionWrapper(
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(nestedScrollConnection)
                        .fabScrollObserver { shouldExtend ->
                            // Update FAB state based on scroll delta
                            onFabStateChange(shouldExtend)
                        },
                    listState = listState,
                ) {
                    // Add WorkshopChips as the first item in the list
                    item {
                        WorkshopChips(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .zIndex(1f), // Lower z-index since it's now part of the scrollable content
                            currentSortOption = viewModel.currentSortOption,
                            onSortOptionChange = viewModel::changeSortOption
                        )
                    }

                    itemsIndexed(charts) { index, chart ->
                        ChartPreview(
                            chart = chart,
                            isDisabled = chart.latestVersion.isExplicit && !isExplicitAllowed.value,
                            onDisabled = {
                                onSnackbar(context.getString(R.string.explicit_content_disabled))
                            },
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
                    } else {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.workshop_feed_end),
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}