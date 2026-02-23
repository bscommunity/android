package com.meninocoiso.bscm.presentation.ui.components.profile

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.domain.model.CatalogItem
import com.meninocoiso.bscm.domain.model.Chart
import com.meninocoiso.bscm.domain.model.Theme
import com.meninocoiso.bscm.domain.model.TourPass
import com.meninocoiso.bscm.domain.result.ContentState
import com.meninocoiso.bscm.presentation.screen.details.OnNavigateToDetails
import com.meninocoiso.bscm.presentation.ui.components.StatusMessageSize
import com.meninocoiso.bscm.presentation.ui.components.StatusMessageUI

@Composable
fun ProfileLibrary(
    items: List<CatalogItem>,
    state: ContentState,
    onFetch: (reset: Boolean) -> Unit,
    onNavigateToDetails: OnNavigateToDetails,
    listState: LazyListState,
    isLoadingMore: Boolean,
    hasMore: Boolean,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    OnScrollLoadMore(
        listState = listState,
        hasMore = hasMore,
        isLoadingMore = isLoadingMore,
        onLoadMore = onLoadMore
    )

    BaseContainer(
        isEmpty = items.isEmpty(),
        state = state,
        onRetry = onFetch,
        empty = {
            StatusMessageUI(
                modifier = Modifier.fillMaxSize(),
                size = StatusMessageSize.Medium,
                message = "No content in library",
                icon = R.drawable.outline_library_music_24
            )
        }
    ) {
        LazyColumn(modifier, state = listState) {
            item {
                CatalogFilters(
                    // TODO: Get these counts from the API instead of estimating them here,
                    //  since we might not be fetching all items at once
                    itemsAmount = Triple(
                        items.count { it is Chart },
                        items.count { it is TourPass },
                        items.count { it is Theme }
                    ),
                    onFilterSelected = {}
                )
            }

            contentList(items, onNavigateToDetails)
            pagination(
                isLoadingMore = isLoadingMore,
                message = if (hasMore) "Carregando..." else "Fim da lista"
            )
        }
    }
}