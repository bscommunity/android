package com.meninocoiso.beatstarcommunity.components.workspace

import NonClippingLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import com.meninocoiso.beatstarcommunity.components.TabItem
import com.meninocoiso.beatstarcommunity.components.Tabs
import com.meninocoiso.beatstarcommunity.utils.AppBarUtils

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
fun WorkspaceTopBar(
	connection: AppBarUtils.CollapsingAppBarNestedScrollConnection,
	appBarHeights: Triple<Dp, Dp, Dp>,
	pagerState: PagerState
) {
	val (collapsibleHeight, fixedHeight, statusBarHeight) = appBarHeights

	Column(
		modifier = Modifier
			.offset { IntOffset(0, connection.appBarOffset) }
			.height(collapsibleHeight + fixedHeight + statusBarHeight)
			.fillMaxWidth()
			.background(MaterialTheme.colorScheme.surfaceContainerLow),
		verticalArrangement = Arrangement.SpaceBetween,
		horizontalAlignment = Alignment.CenterHorizontally
	) {
		NonClippingLayout {
			Box(
				modifier = Modifier
					.zIndex(2f)
					.fillMaxWidth(),
				contentAlignment = Alignment.Center
			) {
				WorkspaceSearchBar(
					modifier = Modifier
						.alpha(connection.appBarOpacity)
				)
			}
		}
		Tabs(pagerState = pagerState, tabs = workspaceTabsItems)
	}
}