package com.meninocoiso.beatstarcommunity.presentation.screens.workshop

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.meninocoiso.beatstarcommunity.presentation.screens.details.OnNavigateToDetails
import com.meninocoiso.beatstarcommunity.presentation.screens.workshop.sections.ChartsSection
import com.meninocoiso.beatstarcommunity.presentation.screens.workshop.sections.ThemesSection
import com.meninocoiso.beatstarcommunity.presentation.screens.workshop.sections.TourPassesSection
import com.meninocoiso.beatstarcommunity.presentation.ui.components.workshop.WorkshopChips
import com.meninocoiso.beatstarcommunity.presentation.ui.components.workshop.WorkshopSearchBar
import com.meninocoiso.beatstarcommunity.presentation.ui.components.workshop.WorkshopTabsItems
import com.meninocoiso.beatstarcommunity.presentation.ui.components.workshop.WorkshopTopBar
import com.meninocoiso.beatstarcommunity.presentation.viewmodel.WorkshopViewModel
import com.meninocoiso.beatstarcommunity.util.AppBarUtils

private val SearchBarHeight = 80.dp
private val TabsHeight = 48.dp
private val WorkshopChipsHeight = 56.dp

@Composable
fun WorkshopScreen(
    onNavigateToDetails: OnNavigateToDetails,
    onFabStateChange: (Boolean) -> Unit,
    onSnackbar: (String) -> Unit,
    viewModel: WorkshopViewModel = hiltViewModel()
) {
    val horizontalPagerState = rememberPagerState {
        WorkshopTabsItems.size
    }

    val bottomCollapsableHeight = remember { mutableStateOf<Dp?>(WorkshopChipsHeight) }

    val (connection, spaceHeight, statusBarHeight) = AppBarUtils.getConnection(
        collapsableHeight = SearchBarHeight,
        fixedHeight = TabsHeight,
        bottomCollapsableHeight = bottomCollapsableHeight,
    )

    LaunchedEffect(Unit) { 
        viewModel.observeSuggestions()
    }

    Column {
        Column(
            modifier = Modifier.height(spaceHeight),
            verticalArrangement = Arrangement.Bottom,
        ) {
            Box(
                modifier = Modifier.height(WorkshopChipsHeight),
                contentAlignment = Alignment.Center
            ) {
                WorkshopChips(
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(5f)
                )
            }
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
                    listState = viewModel.listState,
                    onNavigateToDetails,
                    onFabStateChange,
                    onSnackbar,
                    viewModel
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
                Log.d("WorkshopScreen", "onSearch")
                
                if (query.isEmpty()) {
                    // Restore WorkspaceChips when search is empty
                    bottomCollapsableHeight.value = WorkshopChipsHeight
                } else {
                    // Remove WorkspaceChips while searching
                    bottomCollapsableHeight.value = null
                }

                // Perform search or cleanup
                viewModel.searchCharts(query)
            }
        )
    }
}