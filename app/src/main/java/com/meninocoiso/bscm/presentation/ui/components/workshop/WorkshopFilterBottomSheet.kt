package com.meninocoiso.bscm.presentation.ui.components.workshop

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.domain.lists.getDifficultiesList
import com.meninocoiso.bscm.domain.lists.getGenresList
import com.meninocoiso.bscm.presentation.ui.components.CollapsableSection
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
				/*leadingIcon?.invoke()*/
			}
		},
		label = label
	)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkshopFilterBottomSheet(
	filtersList: SnapshotStateList<String>,
	sheetState: SheetState,
	onDismissRequest: () -> Unit,
	onClose: () -> Unit
) {
	val difficultiesList = getDifficultiesList()
	val genresList = getGenresList()
	
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
			Text(text = stringResource(R.string.filters), style = MaterialTheme.typography.titleLarge)
			IconButton(onClick = onClose) {
				Icon(imageVector = Icons.Default.Close, contentDescription = stringResource(R.string.close_bottomsheet))
			}
		}
		Column(
			modifier = Modifier.verticalScroll(rememberScrollState())
		) {
			CollapsableSection(title = stringResource(R.string.awarded)) {
				ExtendedFilterChip(
					filtersList = filtersList,
					id = "editor_choice",
					leadingIcon = {
						Icon(
							modifier = Modifier.size(FilterChipDefaults.IconSize),
							painter = painterResource(id = R.drawable.rounded_award_star_24),
							contentDescription = null
						)
					},
					label = {
						Text(text = stringResource(R.string.editors_choice))
					}
				)
				ExtendedFilterChip(
					filtersList = filtersList,
					id = "featured",
					leadingIcon = {
						Icon(
							modifier = Modifier.size(FilterChipDefaults.IconSize),
							painter = painterResource(id = R.drawable.rounded_local_fire_department_24),
							contentDescription = null
						)
					},
					label = {
						Text(text = stringResource(R.string.featured))
					}
				)
				ExtendedFilterChip(
					filtersList = filtersList,
					id = "trending",
					leadingIcon = {
						Icon(
							modifier = Modifier.size(FilterChipDefaults.IconSize),
							painter = painterResource(id = R.drawable.rounded_trending_up_24),
							contentDescription = null
						)
					},
					label = {
						Text(text = stringResource(R.string.trending))
					}
				)
			}
			CollapsableSection(title = stringResource(R.string.difficulty)) {
				difficultiesList.forEach {
					ExtendedFilterChip(
						filtersList = filtersList,
						id = it.label.lowercase(Locale.ROOT),
						label = {
							Text(text = it.label)
						}
					)
				}
			}
			CollapsableSection(title = stringResource(R.string.genre)) {
				genresList.forEach {
					ExtendedFilterChip(
						filtersList = filtersList,
						id = it.label,
						leadingIcon = {
							Icon(
								painter = painterResource(id = it.icon),
								contentDescription = it.label
							)
						},
						label = {
							Text(text = it.label.lowercase()
								.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() })
						}
					)
				}
			}
			CollapsableSection(title = stringResource(R.string.version)) {
				ExtendedFilterChip(
					filtersList = filtersList,
					id = "default",
					label = {
						Text(text = stringResource(R.string.no_deluxe))
					}
				)
				ExtendedFilterChip(
					filtersList = filtersList,
					id = "deluxe",
					label = {
						Text(text = stringResource(R.string.deluxe))
					}
				)
			}
		}
	}
}