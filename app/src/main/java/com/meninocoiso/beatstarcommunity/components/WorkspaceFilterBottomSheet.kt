package com.meninocoiso.beatstarcommunity.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.meninocoiso.beatstarcommunity.R
import com.meninocoiso.beatstarcommunity.data.enums.Difficulty
import com.meninocoiso.beatstarcommunity.data.genresList
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceFilterBottomSheet(
	sheetState: SheetState,
	onDismissRequest: () -> Unit,
	onClose: () -> Unit
) {
	ModalBottomSheet(
		sheetState = sheetState,
		onDismissRequest = onDismissRequest,
		windowInsets = BottomSheetDefaults.windowInsets.only(WindowInsetsSides.Bottom)
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
		CollapsableSection(title = "Awarded") {
			FilterChip(
				selected = false,
				onClick = { /*TODO*/ },
				leadingIcon = {
					Icon(
						modifier = Modifier.size(22.dp),
						painter = painterResource(id = R.drawable.rounded_award_star_24),
						contentDescription = "Editor's Choice"
					)
				},
				label = {
					Text(text = "Editor’s Choice")
				}
			)
			FilterChip(
				selected = false,
				onClick = { /*TODO*/ },
				leadingIcon = {
					Icon(
						modifier = Modifier.size(22.dp),
						painter = painterResource(id = R.drawable.rounded_local_fire_department_24),
						contentDescription = "Featured"
					)
				},
				label = {
					Text(text = "Featured")
				}
			)
			FilterChip(
				selected = false,
				onClick = { /*TODO*/ },
				leadingIcon = {
					Icon(
						modifier = Modifier.size(22.dp),
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
			Difficulty.entries.forEach {
				FilterChip(
					selected = false,
					onClick = { /*TODO*/ },
					label = {
						Text(text = it.name.lowercase()
							.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() })
					}
				)
			}
		}
		CollapsableSection(title = "Genre") {
			genresList.forEach {
				FilterChip(
					selected = false,
					onClick = { /*TODO*/ },
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
			FilterChip(
				selected = false,
				onClick = { /*TODO*/ },
				label = {
					Text(text = "Default")
				}
			)
			FilterChip(
				selected = false,
				onClick = { /*TODO*/ },
				label = {
					Text(text = "Deluxe")
				}
			)
		}
	}
}