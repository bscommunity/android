package com.meninocoiso.beatstarcommunity.presentation.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExposedDropdownMenuBoxUI(
	options: List<String>,
) {
	var isExpanded by remember { mutableStateOf(false) }
	var currentOption by remember { mutableStateOf(options[0]) }

	ExposedDropdownMenuBox(
		modifier = Modifier
			.clip(MaterialTheme.shapes.small),
		expanded = isExpanded,
		onExpandedChange = { isExpanded = it },
	) {
		OutlinedTextField(
			// The `menuAnchor` modifier must be passed to the text field to handle
			// expanding/collapsing the menu on click. A read-only text field has
			// the anchor type `PrimaryNotEditable`.
			modifier = Modifier
				.menuAnchor(MenuAnchorType.PrimaryNotEditable)
				.fillMaxWidth(),
			value = currentOption,
			onValueChange = {},
			readOnly = true,
			singleLine = true,
			//label = { Text("Label") },
			trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
		)
		ExposedDropdownMenu(
			expanded = isExpanded,
			onDismissRequest = { isExpanded = false },
		) {
			options.forEach { option ->
				DropdownMenuItem(
					text = { Text(option, style = MaterialTheme.typography.bodyLarge) },
					onClick = {
						currentOption = option
						isExpanded = false
					},
					contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
				)
			}
		}
	}
}