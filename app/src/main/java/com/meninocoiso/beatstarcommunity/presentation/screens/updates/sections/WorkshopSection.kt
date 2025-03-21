package com.meninocoiso.beatstarcommunity.presentation.screens.updates.sections

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.unit.dp
import com.meninocoiso.beatstarcommunity.presentation.screens.details.OnNavigateToDetails
import com.meninocoiso.beatstarcommunity.presentation.ui.components.local.LocalContentSection
import com.meninocoiso.beatstarcommunity.presentation.ui.components.updates.UpdatesSection
import com.meninocoiso.beatstarcommunity.presentation.viewmodel.LocalChartsState
import com.meninocoiso.beatstarcommunity.presentation.viewmodel.UpdatesState

@Composable
internal fun WorkshopSection(
    localChartsState: LocalChartsState,
    updatesState: UpdatesState,
    onFetchUpdates: () -> Unit,
    onNavigateToDetails: OnNavigateToDetails,
    onFabStateChange: (Boolean) -> Unit,
    nestedScrollConnection: NestedScrollConnection,
) {
    Column(
        modifier = Modifier.padding(top = 24.dp).fillMaxSize()
    ) {
        UpdatesSection(
            updatesState = updatesState,
            onFetchUpdates = onFetchUpdates,
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