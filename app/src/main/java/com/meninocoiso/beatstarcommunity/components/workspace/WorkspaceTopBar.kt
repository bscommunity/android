package com.meninocoiso.beatstarcommunity.components.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.meninocoiso.beatstarcommunity.components.WorkspaceSearchBar
import com.meninocoiso.beatstarcommunity.screens.AppBarHeight
import com.meninocoiso.beatstarcommunity.screens.AppTabsHeight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceTopBar(appBarOffset: Int, appBarOpacity: Float, pagerState: PagerState) {
	var expanded by rememberSaveable { mutableStateOf(false) }

	Column(
		modifier = Modifier
			.offset { IntOffset(0, appBarOffset) }
			.heightIn(min = AppBarHeight)
			.defaultMinSize(minHeight = AppTabsHeight)
			.background(MaterialTheme.colorScheme.surfaceContainerLow),
		verticalArrangement = Arrangement.SpaceBetween,
		horizontalAlignment = Alignment.CenterHorizontally
	) {
		Spacer(modifier = Modifier.height(8.dp))
		Box(
			contentAlignment = Alignment.Center,
			modifier = Modifier.weight(1f, false)
		) {
			WorkspaceSearchBar(modifier = Modifier.alpha(appBarOpacity))
		}
		WorkspaceTabs(pagerState = pagerState)
	}
}