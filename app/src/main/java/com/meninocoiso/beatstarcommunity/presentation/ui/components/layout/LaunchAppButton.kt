package com.meninocoiso.beatstarcommunity.presentation.ui.components.layout

import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.meninocoiso.beatstarcommunity.R
import com.meninocoiso.beatstarcommunity.presentation.ui.components.dialog.RequestAppDownloadDialog
import com.meninocoiso.beatstarcommunity.util.LinkingUtils.launchBeatClone

@Composable
fun LaunchAppButton(
	onNavigateToUpdates: () -> Unit,
	extended: Boolean,
	modifier: Modifier = Modifier
) {
	val context = LocalContext.current
	val openAlertDialog = remember { mutableStateOf(false) }

	ExtendedFloatingActionButton(
		modifier = modifier,
		text = { Text("Launch app") },
		icon = {
			Icon(
				painter = painterResource(id = R.drawable.outline_play_circle_24),
				contentDescription = "Launch app"
			)
		},
		onClick = {
			launchBeatClone(context, (openAlertDialog::value)::set)
		},
		expanded = extended // This drives the built-in animation
	)

	when {
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