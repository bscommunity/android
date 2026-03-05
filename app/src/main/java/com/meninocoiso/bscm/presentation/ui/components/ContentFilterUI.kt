package com.meninocoiso.bscm.presentation.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

data class ContentFilterOption(
    val id: Int,
    val title: String,
    val count: Int? = null,
    val disabled: Boolean = false,
)

@Composable
fun ContentFilterUI(
    modifier: Modifier = Modifier,
    currentSelected: Int = 0,
    options: List<ContentFilterOption>,
    onClick: (Int) -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(
                rememberScrollState()
            )
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        options.forEach { option ->
            Text(
                modifier = Modifier
                    .then(
                        if (currentSelected == option.id && !option.disabled) {
                            Modifier
                        } else {
                            Modifier.graphicsLayer {
                                alpha = 0.6f
                            }
                        }
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        if (!option.disabled) {
                            onClick(option.id)
                        }
                    }
                    .padding(horizontal = 12.dp, vertical = 20.dp),
                text = "${option.title} ${option.count ?: ""}",
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}