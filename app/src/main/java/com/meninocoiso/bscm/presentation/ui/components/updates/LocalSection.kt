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
import com.meninocoiso.bscm.domain.model.Theme
import com.meninocoiso.bscm.domain.model.TourPass
import com.meninocoiso.bscm.domain.result.ContentState
import com.meninocoiso.bscm.presentation.screen.details.OnNavigateToDetails
import com.meninocoiso.bscm.presentation.ui.components.SegmentedButtonUI
import com.meninocoiso.bscm.presentation.ui.components.layout.Section
import com.meninocoiso.bscm.presentation.ui.components.preview.ChartPreview
import com.meninocoiso.bscm.presentation.ui.components.preview.ThemePreview
import com.meninocoiso.bscm.presentation.ui.components.preview.TourPassPreview

/**
 * Segment indices used by the segmented control of the local section.
 * -1 means "no filter selected" (show everything).
 */
private const val FILTER_ALL = -1
private const val FILTER_CHARTS = 0
private const val FILTER_TOUR_PASSES = 1
private const val FILTER_THEMES = 2

fun LazyListScope.localContentSection(
    state: ContentState,
    charts: List<Chart>,
    onNavigateToDetails: OnNavigateToDetails,
    onShowLocalItemDialog: () -> Unit,
    tourPasses: List<TourPass> = emptyList(),
    themes: List<Theme> = emptyList(),
    selectedFilter: Int = FILTER_ALL,
    onFilterSelected: (Int) -> Unit = {},
) {
    val showAll = selectedFilter == FILTER_ALL
    val showCharts = showAll || selectedFilter == FILTER_CHARTS
    val showTourPasses = showAll || selectedFilter == FILTER_TOUR_PASSES
    val showThemes = showAll || selectedFilter == FILTER_THEMES

    val visibleCount = when {
        selectedFilter == FILTER_CHARTS -> charts.size
        selectedFilter == FILTER_TOUR_PASSES -> tourPasses.size
        selectedFilter == FILTER_THEMES -> themes.size
        else -> charts.size + tourPasses.size + themes.size
    }

    item {
        Section(
            title = when (state) {
                is ContentState.Success -> {
                    if (visibleCount > 0) {
                        stringResource(R.string.downloaded, visibleCount)
                    } else null
                }

                else -> null
            },
            thickness = when (state) {
                is ContentState.Success -> if (visibleCount > 0) 1.dp else 0.dp
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
                    SegmentedButtonUI(
                        options = listOf(
                            stringResource(R.string.charts),
                            stringResource(R.string.tour_passes),
                            stringResource(R.string.themes)
                        ),
                        selectedIndex = selectedFilter,
                        onSelected = onFilterSelected
                    )
                }
            }
        }
    }

    if (showCharts && state !is ContentState.Loading && charts.isNotEmpty()) {
        item {
            Text(
                text = stringResource(R.string.charts),
                modifier = Modifier.padding(start = 32.dp, bottom = 8.dp),
                style = MaterialTheme.typography.labelLarge
            )
        }

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
    }

    if (showTourPasses && tourPasses.isNotEmpty()) {
        item {
            Text(
                text = stringResource(R.string.tour_passes),
                modifier = Modifier.padding(start = 32.dp, top = 24.dp, bottom = 8.dp),
                style = MaterialTheme.typography.labelLarge
            )
        }

        items(tourPasses) { tourPass ->
            TourPassPreview(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
                tourPass = tourPass,
                isLocal = true,
                isSecondary = true,
                onPress = { onNavigateToDetails(tourPass) },
            )
        }

        item {
            Spacer(modifier = Modifier.padding(bottom = 24.dp))
        }
    }

    if (showThemes && themes.isNotEmpty()) {
        item {
            Text(
                text = stringResource(R.string.themes),
                modifier = Modifier.padding(start = 32.dp, top = 24.dp, bottom = 8.dp),
                style = MaterialTheme.typography.labelLarge
            )
        }

        items(themes) { theme ->
            ThemePreview(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
                theme = theme,
                isLocal = true,
                isSecondary = true,
                onPress = { onNavigateToDetails(theme) },
            )
        }

        item {
            Spacer(modifier = Modifier.padding(bottom = 24.dp))
        }
    }
}