package com.meninocoiso.beatstarcommunity.components.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.IntOffset
import com.meninocoiso.beatstarcommunity.components.TabItem
import com.meninocoiso.beatstarcommunity.components.Tabs
import com.meninocoiso.beatstarcommunity.screens.SearchBarHeight
import com.meninocoiso.beatstarcommunity.screens.WorkspaceTabsHeight

val workspaceTabsItems = listOf(
	TabItem(
		title = "Charts",
		hasNews = false
	),
	TabItem(
		title = "Tour Passes",
		hasNews = false,
		badgeCount = 3
	),
	TabItem(
		title = "Themes",
		hasNews = false
	)
)

@Composable
fun WorkspaceTopBar(appBarOffset: Int, appBarOpacity: Float, pagerState: PagerState) {
	val statusBarHeight = WindowInsets.systemBars.asPaddingValues().calculateTopPadding()

	Column(
		modifier = Modifier
			.offset { IntOffset(0, appBarOffset) }
			.defaultMinSize(minHeight = SearchBarHeight + WorkspaceTabsHeight + statusBarHeight)
			.fillMaxWidth()
			.background(MaterialTheme.colorScheme.surfaceContainerLow),
		verticalArrangement = Arrangement.SpaceBetween,
		horizontalAlignment = Alignment.CenterHorizontally
	) {
		Box(
			modifier = Modifier
				.defaultMinSize(
					minHeight = SearchBarHeight
				),
			contentAlignment = Alignment.Center
		) {
			WorkspaceSearchBar(
				modifier = Modifier
					.alpha(appBarOpacity)
			)
		}
		Box(
			modifier = Modifier
				//.background(Color.Blue)
				.height(WorkspaceTabsHeight),
			contentAlignment = Alignment.Center
		) {
			//Text(text = "Tabs")
			Tabs(pagerState = pagerState, tabs = workspaceTabsItems)
		}
		//
	}
}