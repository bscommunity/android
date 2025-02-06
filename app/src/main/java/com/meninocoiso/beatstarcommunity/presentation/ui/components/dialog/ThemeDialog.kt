package com.meninocoiso.beatstarcommunity.presentation.ui.components.dialog

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
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.meninocoiso.beatstarcommunity.R
import com.meninocoiso.beatstarcommunity.domain.enums.ThemePreference
import com.meninocoiso.beatstarcommunity.presentation.ui.components.RadioGroupUI
import java.util.Locale

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
	onThemeSelected: (ThemePreference) -> Unit
) {
	val (isOpened, setIsOpened) = remember { mutableStateOf(false) }

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
						onOptionSelected = {
							onThemeSelected(ThemePreference.valueOf(it.uppercase(Locale.ROOT)))
						})
				},
				dismissButton = {
					TextButton(onClick = {
						setIsOpened(false)
					}) {
						Text(text = "Cancel")
					}
				},
				confirmButton = {
					Button(onClick = {
						onThemeSelected(option)
						setIsOpened(false)
					}) {
						Text(text = "Confirm")
					}
				}
			)
		}
	}
}