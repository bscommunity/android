package com.meninocoiso.bscm.presentation.ui.components.profile

import androidx.compose.runtime.Composable
import com.meninocoiso.bscm.domain.model.CatalogItem
import com.meninocoiso.bscm.domain.model.Chart
import com.meninocoiso.bscm.domain.model.Theme
import com.meninocoiso.bscm.domain.model.TourPass
import com.meninocoiso.bscm.presentation.ui.components.ContentFilterOption
import com.meninocoiso.bscm.presentation.ui.components.ContentFilterUI

@Composable
fun CatalogFilters(items: List<CatalogItem>, collectionsAmount: Int? = null, onFilterSelected: (Int) -> Unit) {
    val chartsCount = items.filter { it is Chart }.size
    val tourPassesCount = items.filter { it is TourPass }.size
    val themesCount = items.filter { it is Theme }.size

    /*
    * ContentFilterOption(
                id = 1,
                title = "Collections",
                count = collectionsAmount
            ),*/

    val options = mutableListOf(
        ContentFilterOption(
            id = 0,
            title = "All",
            count = items.size
        ),
        ContentFilterOption(
            id = 2,
            title = "Charts",
            count = chartsCount
        ),
        ContentFilterOption(
            id = 3,
            title = "Tour Passes",
            count = tourPassesCount
        ),
        ContentFilterOption(
            id = 4,
            title = "Themes",
            count = themesCount
        )
    )

    if (collectionsAmount != null) {
        options.add(1, ContentFilterOption(
            id = 1,
            title = "Collections",
            count = collectionsAmount
        ))
    }

    ContentFilterUI(
        options = options,
        onClick = { onFilterSelected(it) }
    )
}