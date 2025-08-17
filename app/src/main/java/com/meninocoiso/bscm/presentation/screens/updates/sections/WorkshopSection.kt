package com.meninocoiso.bscm.presentation.screens.updates.sections

import DownloadEvent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.presentation.screens.details.OnNavigateToDetails
import com.meninocoiso.bscm.presentation.ui.components.StatusMessageUI
import com.meninocoiso.bscm.presentation.ui.components.updates.localContentSection
import com.meninocoiso.bscm.presentation.ui.components.updates.remoteSection
import com.meninocoiso.bscm.presentation.ui.modifiers.fabScrollObserver
import com.meninocoiso.bscm.presentation.viewmodel.ContentViewModel
import com.meninocoiso.bscm.presentation.viewmodel.UpdatesViewModel

@Composable
internal fun WorkshopSection(
    viewModel: UpdatesViewModel,
    onNavigateToDetails: OnNavigateToDetails,
    onSnackbar: (String) -> Unit,
    onFabStateChange: (Boolean) -> Unit,
    nestedScrollConnection: NestedScrollConnection,
    contentViewModel: ContentViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    // Collect the direct flows as states
    val updatesCharts by viewModel.updatesAvailable.collectAsStateWithLifecycle(initialValue = emptyList())
    val localCharts by viewModel.localCharts.collectAsStateWithLifecycle(initialValue = emptyList())

    val cacheState by viewModel.cacheState.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()

    val itemsUpdating = remember { mutableStateListOf<String>() }

    LaunchedEffect(Unit) {
        contentViewModel.events.collect { event ->
            when (event) {
                is DownloadEvent.Complete -> {
                    itemsUpdating.remove(event.chartId)
                    onSnackbar(context.getString(R.string.update_complete))
                }

                is DownloadEvent.Error -> onSnackbar(
                    context.getString(R.string.error, event.message)
                )

                else -> {}
            }
        }
    }

    if (updatesCharts.isNotEmpty() || localCharts.isNotEmpty()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .fabScrollObserver { shouldExtend ->
                    // Update FAB state based on scroll delta
                    onFabStateChange(shouldExtend)
                }
            ,
        ) {
            if (localCharts.isNotEmpty()) {
                remoteSection(
                    state = updateState,
                    charts = updatesCharts,
                    onFetchUpdates = { viewModel.checkForUpdates() },
                    itemsUpdating = itemsUpdating,
                    contentViewModel = contentViewModel,
                )
            }

            localContentSection(
                state = cacheState,
                charts = localCharts,
                onNavigateToDetails = onNavigateToDetails,
            )
        }
    } else {
        StatusMessageUI(
            title = stringResource(R.string.empty_downloads),
            message = stringResource(R.string.empty_downloads_description),
            icon = R.drawable.rounded_box_24,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 36.dp)
        )
    }
}