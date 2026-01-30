package com.meninocoiso.bscm.presentation.ui.components.profile

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.domain.model.CatalogItem
import com.meninocoiso.bscm.domain.result.ContentState
import com.meninocoiso.bscm.presentation.ui.components.StatusMessageUI

@Composable
fun ProfileLibrary(
    items: List<CatalogItem>,
    state: ContentState,
    onFetch: () -> Unit,
    modifier: Modifier = Modifier
) {
    BaseContainer(
        isEmpty = items.isEmpty(),
        state = state,
        onRetry = onFetch,
        empty = {
            StatusMessageUI(
                modifier = Modifier.fillMaxWidth(),
                message = "No content in library",
                icon = R.drawable.outline_library_music_24
            )
        }
    ) {
        LazyColumn(modifier) {
            item {
                CatalogFilters(items, onFilterSelected = {})
            }

            contentList(items)
        }
    }
}