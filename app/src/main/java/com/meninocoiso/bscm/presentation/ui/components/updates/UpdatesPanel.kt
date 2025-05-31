package com.meninocoiso.bscm.presentation.ui.components.updates

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun UpdatesPanel(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .padding(start = 16.dp, end = 16.dp)
            .background(
                MaterialTheme.colorScheme.surfaceContainerLow,
                RoundedCornerShape(16.dp)
            )
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}