package com.meninocoiso.bscm.presentation.ui.components.workshop

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.presentation.ui.components.TabItem
import com.meninocoiso.bscm.presentation.ui.components.TabsUI
import com.meninocoiso.bscm.util.AppBarUtils

@Composable
fun getWorkshopTabsItems(): List<TabItem> {
	return listOf(
		TabItem(
			title = stringResource(R.string.charts),
			hasNews = false
		),
		TabItem(
			title = stringResource(R.string.tour_passes),
			hasNews = false,
			badgeCount = 3
		),
		TabItem(
			title = stringResource(R.string.themes),
			hasNews = false
		)
	)
}

@Composable
fun WorkshopTopBar(
	connection: AppBarUtils.CollapsingAppBarNestedScrollConnection,
	appBarHeights: Triple<Dp, Dp, Dp>,
	pagerState: PagerState,
	content: @Composable () -> Unit,
) {
	val (collapsibleHeight, fixedHeight, statusBarHeight) = appBarHeights

	val workshopTabsItems = getWorkshopTabsItems()
	
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
				.fillMaxWidth(),
			contentAlignment = Alignment.Center
		) {
            content()
		}
		TabsUI(pagerState = pagerState, tabs = workshopTabsItems, modifier = Modifier.zIndex(-1f))
	}
}