package com.meninocoiso.bscm.presentation.ui.components.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun SettingsCard(
    title: String,
    supportingText: String? = null,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline,
                androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
            )
    ) {
        ListItem(
            modifier = Modifier
                .border(1.dp, MaterialTheme.colorScheme.surfaceContainerHighest),
            colors = ListItemDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
            headlineContent = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 16.dp)
                )
            },
            trailingContent = {
                if (supportingText != null) {
                    Text(text = supportingText, style = MaterialTheme.typography.bodyMedium)
                }
            }
        )
        content()
    }
}

@Composable
fun Modifier.settingsCard(padding: PaddingValues = PaddingValues(8.dp)): Modifier {
    return this.padding(padding)
}

@Composable
fun HeadlineText(title: String) {
    Text(
        text = title,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium
    )
}

@Composable
fun SupportingText(title: String) {
    Text(text = title, style = MaterialTheme.typography.bodyMedium)
}
