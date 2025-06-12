package com.meninocoiso.bscm.presentation.ui.components.updates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.data.manager.ChartState
import com.meninocoiso.bscm.domain.model.Chart
import com.meninocoiso.bscm.presentation.ui.components.Size
import com.meninocoiso.bscm.presentation.ui.components.StatusMessageUI
import com.meninocoiso.bscm.presentation.ui.components.layout.Section
import com.meninocoiso.bscm.presentation.ui.components.layout.SectionWrapper
import com.meninocoiso.bscm.presentation.ui.modifiers.fabScrollObserver
import com.meninocoiso.bscm.presentation.ui.modifiers.shimmerLoading
import com.meninocoiso.bscm.presentation.viewmodel.ContentViewModel
import com.meninocoiso.bscm.service.DownloadEvent

private object SectionWrapperDefaults {
    val contentPadding = PaddingValues(horizontal = 16.dp)
    val verticalArrangement = Arrangement.spacedBy(8.dp)
    val horizontalAlignment = Alignment.CenterHorizontally
}

@Composable
fun UpdatesSection(
    state: ChartState,
    charts: List<Chart>,
    onFetchUpdates: () -> Unit,
    onSnackbar: (String) -> Unit,
    onFabStateChange: (Boolean) -> Unit,
    nestedScrollConnection: NestedScrollConnection,
    contentViewModel: ContentViewModel = hiltViewModel(),
) {
    val itemsUpdating = remember { mutableStateListOf<String>() }
    
    LaunchedEffect(Unit) {
        contentViewModel.events.collect { event ->
            when (event) {
                is DownloadEvent.Complete -> {
                    itemsUpdating.remove(event.chartId)
                    onSnackbar("Update complete")
                }
                is DownloadEvent.Error -> onSnackbar("Error: ${event.message}")
                else -> {}
            }
        }
    }

    Section(
        title = if (charts.isNotEmpty()) {
            "Updates available (${charts.size})"
        } else {
            "Updates"
        },
        thickness = 0.dp,
        titleModifier = Modifier.padding(start = 16.dp, bottom = 12.dp),
        modifier = Modifier.padding(bottom = 16.dp)
    ) {
        when (state) {
            is ChartState.Error -> {
                UpdatesPanel {
                    StatusMessageUI(
                        title = "We couldn't fetch updates...",
                        message = "Please check your connection and try again later",
                        icon = R.drawable.rounded_hourglass_disabled_24,
                        size = Size.Small,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            is ChartState.Loading -> {
                UpdatesPanel {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(21.5.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .shimmerLoading(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surfaceContainerLow,
                                    MaterialTheme.colorScheme.surfaceContainerHigh,
                                    MaterialTheme.colorScheme.surfaceContainerLow
                                )
                            )
                    )
                }
            }
            is ChartState.Success -> {
                if (charts.isEmpty()) {
                    UpdatesPanel {
                        Text(text = "No updates available")
                    }
                } else {
                    SectionWrapper(
                        modifier = Modifier
                            .nestedScroll(nestedScrollConnection)
                            .fabScrollObserver(onFabStateChange),
                        contentPadding = SectionWrapperDefaults.contentPadding,
                        verticalArrangement = SectionWrapperDefaults.verticalArrangement,
                        horizontalAlignment = SectionWrapperDefaults.horizontalAlignment
                    ) {
                        items(charts) { chart ->
                            val contentState by contentViewModel.getContentState(chart.id)
                                .collectAsStateWithLifecycle()

                            UpdateListItem(
                                chart = chart,
                                onUpdateClick = {
                                    itemsUpdating.add(chart.id)
                                    contentViewModel.downloadChart(chart)
                                },
                                contentState = contentState
                            )
                        }
                    }
                }
            }
        }
        UpdatesButton(
            isLoading = state is ChartState.Loading,
            isDisabled = itemsUpdating.isNotEmpty(),
            onFetchUpdates = onFetchUpdates
        )
    }
}