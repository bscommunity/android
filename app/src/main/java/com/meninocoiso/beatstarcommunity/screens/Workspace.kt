package com.meninocoiso.beatstarcommunity.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.meninocoiso.beatstarcommunity.components.ChartPreview
import com.meninocoiso.beatstarcommunity.components.workspace.WorkspaceChips
import com.meninocoiso.beatstarcommunity.components.workspace.WorkspaceTopBar
import com.meninocoiso.beatstarcommunity.components.workspace.tabItems
import com.meninocoiso.beatstarcommunity.data.classes.Chart
import com.meninocoiso.beatstarcommunity.data.classes.Song
import com.meninocoiso.beatstarcommunity.data.classes.User
import com.meninocoiso.beatstarcommunity.data.enums.DifficultyEnum
import com.meninocoiso.beatstarcommunity.utils.DateUtils
import java.util.Date

val AppBarHeight = 155.dp
val AppTabsHeight = 80.dp

private class CollapsingAppBarNestedScrollConnection(
	val appBarMaxHeight: Int
) : NestedScrollConnection {

	var appBarOffset: Int by mutableIntStateOf(0)
		private set

	var appBarOpacity: Float by mutableFloatStateOf(1f)
		private set

	override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
		val delta = available.y.toInt()
		val newOffset = appBarOffset + delta
		val previousOffset = appBarOffset
		appBarOffset = newOffset.coerceIn(-appBarMaxHeight, 0)
		val consumed = appBarOffset - previousOffset
		appBarOpacity = 1f + (appBarOffset / appBarMaxHeight.toFloat())
		return Offset(0f, consumed.toFloat())
	}
}

@Composable
fun Workspace(onNavigateToDetails: () -> Unit) {
	val pagerState = rememberPagerState {
		tabItems.size
	}

	val appBarMaxHeightPx = with(LocalDensity.current) { AppBarHeight.roundToPx() }
	val searchBarMaxHeightPx =
		with(LocalDensity.current) { (AppBarHeight - AppTabsHeight).roundToPx() }
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

	Box {
		Column() {
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
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SectionWrapper(
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

val placeholderChart = Chart(
	id = 1,
	song = Song(
		title = "Overdrive",
		duration = 1532.25f,
		artists = listOf("Metrik", "Grafix"),
		isExplicit = false,
		coverArtUrl = "https://picsum.photos/76",
		uploadedBy = User(
			username = "meninocoiso",
			email = "william.henry.moody@my-own-personal-domain.com",
			avatarUrl = "https://github.com/theduardomaciel.png",
			createdAt = DateUtils.getRandomDateInYear(2023),
		)
	),
	createdAt = DateUtils.getRandomDateInYear(2023),
	lastUpdatedAt = DateUtils.getRandomDateInYear(2023),
	url = "",
	difficulty = DifficultyEnum.EXTREME,
	notesAmount = 435,
	knownIssues = listOf(
		"Missing clap effects on bridge",
		"Wrong direction swipe effect",
		"Unsyncronized section after drop"
	),
	authors = listOf(
		User(
			username = "meninocoiso",
			email = "teste@gmail.com",
			avatarUrl = "https://github.com/theduardomaciel.png",
			createdAt = Date(),
			charts = null
		),
		User(
			username = "oCosmo55",
			email = "teste@gmail.com",
			avatarUrl = "https://github.com/oCosmo55.png",
			createdAt = Date(),
		)
	)
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChartsSection(nestedScrollConnection: NestedScrollConnection, onNavigateToDetails: () -> Unit) {
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
fun TourPassesSection(nestedScrollConnection: NestedScrollConnection) {
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
fun ThemesSection(nestedScrollConnection: NestedScrollConnection) {
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