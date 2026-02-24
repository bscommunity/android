package com.meninocoiso.bscm.presentation.ui.components.profile

import androidx.compose.runtime.Composable
import com.meninocoiso.bscm.presentation.ui.components.ContentFilterOption
import com.meninocoiso.bscm.presentation.ui.components.ContentFilterUI

@Composable
fun CatalogFilters(
    itemsAmount: Triple<Int, Int, Int>,
    collectionsAmount: Int? = null,
    currentSelected: Int = 0,
    onFilterSelected: (Int) -> Unit
) {
    val options = mutableListOf(
        ContentFilterOption(
            id = 0,
            title = "Charts",
            count = itemsAmount.first,
        ),
        ContentFilterOption(
            id = 1,
            title = "Tour Passes",
            count = itemsAmount.second,
        ),
        ContentFilterOption(
            id = 2,
            title = "Themes",
            count = itemsAmount.third,
        )
    )

    options.add(
        options.size,
        ContentFilterOption(
            id = 3,
            title = "Collections",
            count = if (collectionsAmount != null && collectionsAmount > 0) collectionsAmount else null,
        )
    )

    ContentFilterUI(
        currentSelected = currentSelected,
        options = options,
        onClick = { onFilterSelected(it) }
    )
}