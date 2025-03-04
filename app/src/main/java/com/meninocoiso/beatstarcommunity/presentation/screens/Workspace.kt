package com.meninocoiso.beatstarcommunity.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.meninocoiso.beatstarcommunity.R
import com.meninocoiso.beatstarcommunity.domain.model.Chart
import com.meninocoiso.beatstarcommunity.presentation.ui.components.StatusMessageUI
import com.meninocoiso.beatstarcommunity.presentation.ui.components.chart.ChartPreview
import com.meninocoiso.beatstarcommunity.presentation.ui.components.workspace.WorkspaceChips
import com.meninocoiso.beatstarcommunity.presentation.ui.components.workspace.WorkspaceTopBar
import com.meninocoiso.beatstarcommunity.presentation.ui.components.workspace.workspaceTabsItems
import com.meninocoiso.beatstarcommunity.presentation.viewmodel.ChartViewModel
import com.meninocoiso.beatstarcommunity.presentation.viewmodel.ChartsState
import com.meninocoiso.beatstarcommunity.util.AppBarUtils

private val SearchBarHeight = 80.dp
private val TabsHeight = 48.dp
private val WorkspaceChipsHeight = 56.dp

@Composable
fun WorkspaceScreen(onNavigateToDetails: OnNavigateToDetails) {
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
			state = horizontalPagerState,
			key = { it }, // Recompose the pager when the page changes
			beyondViewportPageCount = 1 // Keep the next page in memory
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartsSection(
	nestedScrollConnection: NestedScrollConnection,
	onNavigateToDetails: OnNavigateToDetails,
	viewModel: ChartViewModel = hiltViewModel()
) {
	val chartsState by viewModel.charts.collectAsState()

	when (chartsState) {
		is ChartsState.Loading -> {
			Box(
				modifier = Modifier.fillMaxSize(),
				contentAlignment = Alignment.Center,
			) {
				CircularProgressIndicator(
					modifier = Modifier.width(36.dp),
					color = MaterialTheme.colorScheme.secondary,
					trackColor = MaterialTheme.colorScheme.surfaceVariant,
				)
			}
		}
		is ChartsState.Success -> {
			val data = (chartsState as ChartsState.Success).charts
			if (data.isEmpty()) {
				StatusMessageUI(
					title = "We couldn't find anything based on your search",
					message = "Try removing some filters or searching for something else",
					icon = R.drawable.rounded_sentiment_dissatisfied_24,
					onClick = { /* Optionally trigger a refresh or clear filters */ },
					buttonLabel = "Clear filters"
				)
			} else {
				PullToRefreshBox(
					isRefreshing = false,
					onRefresh = { viewModel.refresh() }
				) {
					SectionWrapper(nestedScrollConnection = nestedScrollConnection) {
						items(data) { chart ->
							ChartPreview(
								onNavigateToDetails = { onNavigateToDetails(chart) },
								chart = chart
							)
						}
					}
				}
			}
		}
		is ChartsState.Error -> {
			val errorMessage = (chartsState as ChartsState.Error).message ?: "Unknown error"

			// We handle no internet connection separately
			val (title, message, icon) = if (errorMessage == "No internet connection") {
				Triple(
					"No Internet Connection",
					"Please check your connection and try again.",
					R.drawable.rounded_wifi_off_24
				)
			} else {
				Triple(
					"Looks like something went wrong...",
					"\"$errorMessage\"\nPlease try again or check Discord to see if it’s a known issue",
					R.drawable.rounded_hourglass_disabled_24
				)
			}

			StatusMessageUI(
				title = title,
				message = message,
				icon = icon,
				onClick = { viewModel.refresh() }
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