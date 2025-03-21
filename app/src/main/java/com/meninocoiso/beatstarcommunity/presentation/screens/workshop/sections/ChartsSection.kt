package com.meninocoiso.beatstarcommunity.presentation.screens.workshop.sections

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meninocoiso.beatstarcommunity.R
import com.meninocoiso.beatstarcommunity.presentation.screens.details.OnNavigateToDetails
import com.meninocoiso.beatstarcommunity.presentation.ui.components.StatusMessageUI
import com.meninocoiso.beatstarcommunity.presentation.ui.components.chart.ChartPreview
import com.meninocoiso.beatstarcommunity.presentation.ui.components.layout.SectionWrapper
import com.meninocoiso.beatstarcommunity.presentation.ui.modifiers.fabScrollObserver
import com.meninocoiso.beatstarcommunity.presentation.viewmodel.ChartViewModel
import com.meninocoiso.beatstarcommunity.presentation.viewmodel.ChartsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChartsSection(
    nestedScrollConnection: NestedScrollConnection,
    onNavigateToDetails: OnNavigateToDetails,
    onFabStateChange: (Boolean) -> Unit,
    onSnackbar: (String) -> Unit,
    viewModel: ChartViewModel = hiltViewModel()
) {
    val chartsState by viewModel.charts.collectAsStateWithLifecycle()

    // Extract charts from the current state
    val charts = when (chartsState) {
        is ChartsState.Success -> (chartsState as ChartsState.Success).charts
        is ChartsState.Loading -> (chartsState as ChartsState.Loading).previousCharts
        is ChartsState.Error -> (chartsState as ChartsState.Error).previousCharts
    } ?: emptyList()

    // Show error message if in error state
    LaunchedEffect(chartsState) {
        if (chartsState is ChartsState.Error) {
            if (charts.isNotEmpty()) {
                onSnackbar("Failed to fetch new data")
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
            when (chartsState) {
                is ChartsState.Loading -> {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        CircularProgressIndicator(Modifier.width(36.dp))
                    }
                }

                is ChartsState.Error -> {
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
                isRefreshing = chartsState is ChartsState.Loading,
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
                ) {
                    items(charts) { chart ->
                        ChartPreview(
                            chart = chart,
                            onNavigateToDetails = {
                                onNavigateToDetails(chart)
                            },
                        )
                    }
                }
            }
        }
    }
}