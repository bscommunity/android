package com.meninocoiso.beatstarcommunity.screens.updates

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.meninocoiso.beatstarcommunity.R
import com.meninocoiso.beatstarcommunity.components.CoverArt
import com.meninocoiso.beatstarcommunity.components.LocalChartPreview
import com.meninocoiso.beatstarcommunity.components.Section
import com.meninocoiso.beatstarcommunity.components.TabItem
import com.meninocoiso.beatstarcommunity.components.Tabs
import com.meninocoiso.beatstarcommunity.data.placeholderChart
import com.meninocoiso.beatstarcommunity.domain.model.Chart
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
fun UpdatesScreen() {
	val chartsToUpdate = (0..3).map {
		placeholderChart
	}

	val downloadedCharts = (0..25).map {
		placeholderChart
	}

	val horizontalPagerState = rememberPagerState {
		updatesTabsItems.size
	}

	val (connection, spaceHeight, statusBarHeight) = AppBarUtils.getConnection(collapsableHeight = TabsHeight)

	Column {
		Spacer(
			Modifier
				.height(spaceHeight)
		)

		HorizontalPager(
			state = horizontalPagerState
		) { index ->
			when (
				index
			) {
				0 -> WorkspaceSection(
					chartsToUpdate = chartsToUpdate,
					downloadedCharts = downloadedCharts,
					connection
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
		Tabs(
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
private fun WorkspaceSection(
	chartsToUpdate: List<Chart>,
	downloadedCharts: List<Chart>,
	nestedScrollConnection: NestedScrollConnection,
) {
	val verticalGap = 8.dp

	var selectedIndex by remember { mutableIntStateOf(-1) }
	val options = listOf("Charts", "Tour Passes", "Themes")

	SectionWrapper(nestedScrollConnection) {
		Section(
			title = "Updates available (${chartsToUpdate.size})",
			thickness = 0.dp,
			titleModifier = Modifier.padding(top = 8.dp),
		) {
			LazyColumn(
				modifier = Modifier.height(
					(UpdatableChartPreviewHeight * chartsToUpdate.size) + (verticalGap * (chartsToUpdate.size - 1))
				),
				userScrollEnabled = false,
				contentPadding = PaddingValues(horizontal = 16.dp),
				verticalArrangement = Arrangement.spacedBy(verticalGap),
				horizontalAlignment = Alignment.CenterHorizontally
			) {
				items(chartsToUpdate) { chart ->
					ListItem(
						modifier = Modifier
							.height(UpdatableChartPreviewHeight)
							.clip(RoundedCornerShape(16.dp)),
						colors = ListItemDefaults.colors(
							containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
						),
						leadingContent = {
							CoverArt(
								difficulty = null,
								borderRadius = 2.dp,
								url = chart.song.coverArtUrl,
								size = 40.dp
							)
						},
						headlineContent = {
							Text(
								text = chart.song.title,
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
		Section(
			title = "Downloaded (${downloadedCharts.size})",
			modifier = Modifier.padding(top = 16.dp),
		) {
			SingleChoiceSegmentedButtonRow(
				modifier = Modifier
					.fillMaxWidth()
					.padding(16.dp, 0.dp, 16.dp, 16.dp)
			) {
				options.forEachIndexed { index, label ->
					SegmentedButton(
						shape = SegmentedButtonDefaults.itemShape(
							index = index,
							count = options.size
						),
						onClick = {
							selectedIndex = if (selectedIndex != index) {
								index
							} else {
								-1
							}
						},
						selected = index == selectedIndex
					) {
						Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
					}
				}
			}
			LazyColumn(
				modifier = Modifier.height(
					(LocalChartPreviewHeight * downloadedCharts.size) + (verticalGap * (downloadedCharts.size - 1))
				),
				userScrollEnabled = false,
				contentPadding = PaddingValues(horizontal = 16.dp),
				verticalArrangement = Arrangement.spacedBy(verticalGap),
				horizontalAlignment = Alignment.CenterHorizontally
			) {
				item {
					DownloadsSectionsTitle("Charts")
				}
				items(downloadedCharts) { chart ->
					LocalChartPreview(
						chart = chart,
						version = 1,
						modifier = Modifier.height(LocalChartPreviewHeight)
					)
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