package com.meninocoiso.beatstarcommunity.presentation.screens.workshop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.meninocoiso.beatstarcommunity.presentation.screens.details.OnNavigateToDetails
import com.meninocoiso.beatstarcommunity.presentation.screens.workshop.sections.ChartsSection
import com.meninocoiso.beatstarcommunity.presentation.screens.workshop.sections.ThemesSection
import com.meninocoiso.beatstarcommunity.presentation.screens.workshop.sections.TourPassesSection
import com.meninocoiso.beatstarcommunity.presentation.ui.components.Workshop.WorkshopChips
import com.meninocoiso.beatstarcommunity.presentation.ui.components.Workshop.WorkshopTabsItems
import com.meninocoiso.beatstarcommunity.presentation.ui.components.Workshop.WorkshopTopBar
import com.meninocoiso.beatstarcommunity.util.AppBarUtils

private val SearchBarHeight = 80.dp
private val TabsHeight = 48.dp
private val WorkshopChipsHeight = 56.dp

@Composable
fun WorkshopScreen(
    onNavigateToDetails: OnNavigateToDetails,
    onFabStateChange: (Boolean) -> Unit,
    onSnackbar: (String) -> Unit
) {
    val horizontalPagerState = rememberPagerState {
        WorkshopTabsItems.size
    }

    val (connection, spaceHeight, statusBarHeight) = AppBarUtils.getConnection(
        collapsableHeight = SearchBarHeight,
        fixedHeight = TabsHeight,
        bottomCollapsableHeight = WorkshopChipsHeight,
    )

    Column {
        Column(
            Modifier
                .height(spaceHeight),
            verticalArrangement = Arrangement.Bottom,
        ) {
            WorkshopChips(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(5f)
            )
        }

        HorizontalPager(
            state = horizontalPagerState,
            key = { it }, // Recompose the pager when the page changes
            beyondViewportPageCount = 1 // Keep the next page in memory
        ) { index ->
            when (
                index
            ) {
                0 -> ChartsSection(
                    connection,
                    onNavigateToDetails,
                    onFabStateChange,
                    onSnackbar
                )
                1 -> TourPassesSection(connection)
                2 -> ThemesSection(connection)
            }
        }
    }

    WorkshopTopBar(
        connection = connection,
        appBarHeights = Triple(SearchBarHeight, TabsHeight, statusBarHeight),
        pagerState = horizontalPagerState,
    )
}