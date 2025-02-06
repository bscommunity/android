package com.meninocoiso.beatstarcommunity.presentation.ui.components.workspace

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
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import com.meninocoiso.beatstarcommunity.presentation.ui.components.TabItem
import com.meninocoiso.beatstarcommunity.presentation.ui.components.TabsUI
import com.meninocoiso.beatstarcommunity.util.AppBarUtils

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
	val screenHeight = LocalConfiguration.current.screenHeightDp

	Column(
		modifier = Modifier
			.offset { IntOffset(0, connection.appBarOffset) }
			.height(collapsibleHeight + fixedHeight + statusBarHeight)
			.fillMaxWidth()
			.background(MaterialTheme.colorScheme.surfaceContainerLow),
		verticalArrangement = Arrangement.Bottom,
		horizontalAlignment = Alignment.CenterHorizontally
	) {
		Box(
			modifier = Modifier
				.weight(1f)
				.zIndex(2f)
				.layout { measurable, constraints ->
					// Measure the composable
					val placeable = measurable.measure(
						constraints.copy(
							maxWidth = constraints.maxWidth,
							maxHeight = screenHeight * 3,
						)
					)

					// Define the width and height of the layout
					val width = constraints.maxWidth
					val height = constraints.maxHeight

					// Define the layout
					layout(width, height) {
						// Place the composable at (0, 0) coordinates
						placeable.placeRelative(0, 0)
					}
				}
				//.background(Color.Red)
				.fillMaxWidth(),
			contentAlignment = Alignment.Center
		) {
			WorkspaceSearchBar(
				topOffset = statusBarHeight,
				modifier = Modifier
					//.background(Color.Green)
					.alpha(connection.appBarOpacity)
			)
		}
		TabsUI(pagerState = pagerState, tabs = workspaceTabsItems, modifier = Modifier.zIndex(-1f))
	}
}