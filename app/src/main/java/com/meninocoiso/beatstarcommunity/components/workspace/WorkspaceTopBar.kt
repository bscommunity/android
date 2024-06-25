package com.meninocoiso.beatstarcommunity.components.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.meninocoiso.beatstarcommunity.components.TabItem
import com.meninocoiso.beatstarcommunity.components.Tabs
import com.meninocoiso.beatstarcommunity.screens.AppBarHeightPercentage
import com.meninocoiso.beatstarcommunity.screens.AppTabsHeight

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
	val appBarHeight = (AppBarHeightPercentage * LocalConfiguration.current.screenHeightDp / 100).dp

	Column(
		modifier = Modifier
			.offset { IntOffset(0, appBarOffset) }
			.heightIn(min = appBarHeight)
			.defaultMinSize(minHeight = AppTabsHeight)
			.background(MaterialTheme.colorScheme.surfaceContainerLow),
		verticalArrangement = Arrangement.SpaceBetween,
		horizontalAlignment = Alignment.CenterHorizontally
	) {
		//Spacer(modifier = Modifier.height(8.dp))
		Box(
			contentAlignment = Alignment.Center,
			//modifier = Modifier.absoluteOffset(y = 2.dp)
		) {
			WorkspaceSearchBar(modifier = Modifier.alpha(appBarOpacity))
		}
		//Spacer(modifier = Modifier.height(4.dp))
		Tabs(pagerState = pagerState, tabs = workspaceTabsItems)
	}
}