package com.meninocoiso.beatstarcommunity.presentation.ui.components.local

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.meninocoiso.beatstarcommunity.R
import com.meninocoiso.beatstarcommunity.domain.model.Chart
import com.meninocoiso.beatstarcommunity.presentation.ui.components.StatusMessageUI
import com.meninocoiso.beatstarcommunity.presentation.ui.components.chart.LocalChartPreview
import com.meninocoiso.beatstarcommunity.presentation.ui.components.layout.Section
import com.meninocoiso.beatstarcommunity.presentation.ui.components.layout.SectionWrapper
import com.meninocoiso.beatstarcommunity.presentation.ui.modifiers.fabScrollObserver
import com.meninocoiso.beatstarcommunity.presentation.viewmodel.ChartsState

@Composable
fun LocalContentSection(
    localChartsState: ChartsState,
    onNavigateToDetails: (Chart) -> Unit,
    onFabStateChange: (Boolean) -> Unit,
    nestedScrollConnection: NestedScrollConnection,
) {
    // var selectedIndex by remember { mutableIntStateOf(-1) }
    // val options = listOf("Charts", "Tour Passes", "Themes")

    Section(
        title = when (localChartsState) {
            is ChartsState.Success -> {
                if (localChartsState.charts.isNotEmpty()) {
                    "Downloaded (${localChartsState.charts.size})"
                } else {
                    null
                }
            }
            else -> null
        },
        thickness = when(localChartsState) {
            is ChartsState.Success -> {
                if (localChartsState.charts.isNotEmpty()) 1.dp else 0.dp
            }
            else -> 0.dp
        },
    ) {
        // TODO: Implement other content types
        // SegmentedButtonUI()

        when (localChartsState) {
            is ChartsState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                }
            }
            is ChartsState.Error -> {
                StatusMessageUI(
                    modifier = Modifier.fillMaxSize(),
                    title = "Looks like something went wrong...",
                    message = "Please check your connection or try again later",
                    icon = R.drawable.rounded_hourglass_disabled_24
                )
            }
            is ChartsState.Success -> {
                val chartsList = localChartsState.charts
                if (chartsList.isEmpty()) {
                    StatusMessageUI(
                        title = "No downloads yet",
                        message = "Download some charts to get started",
                        icon = R.drawable.rounded_box_24,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 36.dp)
                    )
                } else {
                    SectionWrapper(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .nestedScroll(nestedScrollConnection)
                            .fabScrollObserver(onFabStateChange),
                    ){
                        item {
                            LocalContentSectionTitle("Charts")
                        }
                        items(chartsList) { chart ->
                            LocalChartPreview(
                                chart = chart,
                                onNavigateToDetails = { onNavigateToDetails(chart) }
                            )
                        }
                        /*item {
                            LocalDownloadsSectionTitle("Tour Passes")
                        }*/
                        /*item {
                            LocalDownloadsSectionTitle("Themes")
                        }*/
                    }
                }
            }
        }
    }
}