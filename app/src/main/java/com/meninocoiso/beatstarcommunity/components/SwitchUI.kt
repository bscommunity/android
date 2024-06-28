package com.meninocoiso.beatstarcommunity.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@Composable
fun SwitchUI(
	defaultChecked: Boolean = false,
	onCheckedChange: (Boolean) -> Unit
) {
	var checked by remember { mutableStateOf(defaultChecked) }

	Switch(
		checked = checked,
		onCheckedChange = {
			checked = it
			onCheckedChange(it)
		},
		thumbContent = if (checked) {
			{
				Icon(
					imageVector = Icons.Filled.Check,
					contentDescription = null,
					modifier = Modifier.size(SwitchDefaults.IconSize),
				)
			}
		} else {
			{
				Icon(
					imageVector = Icons.Filled.Close,
					contentDescription = null,
					modifier = Modifier.size(SwitchDefaults.IconSize),
				)
			}
		}
	)
}