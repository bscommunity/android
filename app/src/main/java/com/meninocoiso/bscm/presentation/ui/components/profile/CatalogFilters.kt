package com.meninocoiso.bscm.presentation.ui.components.profile

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.presentation.ui.components.ContentFilterOption
import com.meninocoiso.bscm.presentation.ui.components.ContentFilterUI

@Composable
fun CatalogFilters(
    itemsAmount: Triple<Int, Int, Int>,
    collectionsAmount: Int? = null,
    showCollection: Boolean = false,
    currentSelected: Int = 0,
    showThemes: Boolean = true,
    onFilterSelected: (Int) -> Unit
) {
    val options = mutableListOf(
        ContentFilterOption(
            id = 0,
            title = stringResource(R.string.charts),
            count = itemsAmount.first,
        ),
        ContentFilterOption(
            id = 1,
            title = stringResource(R.string.tour_passes),
            count = itemsAmount.second,
        )
    )

    if (showThemes) {
        options.add(
            ContentFilterOption(
                id = 2,
                title = stringResource(R.string.themes),
                count = itemsAmount.third,
            )
        )
    }

    // TODO: There's probably a better solution than manually adding the
    //  collection filter at the end of the list, but for now it works
    if (showCollection) {
        options.add(
            options.size,
                ContentFilterOption(
                id = 3,
                title = stringResource(R.string.collections),
                count = collectionsAmount ?: 0,
            )
        )
    }

    ContentFilterUI(
        currentSelected = currentSelected,
        options = options,
        onClick = { onFilterSelected(it) }
    )
}