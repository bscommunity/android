package com.meninocoiso.beatstarcommunity.presentation.ui.modifiers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll

@Composable
fun rememberFabNestedScrollConnection(
    onFabStateChange: (Boolean) -> Unit
): NestedScrollConnection {
    val threshold = 100f
    var accumulatedScroll by remember { mutableFloatStateOf(0f) }

    return remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                accumulatedScroll += available.y
                when {
                    accumulatedScroll > threshold -> {
                        onFabStateChange(true) // extend FAB
                        accumulatedScroll = 0f
                    }
                    accumulatedScroll < -threshold -> {
                        onFabStateChange(false) // shrink FAB
                        accumulatedScroll = 0f
                    }
                }
                return Offset.Zero
            }
        }
    }
}

@Composable
fun Modifier.fabScrollObserver(onFabStateChange: (Boolean) -> Unit): Modifier {
    return this.nestedScroll(rememberFabNestedScrollConnection(
        onFabStateChange = onFabStateChange
    ))
}
