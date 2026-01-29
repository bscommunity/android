package com.meninocoiso.bscm.presentation.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

data class ContentFilterOption(
    val id: Int,
    val title: String,
    val count: Int
)

@Composable
fun ContentFilterUI(
    options: List<ContentFilterOption>,
    onClick: (Int) -> Unit
) {
    var currentSelected by remember { mutableIntStateOf(0) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(
                rememberScrollState())
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        options.forEach { option ->
            Text(
                modifier = Modifier
                    .then(
                        if (currentSelected == option.id) {
                            Modifier
                        } else {
                            Modifier.graphicsLayer {
                                alpha = 0.6f
                            }
                        }
                    )
                    .clickable {
                        currentSelected = option.id
                        onClick(option.id)
                    }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                text = "${option.title} ${option.count}",
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}