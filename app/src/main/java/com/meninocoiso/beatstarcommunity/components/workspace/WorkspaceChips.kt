package com.meninocoiso.beatstarcommunity.components.workspace

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class WorkspaceChip(
	val id: Int,
	val title: String,
)

val chipItems = listOf(
	WorkspaceChip(
		id = 1,
		title = "Weekly Rank"
	),
	WorkspaceChip(
		id = 2,
		title = "Last updated",
	),
	WorkspaceChip(
		id = 3,
		title = "Most downloaded",
	),
	WorkspaceChip(
		id = 3,
		title = "Most liked",
	),
)

@Composable
fun WorkspaceChips() {
	var selectedChipIndex by remember { mutableIntStateOf(0) }

	Row(
		modifier = Modifier
			.fillMaxWidth()
			.padding(top = 8.dp)
			// .background(Color.Red)
			.horizontalScroll(rememberScrollState()),
		horizontalArrangement = Arrangement.spacedBy(8.dp),
	) {
		Spacer(modifier = Modifier.width(8.dp)) // Leading padding

		chipItems.forEachIndexed { index, chip ->
			FilterChip(
				onClick = { selectedChipIndex = index },
				label = {
					Text(chip.title)
				},
				selected = selectedChipIndex == index,
				leadingIcon = if (selectedChipIndex == index) {
					{
						Icon(
							imageVector = Icons.Filled.Check,
							contentDescription = "Check icon",
							modifier = Modifier.size(FilterChipDefaults.IconSize)
						)
					}
				} else {
					null
				},
			)
		}

		Spacer(modifier = Modifier.width(8.dp)) // Leading padding
	}
}