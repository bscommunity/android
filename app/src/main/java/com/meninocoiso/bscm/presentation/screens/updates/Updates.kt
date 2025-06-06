package com.meninocoiso.bscm.presentation.screens.updates

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.meninocoiso.bscm.domain.enums.UpdatesSection
import com.meninocoiso.bscm.presentation.screens.details.OnNavigateToDetails
import com.meninocoiso.bscm.presentation.screens.updates.sections.InstallationsSection
import com.meninocoiso.bscm.presentation.screens.updates.sections.WorkshopSection
import com.meninocoiso.bscm.presentation.ui.components.TabItem
import com.meninocoiso.bscm.presentation.ui.components.TabsUI
import com.meninocoiso.bscm.presentation.viewmodel.UpdatesViewModel
import com.meninocoiso.bscm.util.AppBarUtils

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
	section: UpdatesSection,
	onNavigateToDetails: OnNavigateToDetails,
	onSnackbar: (String) -> Unit,
	onFabStateChange: (Boolean) -> Unit,
	viewModel: UpdatesViewModel = hiltViewModel(),
) {

	val horizontalPagerState = rememberPagerState {
		updatesTabsItems.size
	}

	// Scroll (horizontally) to the correct section
	LaunchedEffect(section) {
		println("UpdatesScreen LaunchedEffect: section = $section")
		val pageIndex = when (section) {
			UpdatesSection.Workshop -> 0
			UpdatesSection.Installations -> 1
		}
		horizontalPagerState.requestScrollToPage(pageIndex)
	}
	
	val (connection, spaceHeight, statusBarHeight) = AppBarUtils.getConnection(
		collapsableHeight = TabsHeight,
	)

	Column {
		Spacer(
			Modifier
				.height(spaceHeight)
		)

		HorizontalPager(state = horizontalPagerState) { index ->
			when (index) {
				0 -> WorkshopSection(
					viewModel = viewModel,
					onNavigateToDetails = onNavigateToDetails,
					onSnackbar = onSnackbar,
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

