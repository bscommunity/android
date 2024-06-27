package com.meninocoiso.beatstarcommunity.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.meninocoiso.beatstarcommunity.components.ChartPreview
import com.meninocoiso.beatstarcommunity.components.workspace.WorkspaceChips
import com.meninocoiso.beatstarcommunity.components.workspace.WorkspaceTopBar
import com.meninocoiso.beatstarcommunity.components.workspace.workspaceTabsItems
import com.meninocoiso.beatstarcommunity.data.placeholderChart
import com.meninocoiso.beatstarcommunity.utils.CollapsingAppBarNestedScrollConnection

val SearchBarHeight = 72.dp
val WorkspaceTabsHeight = 48.dp

@Composable
fun Workspace(onNavigateToDetails: () -> Unit) {
	val pagerState = rememberPagerState {
		workspaceTabsItems.size
	}

	val statusBarHeight = WindowInsets.systemBars.asPaddingValues().calculateTopPadding()
	val appBarMaxHeightPx =
		with(LocalDensity.current) { (SearchBarHeight + WorkspaceTabsHeight + statusBarHeight).roundToPx() }
	val searchBarMaxHeightPx =
		with(LocalDensity.current) { SearchBarHeight.roundToPx() }
	val connection = remember(appBarMaxHeightPx) {
		CollapsingAppBarNestedScrollConnection(searchBarMaxHeightPx)
	}
	val density = LocalDensity.current
	val spaceHeight by remember(density) {
		derivedStateOf {
			with(density) {
				(appBarMaxHeightPx + connection.appBarOffset).toDp()
			}
		}
	}

	Column {
		Spacer(
			Modifier
				.height(spaceHeight)
		)

		HorizontalPager(
			state = pagerState
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
		appBarOffset = connection.appBarOffset,
		appBarOpacity = connection.appBarOpacity,
		pagerState
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
		item {
			WorkspaceChips()
		}

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