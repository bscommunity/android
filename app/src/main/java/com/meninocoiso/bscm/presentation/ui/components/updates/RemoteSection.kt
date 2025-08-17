package com.meninocoiso.bscm.presentation.ui.components.updates

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.data.manager.ChartState
import com.meninocoiso.bscm.domain.model.Chart
import com.meninocoiso.bscm.presentation.ui.components.Size
import com.meninocoiso.bscm.presentation.ui.components.StatusMessageUI
import com.meninocoiso.bscm.presentation.ui.components.layout.Section
import com.meninocoiso.bscm.presentation.ui.modifiers.shimmerLoading
import com.meninocoiso.bscm.presentation.viewmodel.ContentViewModel

fun LazyListScope.remoteSection(
    state: ChartState,
    charts: List<Chart>,
    onFetchUpdates: () -> Unit,
    itemsUpdating: MutableList<String>,
    contentViewModel: ContentViewModel
) {
    item {
        Section(
            title = if (charts.isNotEmpty()) {
                stringResource(R.string.updates_available, charts.size)
            } else {
                null
            },
            thickness = 0.dp,
            titleModifier = Modifier.padding(start = 16.dp, bottom = 12.dp),
            modifier = Modifier.padding(top = 24.dp, bottom = 16.dp)
        ) {
            when (state) {
                is ChartState.Error -> {
                    UpdatesPanel {
                        StatusMessageUI(
                            title = stringResource(R.string.fetch_updates_error),
                            message = stringResource(R.string.check_connection),
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
                                .height(24.dp)
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
                            Text(text = stringResource(R.string.no_updates_available))
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

    if (state is ChartState.Success && charts.isNotEmpty()) {
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