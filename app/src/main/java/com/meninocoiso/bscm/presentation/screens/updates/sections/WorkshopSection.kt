package com.meninocoiso.bscm.presentation.screens.updates.sections

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meninocoiso.bscm.presentation.screens.details.OnNavigateToDetails
import com.meninocoiso.bscm.presentation.ui.components.local.LocalContentSection
import com.meninocoiso.bscm.presentation.ui.components.updates.UpdatesSection
import com.meninocoiso.bscm.presentation.viewmodel.UpdatesViewModel

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

    val cacheState by viewModel.cacheState.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .padding(top = 24.dp)
            .fillMaxSize()
    ) {
        if (localCharts.isNotEmpty()) {
            UpdatesSection(
                state = updateState,
                charts = updatesCharts,
                onFetchUpdates = {
                    viewModel.checkForUpdates()
                },
                onSnackbar = onSnackbar,
                onFabStateChange = onFabStateChange,
                nestedScrollConnection = nestedScrollConnection,
            )
        }

        LocalContentSection(
            state = cacheState,
            charts = localCharts,
            onNavigateToDetails = onNavigateToDetails,
            onFabStateChange = onFabStateChange,
            nestedScrollConnection = nestedScrollConnection,
        )
    }
}