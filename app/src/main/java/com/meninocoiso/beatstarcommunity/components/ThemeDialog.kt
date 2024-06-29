package com.meninocoiso.beatstarcommunity.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.painterResource
import com.meninocoiso.beatstarcommunity.R

fun onConfirmation() {
	println("Concluded")
}

@Composable
fun ThemeDialog() {
	val (isOpened, setIsOpened) = remember { mutableStateOf(false) }

	Button(onClick = {
		setIsOpened(true)
	}) {
		Text(text = "System")
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
						radioOptions = listOf("System", "Light", "Dark"),
						onOptionSelected = {
							println(it)
						})
				},
				dismissButton = {
					TextButton(onClick = {
						onConfirmation()
						setIsOpened(false)
					}) {
						Text(text = "Cancel")
					}
				},
				confirmButton = {
					Button(onClick = {
						onConfirmation()
						setIsOpened(false)
					}) {
						Text(text = "Confirm")
					}
				}
			)
		}
	}
}