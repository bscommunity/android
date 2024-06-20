package com.meninocoiso.beatstarcommunity.components

import android.view.Window
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.meninocoiso.beatstarcommunity.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBarUI(modifier: Modifier? = Modifier) {
	var query by remember { mutableStateOf("") }
	var active by remember { mutableStateOf(false) }
	val historyItems = remember {
		mutableStateListOf(
			"Stars",
			"Mine",
			"Country Girl"
		)
	}

	Box(
		Modifier
			.then(modifier ?: Modifier)
			.fillMaxWidth()
			.semantics { isTraversalGroup = true },
		contentAlignment = Alignment.Center
	) {
		SearchBar(
			query = query,
			onQueryChange = {
				query = it
			},
			onSearch = {
				historyItems.add(it)
				active = false
				query = ""
			},
			tonalElevation = 2.dp,
			active = active,
			onActiveChange = {
				active = it
			},
			placeholder = {
				Text(text = "Search in workshop")
			},
			colors = SearchBarDefaults.colors(
				containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
			),
			leadingIcon = {
				Icon(imageVector = Icons.Default.Search, contentDescription = "Search icon")
			},
			trailingIcon = {
				if (active) {
					Icon(
						modifier = Modifier.clickable {
							if (query.isNotEmpty()) {
								query = ""
							} else {
								active = false
							}
						},
						imageVector = Icons.Default.Close,
						contentDescription = "Close search icon",
					)
				}
			}
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
					Row(modifier = Modifier.padding(all = 16.dp)) {
						Icon(
							painter = painterResource(id = R.drawable.round_history_24),
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
	}
}