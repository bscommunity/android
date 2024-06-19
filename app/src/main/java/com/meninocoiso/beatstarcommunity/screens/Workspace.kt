package com.meninocoiso.beatstarcommunity.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.meninocoiso.beatstarcommunity.components.WorkspaceTopBar
import com.meninocoiso.beatstarcommunity.components.tabItems

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Workspace() {
	val scrollBehavior =
		TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

	val pagerState = rememberPagerState {
		tabItems.size
	}

	// val appBarOffset by remember { mutableIntStateOf(0) }

	Scaffold(topBar = {
		WorkspaceTopBar(scrollBehavior)
	}) { innerPadding ->
		HorizontalPager(
			state = pagerState,
			modifier = Modifier
				.fillMaxSize()
				.padding(innerPadding)
		) { index ->
			when (
				index
			) {
				0 -> ChartsSection()
				1 -> TourPassesSection()
				2 -> ThemesSection()
			}
		}
	}
}

@Composable
fun ChartsSection() {
	LazyColumn(
		modifier = Modifier
			.fillMaxSize()
			.nestedScroll(scrollBehavior.nestedScrollConnection),
		verticalArrangement = Arrangement.spacedBy(8.dp)
	) {
		val list = (0..75).map { it.toString() }
		items(count = list.size) {
			Text(
				text = list[it],
				style = MaterialTheme.typography.bodyLarge,
				modifier = Modifier
					.fillMaxWidth()
					.padding(horizontal = 16.dp)
			)
		}
	}
}

@Composable
fun TourPassesSection() {
	LazyColumn(
		modifier = Modifier
			.fillMaxSize()
			.nestedScroll(scrollBehavior.nestedScrollConnection),
		verticalArrangement = Arrangement.spacedBy(8.dp)
	) {
		val list = (0..75).map { it.toString() }
		items(count = list.size) {
			Text(
				text = list[it],
				style = MaterialTheme.typography.bodyLarge,
				modifier = Modifier
					.fillMaxWidth()
					.padding(horizontal = 16.dp)
			)
		}
	}
}

@Composable
fun ThemesSection() {
	LazyColumn(
		modifier = Modifier
			.fillMaxSize()
			.nestedScroll(scrollBehavior.nestedScrollConnection),
		verticalArrangement = Arrangement.spacedBy(8.dp)
	) {
		val list = (0..75).map { it.toString() }
		items(count = list.size) {
			Text(
				text = list[it],
				style = MaterialTheme.typography.bodyLarge,
				modifier = Modifier
					.fillMaxWidth()
					.padding(horizontal = 16.dp)
			)
		}
	}
}