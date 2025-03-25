package com.meninocoiso.beatstarcommunity.presentation.screens.updates.sections

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meninocoiso.beatstarcommunity.data.manager.ChartsState
import com.meninocoiso.beatstarcommunity.presentation.screens.details.OnNavigateToDetails
import com.meninocoiso.beatstarcommunity.presentation.ui.components.local.LocalContentSection
import com.meninocoiso.beatstarcommunity.presentation.ui.components.updates.UpdatesSection
import com.meninocoiso.beatstarcommunity.presentation.viewmodel.UpdatesViewModel

@Composable
internal fun WorkshopSection(
    viewModel: UpdatesViewModel,
    onNavigateToDetails: OnNavigateToDetails,
    onSnackbar: (String) -> Unit,
    onFabStateChange: (Boolean) -> Unit,
    nestedScrollConnection: NestedScrollConnection,
) {
    // Collect the direct flows as states
    val updatesCharts by viewModel.updatesAvailable.collectAsStateWithLifecycle(initialValue = emptyList())
    val localCharts by viewModel.localCharts.collectAsStateWithLifecycle(initialValue = emptyList())

    Column(
        modifier = Modifier
            .padding(top = 24.dp)
            .fillMaxSize()
    ) {
        if (localCharts.isNotEmpty()) {
            // Convert the list to a ChartsState for compatibility with existing components
            val updatesState = ChartsState.Success(updatesCharts)

            UpdatesSection(
                updatesState = updatesState,
                onFetchUpdates = { chartToRemove ->
                    viewModel.checkForUpdates()
                },
                onLocalContentUpdate = { /*viewModel.refresh()*/ },
                onSnackbar = onSnackbar,
                onFabStateChange = onFabStateChange,
                nestedScrollConnection = nestedScrollConnection,
            )
        }

        // Convert the list to a ChartsState for compatibility with existing components
        val localChartsState = ChartsState.Success(localCharts)

        LocalContentSection(
            localChartsState = localChartsState,
            onNavigateToDetails = onNavigateToDetails,
            onFabStateChange = onFabStateChange,
            nestedScrollConnection = nestedScrollConnection,
        )
    }
}