package com.meninocoiso.beatstarcommunity.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.hilt.navigation.compose.hiltViewModel
import com.meninocoiso.beatstarcommunity.R
import com.meninocoiso.beatstarcommunity.domain.enums.DifficultyEnum
import com.meninocoiso.beatstarcommunity.domain.model.Chart
import com.meninocoiso.beatstarcommunity.presentation.navigation.UpdatesSection
import com.meninocoiso.beatstarcommunity.presentation.ui.components.StatusMessageUI
import com.meninocoiso.beatstarcommunity.presentation.ui.components.TabItem
import com.meninocoiso.beatstarcommunity.presentation.ui.components.TabsUI
import com.meninocoiso.beatstarcommunity.presentation.ui.components.chart.LocalChartPreview
import com.meninocoiso.beatstarcommunity.presentation.ui.components.layout.Section
import com.meninocoiso.beatstarcommunity.presentation.viewmodel.UpdatesState
import com.meninocoiso.beatstarcommunity.presentation.viewmodel.UpdatesViewModel
import com.meninocoiso.beatstarcommunity.util.AppBarUtils

val updatesTabsItems = listOf(
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
	viewModel: UpdatesViewModel = hiltViewModel(),
) {
	val downloadsState by viewModel.downloadsState.collectAsState()

	val chartsToUpdate = (0..3).map {
		Chart(
			isDeluxe = false,
			versions = listOf(),
			difficulty = DifficultyEnum.Extreme,
			coverUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music211/v4/92/9f/69/929f69f1-9977-3a44-d674-11f70c852d1b/24UMGIM36186.rgb.jpg/592x592bb.webp",
			track = "teste",
			id = "2342432",
			contributors = listOf(),
			album = "teste",
			artist = "teste",
			isExplicit = false,
			isFeatured = false,
			isInstalled = false
		)
	}

	val horizontalPagerState = rememberPagerState {
		updatesTabsItems.size
	}

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
				0 -> WorkspaceSection(
					chartsToUpdate = chartsToUpdate,
					downloadsState = downloadsState,  // Pass the sealed state here
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

@Composable
private fun SectionWrapper(
	nestedScrollConnection: NestedScrollConnection,
	content: @Composable () -> Unit
) {
	Box(
		modifier = Modifier
			.fillMaxSize()
			.nestedScroll(nestedScrollConnection),
	) {
		Column(
			modifier = Modifier
				.verticalScroll(rememberScrollState()),
		) {
			Spacer(modifier = Modifier.height(16.dp))
			content()
			Spacer(modifier = Modifier.height(16.dp))
		}
	}
}

// Currently using fixed heights because of Compose lack of support for nested scrollable columns.
// This is hard to set but works well for now.
// Unfortunately, this approach raises some accessibility issues that need to be addressed in the future.
// For now, some effort was put into making the texts visible in phones with larger scale settings.
private val UpdatableChartPreviewHeight = 70.dp
private val LocalChartPreviewHeight = 108.dp
private val DownloadSectionTitleHeight = 24.dp

val TextUnit.nonScaledEm
	@Composable
	get() = (this.value / LocalDensity.current.fontScale).em

@Composable
private fun DownloadsSectionsTitle(
	title: String
) {
	Box(
		modifier = Modifier
			.fillMaxWidth()
			.height(DownloadSectionTitleHeight)
			.padding(16.dp, 0.dp, 0.dp, 0.dp)
	) {
		Text(text = title, style = MaterialTheme.typography.labelLarge)
	}
}

@Composable
fun WorkspaceSection(
	chartsToUpdate: List<Chart>,
	downloadsState: UpdatesState,
	nestedScrollConnection: NestedScrollConnection,
) {
	val verticalGap = 8.dp
	var selectedIndex by remember { mutableIntStateOf(-1) }
	val options = listOf("Charts", "Tour Passes", "Themes")

	SectionWrapper(nestedScrollConnection) {
		// Updates section remains unchanged
		Section(
			title = "Updates available (${chartsToUpdate.size})",
			thickness = 0.dp,
			titleModifier = Modifier.padding(top = 8.dp),
		) {
			// ... Your LazyColumn for chartsToUpdate ...
		}

		// Downloaded section
		Section(
			title = when (downloadsState) {
				is UpdatesState.Success -> "Downloaded (${downloadsState.charts.size})"
				else -> "Downloaded"
			},
			modifier = Modifier.padding(top = 16.dp),
		) {
			SingleChoiceSegmentedButtonRow(
				modifier = Modifier
					.fillMaxWidth()
					.padding(16.dp, 0.dp, 16.dp, 16.dp)
			) {
				options.forEachIndexed { index, label ->
					SegmentedButton(
						shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
						onClick = {
							selectedIndex = if (selectedIndex != index) index else -1
						},
						enabled = false,
						selected = index == selectedIndex
					) {
						Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
					}
				}
			}

			when (downloadsState) {
				is UpdatesState.Loading -> {
					Box(
						modifier = Modifier.fillMaxSize(),
						contentAlignment = Alignment.Center
					) {
						CircularProgressIndicator(modifier = Modifier.padding(16.dp))
					}
				}
				is UpdatesState.Error -> {
					StatusMessageUI(
						title = "Looks like something went wrong...",
						message = "\"${downloadsState.message}\"\nPlease try again or check Discord with error above to see if it’s already a known issue",
						icon = R.drawable.rounded_hourglass_disabled_24
					)
				}
				is UpdatesState.Success -> {
					val chartsList = downloadsState.charts
					if (chartsList.isEmpty()) {
						Text(text = "No downloaded charts available", modifier = Modifier.padding(16.dp))
					} else {
						LazyColumn(
							modifier = Modifier.height(
								(LocalChartPreviewHeight * chartsList.size) +
										(verticalGap * (chartsList.size - 1))
							),
							userScrollEnabled = false,
							contentPadding = PaddingValues(horizontal = 16.dp),
							verticalArrangement = Arrangement.spacedBy(verticalGap),
							horizontalAlignment = Alignment.CenterHorizontally
						) {
							item {
								DownloadsSectionsTitle("Charts")
							}
							items(chartsList) { chart ->
								LocalChartPreview(
									chart = chart,
									version = 1,
									modifier = Modifier.height(LocalChartPreviewHeight),
									onNavigateToDetails = { /*TODO*/ }
								)
							}
						}
					}
				}
			}
		}
	}
}


@Composable
private fun InstallationsSection(
	nestedScrollConnection: NestedScrollConnection,
) {
	SectionWrapper(nestedScrollConnection = nestedScrollConnection) {
		val list = (0..100).map { it.toString() }
		LazyColumn(
			modifier = Modifier.height(500.dp)
		) {
			items(count = list.size) {
				Text(text = "Parte 2 - $it")
			}
		}
	}
}