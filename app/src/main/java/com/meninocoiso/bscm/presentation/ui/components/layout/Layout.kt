package com.meninocoiso.bscm.presentation.ui.components.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun Section(
	title: String?,
	modifier: Modifier = Modifier,
	titleModifier: Modifier? = null,
	thickness: Dp = DividerDefaults.Thickness,
	verticalArrangement: Arrangement.Vertical = Arrangement.Top,
	horizontalAlignment: Alignment.Horizontal = Alignment.Start,
	content: @Composable () -> Unit
) {
	Column(
		verticalArrangement = verticalArrangement,
		horizontalAlignment = horizontalAlignment,
		modifier = modifier.fillMaxWidth()
	) {
		if (thickness != 0.dp) {
			HorizontalDivider(thickness = thickness)
		}
		if (title != null) {
			Text(
				text = title,
				style = MaterialTheme.typography.labelLarge,
				modifier = titleModifier ?: Modifier.padding(top = 24.dp, bottom = 16.dp, start = 16.dp, end = 16.dp)
			)
		}
		content()
	}
}