package com.meninocoiso.beatstarcommunity.presentation.screens

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meninocoiso.beatstarcommunity.R
import com.meninocoiso.beatstarcommunity.presentation.navigation.UpdatesSection
import com.meninocoiso.beatstarcommunity.presentation.ui.components.Size
import com.meninocoiso.beatstarcommunity.presentation.ui.components.StatusMessageUI
import com.meninocoiso.beatstarcommunity.presentation.ui.components.TabItem
import com.meninocoiso.beatstarcommunity.presentation.ui.components.TabsUI
import com.meninocoiso.beatstarcommunity.presentation.ui.components.chart.LocalChartPreview
import com.meninocoiso.beatstarcommunity.presentation.ui.components.layout.Section
import com.meninocoiso.beatstarcommunity.presentation.ui.components.updates.UpdateListItem
import com.meninocoiso.beatstarcommunity.presentation.ui.modifiers.fabScrollObserver
import com.meninocoiso.beatstarcommunity.presentation.ui.modifiers.infiniteRotation
import com.meninocoiso.beatstarcommunity.presentation.ui.modifiers.shimmerLoading
import com.meninocoiso.beatstarcommunity.presentation.viewmodel.LocalChartsState
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
	onNavigateToDetails: OnNavigateToDetails,
	onFabStateChange: (Boolean) -> Unit,
	viewModel: UpdatesViewModel = hiltViewModel(),
) {
	val updatesState by viewModel.updatesState.collectAsState()
	val localChartsState by viewModel.localChartsState.collectAsStateWithLifecycle()

	val horizontalPagerState = rememberPagerState {
		updatesTabsItems.size
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
				0 -> WorkspaceSection(
					updatesState = updatesState,
					localChartsState = localChartsState,
					onNavigateToDetails = onNavigateToDetails,
					onFetchUpdates = {
						viewModel.fetchUpdates((localChartsState as LocalChartsState.Success).charts)
				 	},
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

@Composable
private fun SectionWrapper(
	nestedScrollConnection: NestedScrollConnection,
	modifier: Modifier = Modifier,
	onFabStateChange: ((Boolean) -> Unit)? = null,
	content: LazyListScope.() -> Unit
) {
	LazyColumn(
		modifier = modifier
			.nestedScroll(nestedScrollConnection)
			.run {
				if (onFabStateChange != null) {
					this.fabScrollObserver(onFabStateChange)
				} else {
					this
				}
			},
		contentPadding = PaddingValues(horizontal = 16.dp),
		verticalArrangement = Arrangement.spacedBy(8.dp),
		horizontalAlignment = Alignment.CenterHorizontally
	) {
		content()
	}
}

@Composable
fun WorkspaceSection(
	updatesState: UpdatesState,
	onFetchUpdates: () -> Unit,
	onNavigateToDetails: OnNavigateToDetails,
	localChartsState: LocalChartsState,
	onFabStateChange: (Boolean) -> Unit,
	nestedScrollConnection: NestedScrollConnection,
) {
	// var selectedIndex by remember { mutableIntStateOf(-1) }
	// val options = listOf("Charts", "Tour Passes", "Themes")

	Column(
		modifier = Modifier.padding(top = 24.dp).fillMaxSize()
	) {
		// Updates section remains unchanged
		Section(
			title = when (updatesState) {
				is UpdatesState.Success -> "Updates available (${updatesState.charts.size})"
				else -> null
			},
			titleModifier = Modifier.padding(start = 16.dp, bottom = 12.dp),
			thickness = 0.dp,
		) {
			when (updatesState) {
				is UpdatesState.Error -> {
					UpdatesPanel {
						StatusMessageUI(
							title = "We couldn't fetch updates...",
							message = "Please check your connection and try again later",
							icon = R.drawable.rounded_hourglass_disabled_24,
							size = Size.Small,
							modifier = Modifier.padding(16.dp)
						)
					}
				}
				is UpdatesState.Loading -> {
					UpdatesPanel {
						Box(
							modifier = Modifier
								.fillMaxWidth()
								.height(20.dp)
								.clip(RoundedCornerShape(8.dp))
								.shimmerLoading(
									colors = listOf(
										MaterialTheme.colorScheme.surfaceContainerLow,
										MaterialTheme.colorScheme.surfaceContainerHigh,
										MaterialTheme.colorScheme.surfaceContainerLow
									)
								)
						)
					}
				}
				is UpdatesState.Success -> {
					val chartsList = updatesState.charts
					if (chartsList.isEmpty()) {
						UpdatesPanel {
							Text(text = "No updates available")
						}
					} else {
						SectionWrapper(nestedScrollConnection) {
							items(chartsList) { chart ->
								UpdateListItem(chart)
							}
						}
					}
				}
			}
			FilledTonalButton(
				onClick = onFetchUpdates,
				colors = ButtonDefaults.filledTonalButtonColors(
					containerColor = MaterialTheme.colorScheme.primaryContainer,
					contentColor = MaterialTheme.colorScheme.onPrimaryContainer
				),
				enabled = updatesState !is UpdatesState.Loading,
				modifier = Modifier
					.fillMaxWidth()
					.padding(start = 16.dp, end = 16.dp, top = 8.dp)
			) {
				Row(
					horizontalArrangement = Arrangement.spacedBy(ButtonDefaults.IconSpacing),
					verticalAlignment = Alignment.CenterVertically
				) {
					Icon(
						modifier = Modifier
							.run {
								if (updatesState == UpdatesState.Loading) {
									this.infiniteRotation(easing = CubicBezierEasing(
										0.4f, 0.0f, 0.2f, 1.0f
									))
								} else {
									this
								}
							}
							.size(ButtonDefaults.IconSize),
						painter = painterResource(id = R.drawable.rounded_autorenew_24),
						contentDescription = "Check for updates icon"
					)
					Text(text = if (updatesState == UpdatesState.Loading) "Checking for updates..." else "Check for updates")
				}
			}
		}

		// Downloaded section
		Section(
			title = when (localChartsState) {
				is LocalChartsState.Success -> "Downloaded (${localChartsState.charts.size})"
				else -> "Downloaded"
			},
			modifier = Modifier.padding(top = 16.dp),
		) {
			// TODO: Implement other content types
			/*SingleChoiceSegmentedButtonRow(
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
						selected = index == selectedIndex
					) {
						Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
					}
				}
			}*/

			when (localChartsState) {
				is LocalChartsState.Loading -> {
					Box(
						modifier = Modifier.fillMaxSize(),
						contentAlignment = Alignment.Center
					) {
						CircularProgressIndicator(modifier = Modifier.padding(16.dp))
					}
				}
				is LocalChartsState.Error -> {
					StatusMessageUI(
						modifier = Modifier.fillMaxSize(),
						title = "Looks like something went wrong...",
						message = "Please check your connection or try again later",
						icon = R.drawable.rounded_hourglass_disabled_24
					)
				}
				is LocalChartsState.Success -> {
					val chartsList = localChartsState.charts
					if (chartsList.isEmpty()) {
						Box(
							modifier = Modifier.fillMaxSize(),
							contentAlignment = Alignment.Center
						) {
							StatusMessageUI(
								title = "No downloads yet",
								message = "Download some charts to get started",
								icon = R.drawable.rounded_box_24,
								modifier = Modifier
									.padding(bottom = 36.dp)
							)
						}
					} else {
						SectionWrapper(
							nestedScrollConnection = nestedScrollConnection,
							onFabStateChange = onFabStateChange
						){
							item {
								DownloadsSectionsTitle("Charts")
							}
							items(chartsList) { chart ->
								LocalChartPreview(
									chart = chart,
									onNavigateToDetails = { /*TODO*/ }
								)
							}
							/*item {
								DownloadsSectionsTitle("Tour Passes")
							}*/
							/*item {
								DownloadsSectionsTitle("Themes")
							}*/
						}
					}
				}
			}
		}
	}
}

@Composable
private fun UpdatesPanel(content: @Composable () -> Unit) {
	Box(
		modifier = Modifier
			.fillMaxWidth()
			.padding(start = 16.dp, end = 16.dp)
			.background(
				MaterialTheme.colorScheme.surfaceContainerLow,
				RoundedCornerShape(16.dp)
			)
			.padding(16.dp),
		contentAlignment = Alignment.Center
	) {
		content()
	}
}

@Composable
private fun InstallationsSection(
	nestedScrollConnection: NestedScrollConnection,
) {
	Box(
		modifier = Modifier.fillMaxSize(),
		contentAlignment = Alignment.Center,
	) {
		StatusMessageUI(
			title = "Work in progress!",
			message = "This feature still needs some work\nPlease, check back later",
			icon = R.drawable.rounded_hourglass_24,
		)
	}
}

@Composable
private fun DownloadsSectionsTitle(title: String) {
	Box(
		modifier = Modifier
			.fillMaxWidth()
			.padding(16.dp, 0.dp, 0.dp, 8.dp)
	) {
		Text(
			text = title,
			style = MaterialTheme.typography.labelLarge
		)
	}
}