package com.meninocoiso.bscm.presentation.ui.components.profile

import androidx.compose.runtime.Composable
import com.meninocoiso.bscm.domain.model.CatalogItem
import com.meninocoiso.bscm.domain.model.Chart
import com.meninocoiso.bscm.domain.model.Theme
import com.meninocoiso.bscm.domain.model.TourPass
import com.meninocoiso.bscm.presentation.ui.components.ContentFilterOption
import com.meninocoiso.bscm.presentation.ui.components.ContentFilterUI

@Composable
fun CatalogFilters(
    items: List<CatalogItem>,
    collectionsAmount: Int? = null,
    currentSelected: Int = 0,
    onFilterSelected: (Int) -> Unit
) {
    val chartsCount = items.filter { it is Chart }.size
    val tourPassesCount = items.filter { it is TourPass }.size
    val themesCount = items.filter { it is Theme }.size

    val options = mutableListOf(
        ContentFilterOption(
            id = 0,
            title = "Charts",
            count = chartsCount,
            disabled = chartsCount == 0
        ),
        ContentFilterOption(
            id = 1,
            title = "Tour Passes",
            count = tourPassesCount,
            disabled = tourPassesCount == 0
        ),
        ContentFilterOption(
            id = 2,
            title = "Themes",
            count = themesCount,
            disabled = themesCount == 0
        )
    )

    if (collectionsAmount != null) {
        options.add(
            options.size,
            ContentFilterOption(
                id = 3,
                title = "Collections",
                count = collectionsAmount,
                disabled = collectionsAmount == 0
            )
        )
    }

    ContentFilterUI(
        currentSelected = currentSelected,
        options = options,
        onClick = { onFilterSelected(it) }
    )
}