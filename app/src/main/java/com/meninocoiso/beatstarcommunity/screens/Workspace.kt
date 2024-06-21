package com.meninocoiso.beatstarcommunity.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.meninocoiso.beatstarcommunity.R
import com.meninocoiso.beatstarcommunity.components.WorkspaceTabs
import com.meninocoiso.beatstarcommunity.components.WorkspaceTopBar
import com.meninocoiso.beatstarcommunity.components.tabItems
import java.io.Console

val AppBarHeight = 173.dp
val AppTabsHeight = 90.dp

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
		// println("MAX: $appBarMaxHeight")
		// println("ACTUAL: $appBarOffset")
		// println("SUBTRACTED: ${appBarOffset / appBarMaxHeight}")
		val consumed = appBarOffset - previousOffset
		appBarOpacity = 1f + (appBarOffset / appBarMaxHeight.toFloat())
		// println("OPACITY: $appBarOpacity")
		return Offset(0f, consumed.toFloat())
	}
}

data class WorkspaceChip(
	val id: Int,
	val title: String,
	val icon: Int? = null,
)

val chipItems = listOf(
	WorkspaceChip(
		id = 1,
		title = "Weekly Rank"
	),
	WorkspaceChip(
		id = 2,
		title = "Editor's Choice",
		icon = R.drawable.rounded_award_star_24
	),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Workspace() {
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

	var selectedChipIndex by remember { mutableIntStateOf(0) }

	Box {
		Column() {
			Spacer(
				Modifier
					.height(spaceHeight)
			)
			Row {

			}
			HorizontalPager(
				state = pagerState
			) { index ->
				when (
					index
				) {
					0 -> ChartsSection(connection)
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

@Composable
fun ChartsSection(nestedScrollConnection: NestedScrollConnection) {
	LazyColumn(
		contentPadding = PaddingValues(vertical = 16.dp),
		modifier = Modifier
			.fillMaxSize()
			.nestedScroll(nestedScrollConnection),
		verticalArrangement = Arrangement.spacedBy(8.dp)
	) {
		val list = (0..75).map { it.toString() }
		items(count = list.size) {
			Text(
				text = list[it],
				style = MaterialTheme.typography.bodyLarge,
				modifier = Modifier
					.fillMaxWidth()
					/*.then(
						if (it.toInt() % 2 == 0) {
							Modifier.background(Color.Red)
						} else {
							Modifier.background(Color.Blue)
						}
					)*/
					.padding(horizontal = 16.dp)
			)
		}
	}
}

@Composable
fun TourPassesSection(nestedScrollConnection: NestedScrollConnection) {
	LazyColumn(
		contentPadding = PaddingValues(vertical = 16.dp),
		modifier = Modifier
			.fillMaxSize()
			.nestedScroll(nestedScrollConnection),
		verticalArrangement = Arrangement.spacedBy(8.dp)
	) {
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
	LazyColumn(
		contentPadding = PaddingValues(vertical = 16.dp),
		modifier = Modifier
			.fillMaxSize()
			.nestedScroll(nestedScrollConnection),
		verticalArrangement = Arrangement.spacedBy(8.dp)
	) {
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