package com.meninocoiso.beatstarcommunity.components

import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import com.meninocoiso.beatstarcommunity.R

@Composable
fun LaunchAppButton() {
	ExtendedFloatingActionButton(
		text = { Text("Launch app") },
		icon = {
			Icon(
				painter = painterResource(id = R.drawable.outline_play_circle_24),
				contentDescription = ""
			)
		},
		onClick = {
			/* TODO */
		},
	)
}