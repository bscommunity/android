package com.meninocoiso.bscm.presentation.screen.workshop

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.meninocoiso.bscm.presentation.navigation.OnSnackbar
import com.meninocoiso.bscm.presentation.screen.details.OnNavigateToDetails
import com.meninocoiso.bscm.presentation.screen.workshop.sections.ChartsSection
import com.meninocoiso.bscm.presentation.screen.workshop.sections.ThemesSection
import com.meninocoiso.bscm.presentation.screen.workshop.sections.TourPassesSection
import com.meninocoiso.bscm.presentation.ui.components.workshop.WorkshopSearchBar
import com.meninocoiso.bscm.presentation.ui.components.workshop.WorkshopTopBar
import com.meninocoiso.bscm.presentation.ui.components.workshop.getWorkshopTabsItems
import com.meninocoiso.bscm.presentation.viewmodel.WorkshopViewModel
import com.meninocoiso.bscm.util.AppBarUtils
import kotlinx.coroutines.launch

private val SearchBarHeight = 80.dp
private val TabsHeight = 48.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkshopScreen(
    onSnackbar: OnSnackbar,
    onFabStateChange: (Boolean) -> Unit,
    onNavigateToDetails: OnNavigateToDetails,
    onNavigateToSettings: () -> Unit,
    onWorkshopReselected: (() -> Unit) -> Unit = {},
    viewModel: WorkshopViewModel = hiltViewModel()
) {
    val workshopTabsItems = getWorkshopTabsItems()

    val horizontalPagerState = rememberPagerState {
        workshopTabsItems.size
    }

    val searchBarState = rememberSearchBarState()
    val scope = rememberCoroutineScope()

    // Re-route the active query when switching tabs so each tab searches its own content type
    LaunchedEffect(horizontalPagerState.currentPage) {
        val query = viewModel.searchFieldState.text.toString()
        if (query.isNotEmpty()) {
            when (horizontalPagerState.currentPage) {
                1 -> viewModel.searchTourPasses(query)
                2 -> viewModel.searchThemes(query)
                else -> viewModel.searchCharts(query)
            }
        }
    }

    // Register a close-search action so the caller (BottomNav) can trigger it on tab reselection
    onWorkshopReselected {
        if (searchBarState.currentValue == SearchBarValue.Expanded || viewModel.searchFieldState.text.isNotEmpty()) {
            scope.launch { searchBarState.animateToCollapsed() }
            viewModel.searchFieldState.setTextAndPlaceCursorAtEnd("")
            when (horizontalPagerState.currentPage) {
                1 -> viewModel.searchTourPasses("")
                2 -> viewModel.searchThemes("")
                else -> viewModel.searchCharts("")
            }
        }
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
                    nestedScrollConnection = connection,
                    listState = viewModel.listState,
                    onFabStateChange = onFabStateChange,
                    onSnackbar = onSnackbar,
                    onNavigateToSettings = onNavigateToSettings,
                    onNavigateToDetails = onNavigateToDetails,
                    viewModel = viewModel
                )

                1 -> TourPassesSection(
                    onFabStateChange = onFabStateChange,
                    onNavigateToDetails = onNavigateToDetails,
                    viewModel = viewModel
                )
                2 -> ThemesSection(
                    onFabStateChange = onFabStateChange,
                    onNavigateToDetails = onNavigateToDetails,
                    viewModel = viewModel
                )
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
                when (horizontalPagerState.currentPage) {
                    1 -> viewModel.searchTourPasses(query)
                    2 -> viewModel.searchThemes(query)
                    else -> viewModel.searchCharts(query)
                }
            },
            searchBarState = searchBarState,
        )
    }
}