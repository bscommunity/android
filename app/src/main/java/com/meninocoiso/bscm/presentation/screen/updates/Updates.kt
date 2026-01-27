package com.meninocoiso.bscm.presentation.screen.updates

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.domain.enums.UpdatesSection
import com.meninocoiso.bscm.presentation.navigation.OnSnackbar
import com.meninocoiso.bscm.presentation.screen.details.OnNavigateToDetails
import com.meninocoiso.bscm.presentation.screen.updates.sections.ContentSection
import com.meninocoiso.bscm.presentation.screen.updates.sections.ModificationsSection
import com.meninocoiso.bscm.presentation.ui.components.TabItem
import com.meninocoiso.bscm.presentation.ui.components.TabsUI
import com.meninocoiso.bscm.presentation.viewmodel.UpdatesViewModel
import com.meninocoiso.bscm.util.AppBarUtils
import kotlinx.coroutines.launch

@Composable
private fun getUpdatesTabsItems(): List<TabItem> {
	return listOf(
		TabItem(
			title = stringResource(R.string.workshop),
			hasNews = false
		),
		TabItem(
			title = stringResource(R.string.installations),
			hasNews = false
		)
	)
}

private val TabsHeight = 55.dp

@Composable
fun UpdatesScreen(
	section: UpdatesSection,
	onNavigateToDetails: OnNavigateToDetails,
	onSnackbar: OnSnackbar,
	onFabStateChange: (Boolean) -> Unit,
	viewModel: UpdatesViewModel = hiltViewModel(),
) {
	val coroutineScope = rememberCoroutineScope()
	
	val updatesTabsItems = getUpdatesTabsItems()

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
		coroutineScope.launch {
			horizontalPagerState.scrollToPage(pageIndex)
		}
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
				0 -> ContentSection(
					viewModel = viewModel,
					onNavigateToDetails = onNavigateToDetails,
					onSnackbar = onSnackbar,
					onFabStateChange = onFabStateChange,
					nestedScrollConnection = connection
				)
				1 -> ModificationsSection(connection)
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

