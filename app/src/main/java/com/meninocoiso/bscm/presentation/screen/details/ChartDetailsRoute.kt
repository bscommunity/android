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
import com.meninocoiso.bscm.domain.model.Chart
import com.meninocoiso.bscm.domain.result.ContentResult
import com.meninocoiso.bscm.presentation.ui.components.StatusMessageUI
import com.meninocoiso.bscm.presentation.viewmodel.ChartDetailsViewModel

@Composable
fun ChartDetailsRoute(
    contentId: String?,
    onReturn: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: ChartDetailsViewModel = hiltViewModel()
) {
    val state by viewModel.chart.collectAsStateWithLifecycle()

    // If we don't have a chart from typed navigation, fetch it using contentId
    LaunchedEffect(contentId) {
        viewModel.fetchChartById(contentId)
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
            when (state) {
                is ContentResult.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                is ContentResult.Success -> {
                    ChartDetailsScreen(
                        chart = (state as ContentResult.Success<Chart>).data,
                        onReturn = onReturn,
                        onNavigateToSettings = onNavigateToSettings
                    )
                }

                is ContentResult.Error -> {
                    StatusMessageUI(
                        title = stringResource(R.string.failed_to_load_chart_details),
                        message = (state as ContentResult.Error).message
                            ?: stringResource(R.string.failed_to_load_chart_details_description),
                        icon = R.drawable.rounded_error_24,
                        onClick = {
                            viewModel.fetchChartById(contentId)
                        },
                        buttonLabel = stringResource(R.string.retry),
                    )

                }
            }
        }
    }
}
