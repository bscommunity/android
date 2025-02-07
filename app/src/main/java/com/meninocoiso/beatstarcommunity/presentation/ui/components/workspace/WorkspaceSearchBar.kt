package com.meninocoiso.beatstarcommunity.presentation.ui.components.workspace

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.meninocoiso.beatstarcommunity.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceSearchBar(
	topOffset: Dp,
	modifier: Modifier? = Modifier
) {
	var query by rememberSaveable { mutableStateOf("") }
	var isExpanded by rememberSaveable { mutableStateOf(false) }

	val historyItems = remember {
		mutableStateListOf(
			"Stars",
			"Mine",
			"What You Know"
		)
	}

	// TODO: Currently, the BottomSheet reset it's state to partiallyExpanded when any children are resized (collapsable, tags being reorganized after selection, etc.)
	val filterSheetState = rememberModalBottomSheetState(
		skipPartiallyExpanded = true,
		/*confirmValueChange = {
			false
		}*/
	)
	val scope = rememberCoroutineScope()
	var isFilterSheetOpen by rememberSaveable {
		mutableStateOf(false)
	}

	val filtersList = remember {
		mutableStateListOf<String>()
	}

	SearchBar(
		windowInsets = WindowInsets(0.dp, topOffset, 0.dp, 0.dp),
		modifier = (modifier ?: Modifier),
		// TODO: Workaround for some hidden property of SearchBar I was unable to found. It applies some padding to the component top
		inputField = {
			SearchBarDefaults.InputField(
				modifier = Modifier
					.width(LocalConfiguration.current.screenWidthDp.dp - 32.dp),
				query = query,
				onQueryChange = { query = it },
				onSearch = { isExpanded = false },
				expanded = isExpanded,
				onExpandedChange = { isExpanded = it },
				placeholder = { Text("Search in workshop") },
				leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
				trailingIcon = {
					if (isExpanded) {
						Icon(
							modifier = Modifier.clickable {
								if (query.isNotEmpty()) {
									query = ""
								} else {
									isExpanded = false
								}
							},
							imageVector = Icons.Default.Close,
							contentDescription = "Close search icon",
						)
					} else {
						IconButton(onClick = { isFilterSheetOpen = true }) {
							Icon(
								painter = painterResource(id = R.drawable.outline_filter_alt_24),
								contentDescription = "Filter icon",
							)
						}
					}
				}
			)
		},
		expanded = isExpanded,
		onExpandedChange = { isExpanded = it },
	) {
		Column(
			Modifier
				.verticalScroll(rememberScrollState())
		) {
			Text(
				text = "Recent searches",
				style = MaterialTheme.typography.titleSmall,
				modifier = Modifier.padding(
					start = 16.dp,
					end = 16.dp,
					top = 24.dp,
					bottom = 8.dp
				)
			)
			historyItems.forEach {
				Row(modifier = Modifier
					.padding(all = 16.dp)
					.clickable {
						query = it
					}) {
					Icon(
						painter = painterResource(id = R.drawable.rounded_history_24),
						contentDescription = "History icon",
						modifier = Modifier.padding(end = 10.dp)
					)
					Text(
						text = it
					)
				}
			}
		}
	}

	if (isFilterSheetOpen) {
		WorkspaceFilterBottomSheet(
			filtersList = filtersList,
			sheetState = filterSheetState,
			onDismissRequest = {
				isFilterSheetOpen = false
			},
			onClose = {
				/*scope.launch { filterSheetState.hide() }.invokeOnCompletion {
					if (!filterSheetState.isVisible) {
						isFilterSheetOpen = false
					}
				}*/
			})
	}
}