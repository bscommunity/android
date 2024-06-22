package com.meninocoiso.beatstarcommunity.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceTopBar(appBarOffset: Int, appBarOpacity: Float, pagerState: PagerState) {
	Column(
		modifier = Modifier
			.offset { IntOffset(0, appBarOffset) }
			.background(MaterialTheme.colorScheme.surfaceContainerLow),
		verticalArrangement = Arrangement.spacedBy(2.dp)
	) {
		WorkspaceSearchBar(modifier = Modifier.alpha(appBarOpacity))
		Spacer(modifier = Modifier.height(8.dp))
		WorkspaceTabs(pagerState = pagerState)
	}
}