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
import com.meninocoiso.bscm.domain.model.Chart
import com.meninocoiso.bscm.domain.result.ContentState
import com.meninocoiso.bscm.presentation.screen.details.OnNavigateToDetails
import com.meninocoiso.bscm.presentation.ui.components.layout.Section
import com.meninocoiso.bscm.presentation.ui.components.preview.ChartPreview

fun LazyListScope.localContentSection(
    state: ContentState,
    charts: List<Chart>,
    onNavigateToDetails: OnNavigateToDetails,
    onShowLocalItemDialog: () -> Unit,
) {
    item {
        Section(
            title = when (state) {
                is ContentState.Success -> {
                    if (charts.isNotEmpty()) {
                        stringResource(R.string.downloaded, charts.size)
                    } else null
                }

                else -> null
            },
            thickness = when (state) {
                is ContentState.Success -> if (charts.isNotEmpty()) 1.dp else 0.dp
                else -> 0.dp
            },
        ) {
            when (state) {
                is ContentState.Loading -> {
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

    if (state !is ContentState.Loading && charts.isNotEmpty()) {
        items(charts) { chart ->
            ChartPreview(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
                chart = chart,
                isSecondary = true,
                onPress = { onNavigateToDetails(chart) },
            )
        }

        item {
            Spacer(modifier = Modifier.padding(bottom = 24.dp))
        }

        /*item { LocalDownloadsSectionTitle("Tour Passes") }*/
        /*item { LocalDownloadsSectionTitle("Themes") }*/
    }
}