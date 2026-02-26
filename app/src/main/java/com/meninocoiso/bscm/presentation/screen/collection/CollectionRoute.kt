package com.meninocoiso.bscm.presentation.screen.collection

import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.domain.model.SimplifiedCollection
import com.meninocoiso.bscm.domain.result.ContentResult
import com.meninocoiso.bscm.presentation.screen.details.OnNavigateToDetails
import com.meninocoiso.bscm.presentation.ui.components.RouteUI
import com.meninocoiso.bscm.presentation.ui.components.StatusMessageUI
import com.meninocoiso.bscm.presentation.viewmodel.CollectionViewModel

@Composable
fun CollectionRoute(
    loggedUserId: String?,
    username: String,
    slug: String,
    onReturn: () -> Unit,
    onNavigateToDetails: OnNavigateToDetails,
    viewModel: CollectionViewModel = hiltViewModel()
) {
    val state by viewModel.collectionResult.collectAsStateWithLifecycle()

    // Fetch the collection via slug when the route is first opened
    LaunchedEffect(slug) {
        viewModel.fetchCollectionBySlug(username, slug)
    }

    RouteUI {
        when (state) {
            is ContentResult.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            is ContentResult.Success<SimplifiedCollection> -> {
                val collection = (state as ContentResult.Success<SimplifiedCollection>).data
                CollectionScreen(
                    collection = collection,
                    isOwner = loggedUserId == collection.owner.id,
                    onReturn = onReturn,
                    onNavigateToDetails = onNavigateToDetails,
                    viewModel = viewModel
                )
            }

            is ContentResult.Error -> {
                StatusMessageUI(
                    title = stringResource(R.string.failed_to_load_collection),
                    message = (state as ContentResult.Error).message
                        ?: stringResource(R.string.failed_to_load_collection_description),
                    icon = R.drawable.rounded_error_24,
                    onClick = { viewModel.fetchCollectionBySlug(username, slug) },
                    buttonLabel = stringResource(R.string.retry),
                )

            }
        }
    }
}
