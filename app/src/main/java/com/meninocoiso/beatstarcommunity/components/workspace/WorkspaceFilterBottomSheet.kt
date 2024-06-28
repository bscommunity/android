package com.meninocoiso.beatstarcommunity.components.workspace

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.meninocoiso.beatstarcommunity.R
import com.meninocoiso.beatstarcommunity.components.CollapsableSection
import com.meninocoiso.beatstarcommunity.data.difficultiesList
import com.meninocoiso.beatstarcommunity.data.genresList
import java.util.Locale

@Composable
fun ExtendedFilterChip(
	id: String,
	filtersList: SnapshotStateList<String>,
	leadingIcon: @Composable() (() -> Unit)? = null,
	label: @Composable () -> Unit
) {
	FilterChip(
		selected = filtersList.contains(id),
		onClick = {
			if (filtersList.contains(id)) {
				filtersList.remove(id)
			} else {
				filtersList.add(id)
			}
		},
		leadingIcon = {
			if (filtersList.contains(id)) {
				Icon(
					modifier = Modifier.size(FilterChipDefaults.IconSize),
					imageVector = Icons.Default.Check,
					contentDescription = "Selected"
				)
			} else {
				leadingIcon?.invoke()
			}
		},
		label = label
	)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceFilterBottomSheet(
	filtersList: SnapshotStateList<String>,
	sheetState: SheetState,
	onDismissRequest: () -> Unit,
	onClose: () -> Unit
) {
	ModalBottomSheet(
		sheetState = sheetState,
		onDismissRequest = onDismissRequest,
	) {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(horizontal = 16.dp, vertical = 12.dp),
			horizontalArrangement = Arrangement.SpaceBetween,
			verticalAlignment = Alignment.CenterVertically
		) {
			Text(text = "Filters", style = MaterialTheme.typography.titleLarge)
			IconButton(onClick = onClose) {
				Icon(imageVector = Icons.Default.Close, contentDescription = "Close BottomSheet")
			}
		}
		Column(
			modifier = Modifier.verticalScroll(rememberScrollState())
		) {
			CollapsableSection(title = "Awarded") {
				ExtendedFilterChip(
					filtersList = filtersList,
					id = "editor_choice",
					leadingIcon = {
						Icon(
							modifier = Modifier.size(FilterChipDefaults.IconSize),
							painter = painterResource(id = R.drawable.rounded_award_star_24),
							contentDescription = "Editor's Choice"
						)
					},
					label = {
						Text(text = "Editor’s Choice")
					}
				)
				ExtendedFilterChip(
					filtersList = filtersList,
					id = "featured",
					leadingIcon = {
						Icon(
							modifier = Modifier.size(FilterChipDefaults.IconSize),
							painter = painterResource(id = R.drawable.rounded_local_fire_department_24),
							contentDescription = "Featured"
						)
					},
					label = {
						Text(text = "Featured")
					}
				)
				ExtendedFilterChip(
					filtersList = filtersList,
					id = "trending",
					leadingIcon = {
						Icon(
							modifier = Modifier.size(FilterChipDefaults.IconSize),
							painter = painterResource(id = R.drawable.rounded_trending_up_24),
							contentDescription = "Trending"
						)
					},
					label = {
						Text(text = "Trending")
					}
				)
			}
			CollapsableSection(title = "Difficulty") {
				difficultiesList.forEach {
					ExtendedFilterChip(
						filtersList = filtersList,
						id = it.id.toString().lowercase(Locale.ROOT),
						label = {
							Text(text = it.name)
						}
					)
				}
			}
			CollapsableSection(title = "Genre") {
				genresList.forEach {
					ExtendedFilterChip(
						filtersList = filtersList,
						id = it.name,
						leadingIcon = {
							Icon(
								painter = painterResource(id = it.icon),
								contentDescription = it.name
							)
						},
						label = {
							Text(text = it.name.lowercase()
								.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() })
						}
					)
				}
			}
			CollapsableSection(title = "Version") {
				ExtendedFilterChip(
					filtersList = filtersList,
					id = "default",
					label = {
						Text(text = "Default")
					}
				)
				ExtendedFilterChip(
					filtersList = filtersList,
					id = "deluxe",
					label = {
						Text(text = "Deluxe")
					}
				)
			}
		}
	}
}