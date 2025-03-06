package com.meninocoiso.beatstarcommunity.presentation.screens

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
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.meninocoiso.beatstarcommunity.R
import com.meninocoiso.beatstarcommunity.presentation.navigation.UpdatesSection
import com.meninocoiso.beatstarcommunity.presentation.ui.components.StatusMessageUI
import com.meninocoiso.beatstarcommunity.presentation.ui.components.TabItem
import com.meninocoiso.beatstarcommunity.presentation.ui.components.TabsUI
import com.meninocoiso.beatstarcommunity.presentation.ui.components.chart.LocalChartPreview
import com.meninocoiso.beatstarcommunity.presentation.ui.components.layout.CoverArt
import com.meninocoiso.beatstarcommunity.presentation.ui.components.layout.Section
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
	viewModel: UpdatesViewModel = hiltViewModel(),
) {
	val updatesState by viewModel.updatesState.collectAsState()
	val localChartsState by viewModel.localChartsState.collectAsState()

	val horizontalPagerState = rememberPagerState {
		updatesTabsItems.size
	}

	// Update local charts every time the screen is opened
	LaunchedEffect(Unit) {
		viewModel.loadLocalCharts()
		//viewModel.fetchUpdates(installedCharts = localChartsState.charts)
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
	content: LazyListScope.() -> Unit
) {
	LazyColumn(
		modifier = Modifier
			.fillMaxSize()
			.nestedScroll(nestedScrollConnection),
		contentPadding = PaddingValues(horizontal = 16.dp),
		verticalArrangement = Arrangement.spacedBy(8.dp),
		horizontalAlignment = Alignment.CenterHorizontally
	) {
		content()
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

@Composable
fun WorkspaceSection(
	updatesState: UpdatesState,
	localChartsState: LocalChartsState,
	nestedScrollConnection: NestedScrollConnection,
) {
	var selectedIndex by remember { mutableIntStateOf(-1) }
	val options = listOf("Charts", "Tour Passes", "Themes")

	Column {
		// Updates section remains unchanged
		Section(
			title = when (updatesState) {
				is UpdatesState.Success -> "Updates available (${updatesState.charts.size})"
				else -> null
			},
			thickness = 0.dp,
			titleModifier = Modifier.padding(top = 8.dp),
		) {
			when (updatesState) {
				is UpdatesState.Success -> {
					val chartsList = updatesState.charts
					if (chartsList.isEmpty()) {
						Box(
							modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
							contentAlignment = Alignment.Center
						) {
							Text(
								text = "No updates found",
								modifier = Modifier.padding(horizontal = 16.dp)
							)
						}
					} else {
						SectionWrapper(nestedScrollConnection) {
							items(chartsList) { chart ->
								ListItem(
									modifier = Modifier
										.clip(RoundedCornerShape(16.dp)),
									colors = ListItemDefaults.colors(
										containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
									),
									leadingContent = {
										CoverArt(
											difficulty = null,
											borderRadius = 2.dp,
											url = chart.coverUrl,
											size = 40.dp
										)
									},
									headlineContent = {
										Text(
											text = chart.track,
											style = MaterialTheme.typography.titleMedium,
											maxLines = 1,
											overflow = TextOverflow.Ellipsis,
											lineHeight = TextUnit(1f, TextUnitType.Em)
										)
									},
									supportingContent = {
										Text(
											text = "Update from v1 → v2",
											style = MaterialTheme.typography.bodyMedium,
											lineHeight = TextUnit(1f, TextUnitType.Em)
										)
									},
									trailingContent = {
										IconButton(
											onClick = { /*TODO*/ },
											colors = IconButtonDefaults.iconButtonColors(
												containerColor = MaterialTheme.colorScheme.primary,
												contentColor = MaterialTheme.colorScheme.onPrimary
											)
										) {
											Icon(
												painter = painterResource(id = R.drawable.rounded_download_24),
												contentDescription = "Update icon"
											)
										}
									}
								)
							}
						}
					}
				}
				is UpdatesState.Error -> {
					StatusMessageUI(
						title = "We couldn't fetch updates...",
						message = "Please check your connection or try again later",
						icon = R.drawable.rounded_hourglass_disabled_24
					)
				}
				is UpdatesState.Loading -> {
					Box(
						modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
						contentAlignment = Alignment.Center
					) {
						Text(
							text = "Searching for updates...",
							modifier = Modifier.padding(horizontal = 16.dp)
						)
					}
				}
			}
			FilledTonalButton(
				onClick = { /*TODO*/ },
				colors = ButtonDefaults.filledTonalButtonColors(
					containerColor = MaterialTheme.colorScheme.primaryContainer,
					contentColor = MaterialTheme.colorScheme.onPrimaryContainer
				),
				modifier = Modifier
					.fillMaxWidth()
					.padding(start = 16.dp, end = 16.dp, top = 8.dp)
			) {
				Row(
					horizontalArrangement = Arrangement.spacedBy(ButtonDefaults.IconSpacing),
					verticalAlignment = Alignment.CenterVertically
				) {
					Icon(
						modifier = Modifier.size(ButtonDefaults.IconSize),
						painter = painterResource(id = R.drawable.rounded_autorenew_24),
						contentDescription = "Check for updates icon"
					)
					Text(text = "Check for updates")
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
						title = "Looks like something went wrong...",
						message = "Please check your connection or try again later",
						icon = R.drawable.rounded_hourglass_disabled_24
					)
				}
				is LocalChartsState.Success -> {
					val chartsList = localChartsState.charts
					if (chartsList.isEmpty()) {
						Text(text = "No downloaded charts available", modifier = Modifier.padding(16.dp))
					} else {
						SectionWrapper(
							nestedScrollConnection = nestedScrollConnection
						){
							item {
								DownloadsSectionsTitle("Charts")
							}
							items(chartsList) { chart ->
								LocalChartPreview(
									chart = chart,
									version = 1,
									onNavigateToDetails = { /*TODO*/ }
								)
							}
							/*item {
								DownloadsSectionsTitle("Tour Passes")
							}*/
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