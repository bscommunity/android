package com.meninocoiso.beatstarcommunity.presentation.ui.modifiers

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration

@Composable
fun Modifier.nonClippingLayout(): Modifier {
    val screenHeight = LocalConfiguration.current.screenHeightDp
    
    return layout { measurable, constraints ->
        // Measure the composable
        val placeable = measurable.measure(
            constraints.copy(
                maxWidth = constraints.maxWidth,
                maxHeight = screenHeight * 3,
            )
        )

        // Define the width and height of the layout
        val width = constraints.maxWidth
        val height = constraints.maxHeight

        // Define the layout
        layout(width, height) {
            // Place the composable at (0, 0) coordinates
            placeable.placeRelative(0, 0)
        }
    }
}