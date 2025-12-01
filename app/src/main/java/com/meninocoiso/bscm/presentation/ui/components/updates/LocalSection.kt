package com.meninocoiso.bscm.presentation.ui.components.updates

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.data.manager.ChartState
import com.meninocoiso.bscm.domain.model.Chart
import com.meninocoiso.bscm.presentation.ui.components.layout.Section
import com.meninocoiso.bscm.presentation.ui.components.preview.ChartPreview

fun LazyListScope.localContentSection(
    state: ChartState,
    charts: List<Chart>,
    onNavigateToDetails: (Chart) -> Unit,
) {
    item {
        Section(
            title = when (state) {
                is ChartState.Success -> {
                    if (charts.isNotEmpty()) {
                        stringResource(R.string.downloaded, charts.size)
                    } else null
                }
                else -> null
            },
            thickness = when (state) {
                is ChartState.Success -> if (charts.isNotEmpty()) 1.dp else 0.dp
                else -> 0.dp
            },
        ) {
            when (state) {
                is ChartState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                    }
                }

                else -> {
                    // TODO: Implement other content types
                    // SegmentedButtonUI()

                    Text(
                        text = stringResource(R.string.charts),
                        modifier = Modifier.padding(start = 32.dp, bottom = 8.dp),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }

    if (state !is ChartState.Loading && charts.isNotEmpty()) {
        items(charts) { chart ->
            ChartPreview(
                chart = chart,
                onNavigateToDetails = { onNavigateToDetails(chart) },
                showLocalWarning = chart.isLocalPlaceholder,
                isLocal = true,
            )
        }

        item {
            Spacer(modifier = Modifier.padding(bottom = 24.dp))
        }
        
        /*item { LocalDownloadsSectionTitle("Tour Passes") }*/
        /*item { LocalDownloadsSectionTitle("Themes") }*/
    }
}