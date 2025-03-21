package com.meninocoiso.beatstarcommunity.presentation.screens.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meninocoiso.beatstarcommunity.R
import com.meninocoiso.beatstarcommunity.presentation.ui.components.StatusMessageUI
import com.meninocoiso.beatstarcommunity.presentation.viewmodel.ChartDetailsViewModel
import com.meninocoiso.beatstarcommunity.presentation.viewmodel.DetailsState

@Composable
fun ChartDetailsRoute(
    chartId: String?,
    onReturn : () -> Unit,
    viewModel: ChartDetailsViewModel = hiltViewModel()
) {
    val chartState by viewModel.chart.collectAsStateWithLifecycle()

    // If we don't have a chart from typed navigation, fetch it using chartId
    LaunchedEffect(chartId) {
        viewModel.fetchChartById(chartId)
    }

    when (chartState) {
        is DetailsState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        is DetailsState.Success -> {
            ChartDetailsScreen(
                chart = (chartState as DetailsState.Success).chart,
                onReturn = onReturn
            )
        }
        is DetailsState.Error -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                ,
                contentAlignment = Alignment.Center
            ) {
                StatusMessageUI(
                    title = "Failed to load chart details",
                    icon = R.drawable.rounded_error_24,
                    message = (chartState as DetailsState.Error).message
                        ?: "An error occurred while loading the chart details",
                    buttonLabel = "Retry",
                    onClick = {
                        viewModel.fetchChartById(chartId)
                    }
                )
            }
        }
    }
}
