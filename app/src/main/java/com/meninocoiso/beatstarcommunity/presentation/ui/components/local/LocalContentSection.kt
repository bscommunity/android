package com.meninocoiso.beatstarcommunity.presentation.ui.components.local

import androidx.compose.foundation.layout.Arrangement
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
import com.meninocoiso.beatstarcommunity.data.manager.ChartState
import com.meninocoiso.beatstarcommunity.domain.model.Chart
import com.meninocoiso.beatstarcommunity.presentation.ui.components.StatusMessageUI
import com.meninocoiso.beatstarcommunity.presentation.ui.components.chart.LocalChartPreview
import com.meninocoiso.beatstarcommunity.presentation.ui.components.layout.Section
import com.meninocoiso.beatstarcommunity.presentation.ui.components.layout.SectionWrapper
import com.meninocoiso.beatstarcommunity.presentation.ui.modifiers.fabScrollObserver

@Composable
fun LocalContentSection(
    state: ChartState,
    charts: List<Chart>,
    onNavigateToDetails: (Chart) -> Unit,
    onFabStateChange: (Boolean) -> Unit,
    nestedScrollConnection: NestedScrollConnection,
) {
    // var selectedIndex by remember { mutableIntStateOf(-1) }
    // val options = listOf("Charts", "Tour Passes", "Themes")

    Section(
        title = when (state) {
            is ChartState.Success -> {
                if (charts.isNotEmpty()) {
                    "Downloaded (${charts.size})"
                } else {
                    null
                }
            }
            else -> null
        },
        thickness = when(state) {
            is ChartState.Success -> {
                if (charts.isNotEmpty()) 1.dp else 0.dp
            }
            else -> 0.dp
        },
    ) {
        // TODO: Implement other content types
        // SegmentedButtonUI()

        if (charts.isEmpty()) {
            StatusMessageUI(
                title = "No downloads yet",
                message = "Download some charts to get started",
                icon = R.drawable.rounded_box_24,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 36.dp)
            )
        } else {
            when (state) {
                is ChartState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                    }
                }

                is ChartState.Error -> {
                    StatusMessageUI(
                        modifier = Modifier.fillMaxSize(),
                        title = "Looks like something went wrong...",
                        message = "Please reopen the app and try again",
                        icon = R.drawable.rounded_hourglass_disabled_24
                    )
                }

                else -> {
                    SectionWrapper(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .nestedScroll(nestedScrollConnection)
                            .fabScrollObserver(onFabStateChange),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ){
                        item {
                            LocalContentSectionTitle("Charts")
                        }
                        items(charts) { chart ->
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