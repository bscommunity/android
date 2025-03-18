package com.meninocoiso.beatstarcommunity.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meninocoiso.beatstarcommunity.R
import com.meninocoiso.beatstarcommunity.presentation.ui.components.StatusMessageUI
import com.meninocoiso.beatstarcommunity.presentation.ui.components.chart.ChartPreview
import com.meninocoiso.beatstarcommunity.presentation.ui.components.workspace.WorkspaceChips
import com.meninocoiso.beatstarcommunity.presentation.ui.components.workspace.WorkspaceTopBar
import com.meninocoiso.beatstarcommunity.presentation.ui.components.workspace.workspaceTabsItems
import com.meninocoiso.beatstarcommunity.presentation.ui.modifiers.fabScrollObserver
import com.meninocoiso.beatstarcommunity.presentation.viewmodel.ChartViewModel
import com.meninocoiso.beatstarcommunity.presentation.viewmodel.ChartsState
import com.meninocoiso.beatstarcommunity.util.AppBarUtils

private val SearchBarHeight = 80.dp
private val TabsHeight = 48.dp
private val WorkspaceChipsHeight = 56.dp

@Composable
fun WorkspaceScreen(
    onNavigateToDetails: OnNavigateToDetails,
    onFabStateChange: (Boolean) -> Unit,
    onSnackbar: (String) -> Unit
) {
    val horizontalPagerState = rememberPagerState {
        workspaceTabsItems.size
    }

    val (connection, spaceHeight, statusBarHeight) = AppBarUtils.getConnection(
        collapsableHeight = SearchBarHeight,
        fixedHeight = TabsHeight,
        bottomCollapsableHeight = WorkspaceChipsHeight,
    )

    Column {
        Column(
            Modifier
                .height(spaceHeight),
            verticalArrangement = Arrangement.Bottom,
        ) {
            WorkspaceChips(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(5f)
            )
        }

        HorizontalPager(
            state = horizontalPagerState,
            key = { it }, // Recompose the pager when the page changes
            beyondViewportPageCount = 1 // Keep the next page in memory
        ) { index ->
            when (
                index
            ) {
                0 -> ChartsSection(
                    connection,
                    onNavigateToDetails,
                    onFabStateChange,
                    onSnackbar
                )

                1 -> TourPassesSection(connection)
                2 -> ThemesSection(connection)
            }
        }
    }

    WorkspaceTopBar(
        connection = connection,
        appBarHeights = Triple(SearchBarHeight, TabsHeight, statusBarHeight),
        pagerState = horizontalPagerState,
    )
}

@Composable
private fun SectionWrapper(
    nestedScrollConnection: NestedScrollConnection,
    onFabStateChange: (Boolean) -> Unit,
    content: LazyListScope.() -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
            .fabScrollObserver { shouldExtend ->
                // Update FAB state based on scroll delta
                onFabStateChange(shouldExtend)
            },
    ) {
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartsSection(
    nestedScrollConnection: NestedScrollConnection,
    onNavigateToDetails: OnNavigateToDetails,
    onFabStateChange: (Boolean) -> Unit,
    onSnackbar: (String) -> Unit,
    viewModel: ChartViewModel = hiltViewModel()
) {
    val chartsState by viewModel.charts.collectAsStateWithLifecycle()
    /*val (cachedCharts, setCachedCharts) = remember { mutableStateOf(emptyList<Chart>()) }*/

    /*LaunchedEffect(chartsState) {
        println("ChartsState: $chartsState")
        when (chartsState) {
            is ChartsState.Success -> {
                setCachedCharts((chartsState as ChartsState.Success).charts)
            }
            is ChartsState.Error -> {
                // val errorMsg = (chartsState as ChartsState.Error).message ?: "Unknown error"
				if (cachedCharts.isNotEmpty()) {
                    onSnackbar(
						"Failed to fetch new data. Please check your connection and try again",
					)
				}
			}
			else -> {}
		}

        // Update local charts
        viewModel.updateLocalCharts()
	}*/

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        when(chartsState) {
            is ChartsState.Loading -> {
                // Show spinner if no cached data
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(Modifier.width(36.dp))
                }
            }

            is ChartsState.Success -> {
                // Show cached data and pull-to-refresh
                PullToRefreshBox(
                    isRefreshing = false,
                    onRefresh = { viewModel.refresh() }
                ) {
                    SectionWrapper(
                        nestedScrollConnection,
                        onFabStateChange
                    ) {
                        items((chartsState as ChartsState.Success).charts.flatMap { chart -> List(20) { chart } }) { chart ->
                            ChartPreview(
                                onNavigateToDetails = { onNavigateToDetails(chart) },
                                chart = chart
                            )
                        }
                    }
                }
            }

            is ChartsState.Error -> {
                // Display error with a retry option
                StatusMessageUI(
                    title = "Looks like something went wrong...",
                    message = "Please check your connection and try again",
                    icon = R.drawable.rounded_emergency_home_24,
                    onClick = { viewModel.refresh() },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun TourPassesSection(nestedScrollConnection: NestedScrollConnection) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        StatusMessageUI(
            title = "Work in progress!",
            message = "This feature still needs some work\nPlease, check back later",
            icon = R.drawable.rounded_hourglass_24,
        )
    }
}

@Composable
private fun ThemesSection(nestedScrollConnection: NestedScrollConnection) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        StatusMessageUI(
            title = "Work in progress!",
            message = "This feature still needs some work\nPlease, check back later",
            icon = R.drawable.rounded_hourglass_24,
        )
    }
}