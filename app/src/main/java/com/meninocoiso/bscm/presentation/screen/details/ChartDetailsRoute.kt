package com.meninocoiso.bscm.presentation.screen.details

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.presentation.ui.components.StatusMessageUI
import com.meninocoiso.bscm.presentation.viewmodel.ChartDetailsViewModel
import com.meninocoiso.bscm.presentation.viewmodel.DetailsState

@Composable
fun ChartDetailsRoute(
    chartId: String?,
    onReturn: () -> Unit,
    viewModel: ChartDetailsViewModel = hiltViewModel()
) {
    val chartState by viewModel.chart.collectAsStateWithLifecycle()

    // If we don't have a chart from typed navigation, fetch it using chartId
    LaunchedEffect(chartId) {
        viewModel.fetchChartById(chartId)
    }
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) { it
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when (chartState) {
                is DetailsState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                is DetailsState.Success -> {
                    ChartDetailsScreen(
                        chart = (chartState as DetailsState.Success).chart,
                        onReturn = onReturn
                    )
                }

                is DetailsState.Error -> {
                    StatusMessageUI(
                        title = stringResource(R.string.failed_to_load_chart_details),
                        message = (chartState as DetailsState.Error).message
                            ?: stringResource(R.string.failed_to_load_chart_details_description),
                        icon = R.drawable.rounded_error_24,
                        onClick = {
                            viewModel.fetchChartById(chartId)
                        },
                        buttonLabel = stringResource(R.string.retry),
                    )

                }
            }
        }
    }
}
