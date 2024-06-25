package com.meninocoiso.beatstarcommunity.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.meninocoiso.beatstarcommunity.components.TabItem
import com.meninocoiso.beatstarcommunity.components.Tabs
import com.meninocoiso.beatstarcommunity.utils.CollapsingAppBarNestedScrollConnection

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

val TabsHeight = 80.dp

@Composable
fun Updates() {
	Column(
		modifier = Modifier
			.fillMaxSize(),
		verticalArrangement = Arrangement.Top,
		horizontalAlignment = Alignment.CenterHorizontally
	) {
		val pagerState = rememberPagerState {
			updatesTabsItems.size
		}

		val appBarMaxHeightPx = with(LocalDensity.current) { TabsHeight.roundToPx() }
		val connection = remember(appBarMaxHeightPx) {
			CollapsingAppBarNestedScrollConnection(appBarMaxHeightPx)
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
						0 -> WorkspaceSection(connection)
						1 -> InstallationsSection(connection)
					}
				}
			}

			Box(
				modifier = Modifier
					.offset { IntOffset(0, connection.appBarOffset) }
					.heightIn(min = TabsHeight)
					.defaultMinSize(minHeight = TabsHeight),
				contentAlignment = Alignment.BottomCenter
			) {
				Tabs(pagerState = pagerState, tabs = updatesTabsItems)
			}
		}
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
		contentPadding = PaddingValues(16.dp)
	) {
		content()
	}
}

@Composable
private fun WorkspaceSection(
	nestedScrollConnection: NestedScrollConnection,
) {
	SectionWrapper(nestedScrollConnection = nestedScrollConnection) {
		val list = (0..100).map { it.toString() }
		items(count = list.size) {
			Text(text = "Teste - $it")
		}
	}
}

@Composable
private fun InstallationsSection(
	nestedScrollConnection: NestedScrollConnection,
) {
	SectionWrapper(nestedScrollConnection = nestedScrollConnection) {
		val list = (0..25).map { it.toString() }
		items(count = list.size) {
			Text(text = "Parte 2 - $it")
		}
	}
}