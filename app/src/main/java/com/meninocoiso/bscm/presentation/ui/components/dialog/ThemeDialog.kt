package com.meninocoiso.bscm.presentation.ui.components.dialog

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.domain.enums.ThemePreference
import com.meninocoiso.bscm.presentation.ui.components.RadioGroupUI

val ThemeStrings = mapOf<ThemePreference, String>(
	ThemePreference.SYSTEM to "System",
	ThemePreference.LIGHT to "Light",
	ThemePreference.DARK to "Dark"
)


@Preview
@Composable
fun ThemeDialogPreview() {
	ThemeDialog(
		option = ThemePreference.SYSTEM,
		onThemeSelected = {}
	)
}

@Composable
fun ThemeDialog(
	option: ThemePreference,
	onThemeSelected: (ThemePreference) -> Unit,
	onCancel: (ThemePreference) -> Unit = {}
) {
	val (isOpened, setIsOpened) = remember { mutableStateOf(false) }
	val lastSelected = remember { mutableStateOf(option) }
	
	Button(onClick = {
		setIsOpened(true)
	}) {
		ThemeStrings[option]?.let {
			Text(
				text = it
			)
		}
	}
	when {
		isOpened -> {
			AlertDialog(
				onDismissRequest = { setIsOpened(false) },
				title = {
					Text(text = "App theme")
				},
				icon = {
					Icon(
						painter = painterResource(id = R.drawable.baseline_palette_24),
						contentDescription = "Palette icon"
					)
				},
				text = {
					RadioGroupUI(
						initialSelected = ThemeStrings[option]!!,
						radioOptions = ThemePreference.entries.map {
							ThemeStrings[it]!!
						},
						onOptionSelected = { index, _ ->
							onThemeSelected(ThemePreference.entries[index])
							lastSelected.value = ThemePreference.entries[index]
						})
				},
				dismissButton = {
					TextButton(onClick = {
						setIsOpened(false)
						if (option != lastSelected.value) {
							onCancel(option)
						}
					}) {
						Text(text = "Cancel")
					}
				},
				confirmButton = {
					Button(onClick = {
						setIsOpened(false)
					}) {
						Text(text = "Confirm")
					}
				}
			)
		}
	}
}