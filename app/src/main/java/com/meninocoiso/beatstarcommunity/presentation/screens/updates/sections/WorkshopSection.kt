package com.meninocoiso.beatstarcommunity.presentation.screens.updates.sections

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meninocoiso.beatstarcommunity.presentation.screens.details.OnNavigateToDetails
import com.meninocoiso.beatstarcommunity.presentation.ui.components.local.LocalContentSection
import com.meninocoiso.beatstarcommunity.presentation.ui.components.updates.UpdatesSection
import com.meninocoiso.beatstarcommunity.presentation.viewmodel.LocalChartsState
import com.meninocoiso.beatstarcommunity.presentation.viewmodel.UpdatesViewModel

@Composable
internal fun WorkshopSection(
    viewModel: UpdatesViewModel,
    onNavigateToDetails: OnNavigateToDetails,
    onSnackbar: (String) -> Unit,
    onFabStateChange: (Boolean) -> Unit,
    nestedScrollConnection: NestedScrollConnection,
) {
    val updatesState by viewModel.updatesState.collectAsState()
    val localChartsState by viewModel.localChartsState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        // Update local charts if needed
        if (localChartsState is LocalChartsState.Success) {
            viewModel.loadLocalCharts()
        }
    }

    Column(
        modifier = Modifier.padding(top = 24.dp).fillMaxSize()
    ) {
        UpdatesSection(
            updatesState = updatesState,
            onFetchUpdates = { viewModel.fetchUpdates(null, it) },
            onLocalContentUpdate = { viewModel.loadLocalCharts() },
            onSnackbar = onSnackbar,
            onFabStateChange = onFabStateChange,
            nestedScrollConnection = nestedScrollConnection,
        )
        LocalContentSection(
            localChartsState = localChartsState,
            onNavigateToDetails = onNavigateToDetails,
            onFabStateChange = onFabStateChange,
            nestedScrollConnection = nestedScrollConnection,
        )
    }
}