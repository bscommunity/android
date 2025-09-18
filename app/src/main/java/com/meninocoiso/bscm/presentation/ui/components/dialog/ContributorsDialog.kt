package com.meninocoiso.bscm.presentation.ui.components.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.domain.model.internal.ContributionCategory

private const val TAG = "ContributorsDialog"

private val CategoryIcons = mapOf(
    "programming" to R.drawable.rounded_code_24,
    "design" to R.drawable.rounded_palette_24,
    "localization" to R.drawable.round_translate_24,
)

@Composable
fun ContributorsDialog(
    isLoading: Boolean,
    items: List<ContributionCategory>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        modifier = Modifier.heightIn(max = 600.dp),
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Contributors")
        },
        text = {
            if (isLoading && items.isEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Check out the amazing people we have contributing to bscm",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    items.forEach { category ->
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val iconRes = CategoryIcons[category.name.lowercase()]
                                if (iconRes != null) {
                                    Icon(
                                        modifier = Modifier.size(18.dp),
                                        painter = painterResource(id = iconRes),
                                        contentDescription = null
                                    )
                                }
                                Text(
                                    text = category.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Column(
                                verticalArrangement = Arrangement.spacedBy(16.dp), 
                            ) {
                                category.contributors.forEach { c ->
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = c.name,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Text(
                                            text = c.role,
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onDismiss() },
            ) {
                Text(text = "Close")
            }
        }
    )
}