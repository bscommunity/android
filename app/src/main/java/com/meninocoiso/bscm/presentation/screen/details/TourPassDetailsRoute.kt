package com.meninocoiso.bscm.presentation.screen.details

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.domain.model.CatalogItem
import com.meninocoiso.bscm.domain.model.TourPass
import com.meninocoiso.bscm.domain.result.ContentResult
import com.meninocoiso.bscm.presentation.ui.components.Loading
import com.meninocoiso.bscm.presentation.ui.components.RouteUI
import com.meninocoiso.bscm.presentation.ui.components.StatusMessageUI
import com.meninocoiso.bscm.presentation.ui.utils.asString
import com.meninocoiso.bscm.presentation.viewmodel.TourPassDetailsViewModel

@Composable
fun TourPassDetailsRoute(
    id: String?,
    onReturn: () -> Unit,
    onNavigateToDetails: (CatalogItem) -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: TourPassDetailsViewModel = hiltViewModel()
) {
    val state by viewModel.tourPass.collectAsStateWithLifecycle()

    // If we don't have a tour pass from typed navigation, fetch it using id
    LaunchedEffect(id) {
        viewModel.fetchTourPassById(id)
    }

    RouteUI {
        when (state) {
            is ContentResult.Loading -> { Loading() }

            is ContentResult.Success -> {
                TourPassDetailsScreen(
                    tourPass = (state as ContentResult.Success<TourPass>).data,
                    onReturn = onReturn,
                    onNavigateToChart = { chart ->
                        onNavigateToDetails(chart)
                    },
                    onNavigateToSettings = onNavigateToSettings,
                )
            }

            is ContentResult.Error -> {
                StatusMessageUI(
                    title = stringResource(R.string.failed_to_load_tour_pass_details),
                    message = (state as ContentResult.Error).message.asString(),
                    icon = R.drawable.rounded_error_24,
                    onClick = {
                        viewModel.fetchTourPassById(id)
                    },
                    buttonLabel = stringResource(R.string.retry),
                )
            }
        }
    }
}
