package com.meninocoiso.bscm.presentation.screen.workshop

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.meninocoiso.bscm.presentation.screen.details.OnNavigateToDetails
import com.meninocoiso.bscm.presentation.screen.workshop.sections.ChartsSection
import com.meninocoiso.bscm.presentation.screen.workshop.sections.ThemesSection
import com.meninocoiso.bscm.presentation.screen.workshop.sections.TourPassesSection
import com.meninocoiso.bscm.presentation.ui.components.workshop.WorkshopSearchBar
import com.meninocoiso.bscm.presentation.ui.components.workshop.WorkshopTopBar
import com.meninocoiso.bscm.presentation.ui.components.workshop.getWorkshopTabsItems
import com.meninocoiso.bscm.presentation.viewmodel.WorkshopViewModel
import com.meninocoiso.bscm.util.AppBarUtils

private val SearchBarHeight = 80.dp
private val TabsHeight = 48.dp

@Composable
fun WorkshopScreen(
    onNavigateToDetails: OnNavigateToDetails,
    onFabStateChange: (Boolean) -> Unit,
    onSnackbar: (String) -> Unit,
    viewModel: WorkshopViewModel = hiltViewModel()
) {
    val workshopTabsItems = getWorkshopTabsItems()

    val horizontalPagerState = rememberPagerState {
        workshopTabsItems.size
    }

    // Remove bottomCollapsableHeight since WorkshopChips are now part of the scrollable content
    val (connection, spaceHeight, statusBarHeight) = AppBarUtils.getConnection(
        collapsableHeight = SearchBarHeight,
        fixedHeight = TabsHeight,
    )

    Column {
        Spacer(modifier = Modifier.height(spaceHeight))

        HorizontalPager(
            state = horizontalPagerState,
            key = { it }, // Recompose the pager when the page changes
            beyondViewportPageCount = 1 // Keep the next page in memory
        ) { index ->
            when (index) {
                0 -> ChartsSection(
                    connection,
                    viewModel.listState,
                    onNavigateToDetails,
                    onFabStateChange,
                    onSnackbar,
                    viewModel
                )

                1 -> TourPassesSection(
                    connection, 
                    viewModel.listState,
                    onFabStateChange
                )

                2 -> ThemesSection(connection)
            }
        }
    }

    WorkshopTopBar(
        connection = connection,
        appBarHeights = Triple(SearchBarHeight, TabsHeight, statusBarHeight),
        pagerState = horizontalPagerState,
    ) {
        WorkshopSearchBar(
            textFieldState = viewModel.searchFieldState,
            modifier = Modifier.alpha(connection.appBarOpacity),
            historyItems = viewModel.searchHistory,
            onHistoryItemDelete = {
                viewModel.removeSearchHistory(it)
            },
            suggestions = viewModel.suggestions,
            onSearch = { query ->
                viewModel.searchCharts(query)
            }
        )
    }
}