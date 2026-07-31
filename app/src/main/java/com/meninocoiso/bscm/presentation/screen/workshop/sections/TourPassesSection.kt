package com.meninocoiso.bscm.presentation.screen.workshop.sections

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.domain.result.ContentState
import com.meninocoiso.bscm.presentation.screen.details.OnNavigateToDetails
import com.meninocoiso.bscm.presentation.ui.components.StatusMessageUI
import com.meninocoiso.bscm.presentation.ui.components.layout.SectionWrapper
import com.meninocoiso.bscm.presentation.ui.components.preview.TourPassPreview
import com.meninocoiso.bscm.presentation.ui.modifiers.fabScrollObserver
import com.meninocoiso.bscm.presentation.viewmodel.WorkshopViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TourPassesSection(
    onFabStateChange: (Boolean) -> Unit,
    onNavigateToDetails: OnNavigateToDetails,
    viewModel: WorkshopViewModel,
) {
    val tourPasses by viewModel.feedTourPasses.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchTourPasses.collectAsStateWithLifecycle()
    val tourPassState by viewModel.tourPassState.collectAsStateWithLifecycle()

    var searchFieldState by remember { mutableStateOf(viewModel.searchFieldState) }
    val hasActiveQuery = searchFieldState.text.isNotEmpty()

    val items = if (hasActiveQuery) {
        searchResults ?: emptyList()
    } else {
        tourPasses
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        if (items.isEmpty() && tourPassState is ContentState.Loading) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator(Modifier.size(36.dp))
            }
        } else if (items.isEmpty() && tourPassState is ContentState.Error) {
            StatusMessageUI(
                modifier = Modifier.fillMaxSize(),
                title = stringResource(R.string.something_went_wrong),
                message = stringResource(R.string.check_connection),
                icon = R.drawable.rounded_emergency_home_24,
                onClick = { viewModel.fetchTourPasses() }
            )
        } else if (items.isEmpty() && hasActiveQuery) {
            StatusMessageUI(
                modifier = Modifier.fillMaxSize(),
                title = stringResource(R.string.no_tour_passes_found),
                message = stringResource(R.string.no_tour_passes_found_description),
                icon = R.drawable.outline_filter_alt_24,
                onClick = { viewModel.clearTourPassSearch() },
                buttonLabel = stringResource(R.string.clear_search)
            )
        } else {
            PullToRefreshBox(
                isRefreshing = tourPassState is ContentState.Loading,
                onRefresh = { viewModel.fetchTourPasses() }
            ) {
                SectionWrapper(
                    modifier = Modifier
                        .fillMaxSize()
                        .fabScrollObserver { shouldExtend ->
                            onFabStateChange(shouldExtend)
                        },
                    listState = viewModel.tourPassListState,
                ) {
                    itemsIndexed(items) { _, tourPass ->
                        TourPassPreview(
                            tourPass = tourPass,
                            onPress = {
                                onNavigateToDetails(tourPass)
                            },
                        )
                    }

                    if (viewModel.isLoadingMoreTourPasses) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 36.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        }
                    } else {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.tour_passes_feed_end),
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
