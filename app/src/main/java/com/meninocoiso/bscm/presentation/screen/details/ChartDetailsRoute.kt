package com.meninocoiso.bscm.presentation.screen.details

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.domain.model.Chart
import com.meninocoiso.bscm.domain.result.ContentResult
import com.meninocoiso.bscm.presentation.ui.components.Loading
import com.meninocoiso.bscm.presentation.ui.components.RouteUI
import com.meninocoiso.bscm.presentation.ui.components.StatusMessageUI
import com.meninocoiso.bscm.presentation.viewmodel.ChartDetailsViewModel
import com.meninocoiso.bscm.presentation.ui.utils.asString

@Composable
fun ChartDetailsRoute(
    id: String?,
    onReturn: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: ChartDetailsViewModel = hiltViewModel()
) {
    val state by viewModel.chart.collectAsStateWithLifecycle()

    // If we don't have a chart from typed navigation, fetch it using id
    LaunchedEffect(id) {
        viewModel.fetchChartById(id)
    }

    RouteUI {
        when (state) {
            is ContentResult.Loading -> { Loading() }

            is ContentResult.Success -> {
                ChartDetailsScreen(
                    chart = (state as ContentResult.Success<Chart>).data,
                    onReturn = onReturn,
                    onNavigateToSettings = onNavigateToSettings,
                )
            }

            is ContentResult.Error -> {
                StatusMessageUI(
                    title = stringResource(R.string.failed_to_load_chart_details),
                    message = (state as ContentResult.Error).message.asString(),
                    icon = R.drawable.rounded_error_24,
                    onClick = {
                        viewModel.fetchChartById(id)
                    },
                    buttonLabel = stringResource(R.string.retry),
                )
            }
        }
    }
}
