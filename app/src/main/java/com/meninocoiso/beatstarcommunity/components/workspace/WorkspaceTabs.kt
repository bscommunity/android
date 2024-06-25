package com.meninocoiso.beatstarcommunity.components.workspace

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

data class WorkspaceTabsItem(
	val title: String,
	val hasNews: Boolean,
	val badgeCount: Int? = null
)

val tabItems = listOf(
	WorkspaceTabsItem(
		title = "Charts",
		hasNews = false
	),
	WorkspaceTabsItem(
		title = "Tour Passes",
		hasNews = false,
		badgeCount = 3
	),
	WorkspaceTabsItem(
		title = "Themes",
		hasNews = false
	)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceTabs(pagerState: PagerState) {
	var selectedTabIndex by remember { mutableIntStateOf(0) }

	LaunchedEffect(selectedTabIndex) {
		pagerState.animateScrollToPage(selectedTabIndex)
	}

	LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
		if (!pagerState.isScrollInProgress) {
			selectedTabIndex = pagerState.currentPage
		}
	}

	SecondaryTabRow(
		modifier = Modifier.fillMaxWidth(),
		selectedTabIndex = selectedTabIndex,
		indicator = {
			TabRowDefaults.SecondaryIndicator(
				Modifier
					.tabIndicatorOffset(selectedTabIndex, matchContentSize = true)
					.clip(RoundedCornerShape(15.dp, 15.dp, 0.dp, 0.dp))
			)
		},
		containerColor = Color.Transparent
	) {
		tabItems.forEachIndexed { index, item ->
			Tab(
				selected = index == selectedTabIndex,
				onClick = {
					selectedTabIndex = index
				},
				unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
				selectedContentColor = MaterialTheme.colorScheme.primary,
				text = { Text(text = item.title, maxLines = 1, overflow = TextOverflow.Ellipsis) }
			)
		}
	}
}