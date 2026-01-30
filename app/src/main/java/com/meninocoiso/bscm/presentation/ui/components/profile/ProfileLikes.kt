package com.meninocoiso.bscm.presentation.ui.components.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.domain.model.CatalogItem
import com.meninocoiso.bscm.domain.result.ContentState
import com.meninocoiso.bscm.presentation.ui.components.SegmentedButtonUI
import com.meninocoiso.bscm.presentation.ui.components.StatusMessageUI

@Composable
fun ProfileLikes(
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
                message = "No liked content",
                icon = R.drawable.rounded_favorite_24
            )
        }
    ) {
        LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SegmentedButtonUI(
                    options = listOf("Charts", "Tour Passes", "Themes"),
                    onSelected = {})
            }
            contentList(items)
        }
    }
}