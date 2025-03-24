package com.meninocoiso.beatstarcommunity.presentation.screens.workshop.sections

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meninocoiso.beatstarcommunity.R
import com.meninocoiso.beatstarcommunity.presentation.screens.details.OnNavigateToDetails
import com.meninocoiso.beatstarcommunity.presentation.ui.components.StatusMessageUI
import com.meninocoiso.beatstarcommunity.presentation.ui.components.chart.ChartPreview
import com.meninocoiso.beatstarcommunity.presentation.ui.components.layout.SectionWrapper
import com.meninocoiso.beatstarcommunity.presentation.ui.modifiers.fabScrollObserver
import com.meninocoiso.beatstarcommunity.presentation.viewmodel.WorkshopViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChartsSection(
    nestedScrollConnection: NestedScrollConnection,
    onNavigateToDetails: OnNavigateToDetails,
    onFabStateChange: (Boolean) -> Unit,
    onSnackbar: (String) -> Unit,
    viewModel: WorkshopViewModel = hiltViewModel()
) {
    val charts by viewModel.charts.collectAsStateWithLifecycle(initialValue = emptyList())
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    // Collect events for snackbar
    LaunchedEffect(error) {
        error?.let { errorMessage ->
            if (charts.isNotEmpty()) {
                onSnackbar(errorMessage)
            }
        }
    }

    // If no charts and loading, show loading indicator
    if (charts.isEmpty()) {
        when {
            isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.width(36.dp))
                }
            }
            error != null -> {
                StatusMessageUI(
                    title = "Looks like something went wrong...",
                    message = error ?: "Unknown error",
                    icon = R.drawable.rounded_emergency_home_24,
                    onClick = { viewModel.refresh() },
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                StatusMessageUI(
                    title = "No charts available",
                    message = "Pull to refresh or check your connection",
                    icon = R.drawable.rounded_emergency_home_24,
                    onClick = { viewModel.refresh() },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    } else {
        PullToRefreshBox(
            isRefreshing = isLoading,
            onRefresh = { viewModel.refresh() }
        ) {
            SectionWrapper(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(nestedScrollConnection)
                    .fabScrollObserver { shouldExtend ->
                        onFabStateChange(shouldExtend)
                    }
            ) {
                items(charts) { chart ->
                    ChartPreview(
                        chart = chart,
                        onNavigateToDetails = {
                            onNavigateToDetails(chart)
                        }
                    )
                }
            }
        }
    }
}