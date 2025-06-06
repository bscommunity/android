package com.meninocoiso.bscm.presentation.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

@Composable
fun RadioGroupUI(
	radioOptions: List<String>,
	trailingElements: List<@Composable () -> Unit>? = null,
	initialSelected: String = radioOptions[0],
	onOptionSelected: (Int, String) -> Unit
) {
	val (selectedOption, onSelected) = remember { mutableStateOf(initialSelected) }

	// Modifier.selectableGroup() is essential to ensure correct accessibility behavior
	Column(Modifier.selectableGroup()) {
		radioOptions.forEachIndexed { index, text ->
			Row(
				Modifier
					.fillMaxWidth()
					.height(56.dp)
					.selectable(
						selected = (text == selectedOption),
						onClick = {
							onSelected(text)
							onOptionSelected(index, text)
						},
						role = Role.RadioButton
					)
					.padding(horizontal = 16.dp),
				verticalAlignment = Alignment.CenterVertically,
				horizontalArrangement = Arrangement.SpaceBetween
			) {
				Row {
					RadioButton(
						selected = (text == selectedOption),
						onClick = null // null recommended for accessibility with screen-readers
					)
					Text(
						text = text,
						style = MaterialTheme.typography.bodyLarge,
						modifier = Modifier.padding(start = 16.dp)
					)
				}
				if (trailingElements != null) {
					trailingElements[index].invoke()
				}
			}
		}
	}
}