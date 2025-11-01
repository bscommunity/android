package com.meninocoiso.bscm.presentation.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.meninocoiso.bscm.R

@Composable
fun SwitchUI(
	checked: Boolean = false,
	onCheckedChange: (Boolean) -> Unit,
	enabled: Boolean = true,
) {
	Switch(
		checked = checked,
		onCheckedChange = {
			onCheckedChange(it)
		},
		enabled = enabled,
		thumbContent = if (checked) {
			{
				Icon(
					imageVector = Icons.Filled.Check,
					contentDescription = stringResource(R.string.checked),
					modifier = Modifier.size(SwitchDefaults.IconSize),
				)
			}
		} else {
			{
				Icon(
					imageVector = Icons.Filled.Close,
					contentDescription = stringResource(R.string.unchecked),
					modifier = Modifier.size(SwitchDefaults.IconSize),
				)
			}
		}
	)
}