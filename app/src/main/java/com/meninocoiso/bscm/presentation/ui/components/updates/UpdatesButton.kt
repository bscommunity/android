package com.meninocoiso.bscm.presentation.ui.components.updates

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.presentation.ui.modifiers.infiniteRotation

@Composable
internal fun UpdatesButton(isLoading: Boolean, isDisabled: Boolean, onFetchUpdates: () -> Unit) {
    FilledTonalButton(
        onClick = { onFetchUpdates() },
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        enabled = !isLoading && !isDisabled,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(ButtonDefaults.IconSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier
                    .run {
                        if (isLoading) {
                            this.infiniteRotation(easing = CubicBezierEasing(
                                0.4f, 0.0f, 0.2f, 1.0f
                            )
                            )
                        } else {
                            this
                        }
                    }
                    .size(ButtonDefaults.IconSize),
                painter = painterResource(id = R.drawable.rounded_autorenew_24),
                contentDescription = "Check for updates icon"
            )
            Text(text = if (isLoading) "Checking for updates..." else "Check for updates")
        }
    }
}