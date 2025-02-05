package com.meninocoiso.beatstarcommunity.screens.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.meninocoiso.beatstarcommunity.components.ChartPreview
import com.meninocoiso.beatstarcommunity.components.workspace.WorkspaceChip
import com.meninocoiso.beatstarcommunity.components.workspace.WorkspaceChips
import com.meninocoiso.beatstarcommunity.components.workspace.WorkspaceTopBar
import com.meninocoiso.beatstarcommunity.components.workspace.workspaceTabsItems
import com.meninocoiso.beatstarcommunity.data.placeholderChart
import com.meninocoiso.beatstarcommunity.util.AppBarUtils

private val SearchBarHeight = 80.dp
private val TabsHeight = 48.dp
private val WorkspaceChipsHeight = 56.dp

@Composable
fun WorkspaceScreen(onNavigateToDetails: () -> Unit) {
	val horizontalPagerState = rememberPagerState {
		workspaceTabsItems.size
	}

	val (connection, spaceHeight, statusBarHeight) = AppBarUtils.getConnection(
		collapsableHeight = SearchBarHeight,
		fixedHeight = TabsHeight,
		bottomCollapsableHeight = WorkspaceChipsHeight,
	)

	Column {
		Column(
			Modifier
				.height(spaceHeight),
			verticalArrangement = Arrangement.Bottom,
		) {
			WorkspaceChips(
				modifier = Modifier
					.fillMaxWidth()
					.zIndex(5f)
			)
		}

		HorizontalPager(
			state = horizontalPagerState
		) { index ->
			when (
				index
			) {
				0 -> ChartsSection(connection, onNavigateToDetails)
				1 -> TourPassesSection(connection)
				2 -> ThemesSection(connection)
			}
		}
	}

	WorkspaceTopBar(
		connection = connection,
		appBarHeights = Triple(SearchBarHeight, TabsHeight, statusBarHeight),
		pagerState = horizontalPagerState,
	)
}

@Composable
private fun SectionWrapper(
	nestedScrollConnection: NestedScrollConnection,
	content: LazyListScope.() -> Unit
) {
	LazyColumn(
		modifier = Modifier
			.fillMaxSize()
			.nestedScroll(nestedScrollConnection),
	) {
		content()
	}
}

@Composable
private fun ChartsSection(
	nestedScrollConnection: NestedScrollConnection,
	onNavigateToDetails: () -> Unit
) {
	SectionWrapper(nestedScrollConnection = nestedScrollConnection) {
		val list = (0..10).map { it.toString() }
		items(count = list.size) {
			ChartPreview(
				onNavigateToDetails = onNavigateToDetails,
				chart = placeholderChart
			)
		}
	}
}

@Composable
private fun TourPassesSection(nestedScrollConnection: NestedScrollConnection) {
	SectionWrapper(nestedScrollConnection = nestedScrollConnection) {
		val list = (0..25).map { it.toString() }
		items(count = list.size) {
			Text(
				text = "${list[it]} - Outra tela",
				style = MaterialTheme.typography.bodyLarge,
				modifier = Modifier
					.fillMaxWidth()
					.padding(horizontal = 16.dp)
			)
		}
	}
}

@Composable
private fun ThemesSection(nestedScrollConnection: NestedScrollConnection) {
	SectionWrapper(nestedScrollConnection = nestedScrollConnection) {
		val list = (0..5).map { it.toString() }
		items(count = list.size) {
			Text(
				text = "Este é o item ${list[it]}",
				style = MaterialTheme.typography.bodyLarge,
				modifier = Modifier
					.fillMaxWidth()
					.padding(horizontal = 16.dp)
			)
		}
	}
}