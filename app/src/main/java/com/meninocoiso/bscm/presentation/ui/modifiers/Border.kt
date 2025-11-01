package com.meninocoiso.bscm.presentation.ui.modifiers

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp

@Composable
fun Modifier.verticalBorder(): Modifier {
    val outlineColor = MaterialTheme.colorScheme.outline
    
    return drawBehind {
        val strokeWidth = 2.dp.toPx()
        // Top border
        drawLine(
            color = outlineColor,
            start = Offset(0f, 0f),
            end = Offset(size.width, 0f),
            strokeWidth = strokeWidth
        )
        // Bottom border
        drawLine(
            color = outlineColor,
            start = Offset(0f, size.height),
            end = Offset(size.width, size.height),
            strokeWidth = strokeWidth
        )
    }
}