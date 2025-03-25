package com.meninocoiso.beatstarcommunity.presentation.ui.components.updates

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
import com.meninocoiso.beatstarcommunity.R
import com.meninocoiso.beatstarcommunity.data.manager.ChartsState
import com.meninocoiso.beatstarcommunity.presentation.ui.modifiers.infiniteRotation

@Composable
internal fun UpdatesButton(
    state: ChartsState,
    onFetchUpdates: (chartToRemove: String?) -> Unit
) {
    FilledTonalButton(
        onClick = { onFetchUpdates(null) },
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        enabled = state !is ChartsState.Loading,
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
                        if (state == ChartsState.Loading()) {
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
            Text(text = if (state == ChartsState.Loading()) "Checking for updates..." else "Check for updates")
        }
    }
}