package com.meninocoiso.beatstarcommunity.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceTopBar(scrollBehavior: TopAppBarScrollBehavior) {
	Column(
		modifier = Modifier
			.fillMaxWidth()
	) {
		SearchBarUI()
		WorkspaceTabs()
	}
}