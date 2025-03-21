package com.meninocoiso.beatstarcommunity.presentation.screens.updates

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meninocoiso.beatstarcommunity.presentation.navigation.UpdatesSection
import com.meninocoiso.beatstarcommunity.presentation.screens.details.OnNavigateToDetails
import com.meninocoiso.beatstarcommunity.presentation.screens.updates.sections.InstallationsSection
import com.meninocoiso.beatstarcommunity.presentation.screens.updates.sections.WorkshopSection
import com.meninocoiso.beatstarcommunity.presentation.ui.components.TabItem
import com.meninocoiso.beatstarcommunity.presentation.ui.components.TabsUI
import com.meninocoiso.beatstarcommunity.presentation.viewmodel.LocalChartsState
import com.meninocoiso.beatstarcommunity.presentation.viewmodel.UpdatesViewModel
import com.meninocoiso.beatstarcommunity.util.AppBarUtils

private val updatesTabsItems = listOf(
	TabItem(
		title = "Workshop",
		hasNews = false
	),
	TabItem(
		title = "Installations",
		hasNews = false
	)
)

private val TabsHeight = 55.dp

@Composable
fun UpdatesScreen(
	section: UpdatesSection? = UpdatesSection.Workshop,
	onNavigateToDetails: OnNavigateToDetails,
	onFabStateChange: (Boolean) -> Unit,
	viewModel: UpdatesViewModel = hiltViewModel(),
) {
	val updatesState by viewModel.updatesState.collectAsState()
	val localChartsState by viewModel.localChartsState.collectAsStateWithLifecycle()

	val horizontalPagerState = rememberPagerState {
		updatesTabsItems.size
	}

	LaunchedEffect(Unit) {
		// Update local charts if needed
		if (localChartsState is LocalChartsState.Success) {
			viewModel.loadLocalCharts()
		}
	}

	// Scroll (horizontally) to the correct section
	LaunchedEffect(section) {
		val pageIndex = when (section) {
			UpdatesSection.Workshop -> 0
			UpdatesSection.Installations -> 1
			null -> UpdatesSection.Workshop.ordinal
		}
		horizontalPagerState.requestScrollToPage(pageIndex)
	}

	val (connection, spaceHeight, statusBarHeight) = AppBarUtils.getConnection(collapsableHeight = TabsHeight)

	Column {
		Spacer(
			Modifier
				.height(spaceHeight)
		)

		HorizontalPager(state = horizontalPagerState) { index ->
			when (index) {
				0 -> WorkshopSection(
					updatesState = updatesState,
					localChartsState = localChartsState,
					onFetchUpdates = {
						viewModel.fetchUpdates((localChartsState as LocalChartsState.Success).charts)
				 	},
					onNavigateToDetails = onNavigateToDetails,
					onFabStateChange = onFabStateChange,
					nestedScrollConnection = connection
				)
				1 -> InstallationsSection(connection)
			}
		}
	}

	Box(
		modifier = Modifier
			.offset { IntOffset(0, connection.appBarOffset) }
			.height(TabsHeight + statusBarHeight),
		contentAlignment = Alignment.BottomCenter
	) {
		TabsUI(
			tabs = updatesTabsItems,
			pagerState = horizontalPagerState,
		)
	}
}

