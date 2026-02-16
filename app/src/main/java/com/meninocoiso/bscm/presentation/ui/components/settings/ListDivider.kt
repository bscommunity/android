package com.meninocoiso.bscm.presentation.ui.components.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ListDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = Modifier
            .height(1.dp)
            .fillMaxWidth()
    )
}