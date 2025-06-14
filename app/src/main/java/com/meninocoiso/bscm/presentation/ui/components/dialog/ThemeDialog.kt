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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.domain.enums.ThemePreference
import com.meninocoiso.bscm.presentation.ui.components.RadioGroupUI

@Composable
private fun getThemeStrings(): Map<ThemePreference, String> {
	return mapOf(
		ThemePreference.SYSTEM to stringResource(R.string.system),
		ThemePreference.LIGHT to stringResource(R.string.light),
		ThemePreference.DARK to stringResource(R.string.dark)
	)
}

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
	
	val themeStrings = getThemeStrings()
	
	Button(onClick = {
		setIsOpened(true)
	}) {
		themeStrings[option]?.let {
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
					Text(text = stringResource(R.string.app_theme))
				},
				icon = {
					Icon(
						painter = painterResource(id = R.drawable.baseline_palette_24),
						contentDescription = null
					)
				},
				text = {
					RadioGroupUI(
						initialSelected = themeStrings[option]!!,
						radioOptions = ThemePreference.entries.map {
							themeStrings[it]!!
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
						Text(text = stringResource(R.string.cancel))
					}
				},
				confirmButton = {
					Button(onClick = {
						setIsOpened(false)
					}) {
						Text(text = stringResource(R.string.confirm))
					}
				}
			)
		}
	}
}