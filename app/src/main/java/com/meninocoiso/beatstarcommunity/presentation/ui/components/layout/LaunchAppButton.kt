package com.meninocoiso.beatstarcommunity.presentation.ui.components.layout

import android.content.Context
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.meninocoiso.beatstarcommunity.R

@Composable
fun LaunchAppButton(
	onNavigateToUpdates: () -> Unit
) {
	val context = LocalContext.current
	val openAlertDialog = remember { mutableStateOf(false) }

	ExtendedFloatingActionButton(
		text = { Text("Launch app") },
		icon = {
			Icon(
				painter = painterResource(id = R.drawable.outline_play_circle_24),
				contentDescription = ""
			)
		},
		onClick = {
			launchBeatClone(context, (openAlertDialog::value)::set)
		},
	)
	when {
		// ...
		openAlertDialog.value -> {
			RequestAppDownloadDialog(
				onDismissRequest = { openAlertDialog.value = false },
				onConfirmation = {
					openAlertDialog.value = false
					onNavigateToUpdates()
				}
			)
		}
	}
}

fun launchBeatClone(
	context: Context,
	openAlertDialog: (Boolean) -> Unit,
) {
	val packageName = "com.spaceapegames.beatclon"

	// Create an Intent to launch the app
	val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)

	println("launchIntent: $launchIntent")

	if (launchIntent != null) {
		// If the intent is not null, start the activity
		context.startActivity(launchIntent)
	} else {
		// If the app is not installed,
		openAlertDialog(true)
	}
}

@Composable
fun RequestAppDownloadDialog(
	onDismissRequest: () -> Unit,
	onConfirmation: () -> Unit
) {
	AlertDialog(
		icon = {
			Icon(
				painter = painterResource(
					R.drawable.baseline_device_unknown_24
				),
				modifier = Modifier.size(24.dp),
				contentDescription = "App not found icon"
			)
		},
		title = {
			Text(text = "Modded app not installed")
		},
		text = {
			Text(text = "We couldn't find the modded app on your device. Visit the 'Installations' section on the Updates page to download the latest version.")
		},
		onDismissRequest = onDismissRequest,
		confirmButton = {
			TextButton(onClick = onConfirmation) {
				Text("Go to Updates")
			}
		},
		dismissButton = {
			TextButton(onClick = onDismissRequest) {
				Text("Dismiss")
			}
		}
	)
}