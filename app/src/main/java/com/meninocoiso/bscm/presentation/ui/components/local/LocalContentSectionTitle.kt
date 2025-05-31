package com.meninocoiso.bscm.presentation.ui.components.local

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun LocalContentSectionTitle(title: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp, 0.dp, 0.dp, 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge
        )
    }
}