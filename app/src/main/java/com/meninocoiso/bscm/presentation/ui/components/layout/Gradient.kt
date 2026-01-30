package com.meninocoiso.bscm.presentation.ui.components.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun LinearGradient(
    modifier: Modifier = Modifier,
    colors: List<Color>,
    size: Dp? = 76.dp,
    borderRadius: Dp = 0.dp
) {
    val brush = Brush.horizontalGradient(colors)

    Box(
        modifier = modifier
            .then(if (size != null) Modifier.requiredSize(size) else Modifier)
            .clip(shape = androidx.compose.foundation.shape.RoundedCornerShape(borderRadius))
            .background(brush),
    )
}
@Composable
fun RadialGradient(
    modifier: Modifier = Modifier,
    colors: List<Color>,
    size: Dp? = 76.dp,
    borderRadius: Dp = 0.dp
) {
    val brush = Brush.radialGradient(colors)

    Box(
        modifier = modifier
            .then(if (size != null) Modifier.requiredSize(size) else Modifier)
            .clip(shape = androidx.compose.foundation.shape.RoundedCornerShape(borderRadius))
            .background(brush),
    )
}