package com.meninocoiso.bscm.presentation.ui.components.details

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.meninocoiso.bscm.presentation.ui.components.AnimatedIcon
import com.meninocoiso.bscm.presentation.ui.components.BurstDotsConfig
import com.meninocoiso.bscm.presentation.ui.components.BurstIconButton
import com.meninocoiso.bscm.presentation.ui.components.RingConfig
import com.meninocoiso.bscm.presentation.ui.components.rememberBurstDotsModule
import com.meninocoiso.bscm.presentation.ui.components.rememberIconScaleModule
import com.meninocoiso.bscm.presentation.ui.components.rememberRingModule

/**
 * Animated toggle button used for like/bookmark actions.
 *
 * It keeps a local visual state for immediate feedback while still syncing with external state.
 */
@Composable
fun InteractionButton(
    activeIconResId: Int,
    inactiveIconResId: Int,
    isActive: Boolean = false,
    isDisabled: Boolean = false,
    onDisabled: () -> Unit = {},
    beforeToggle: (current: Boolean, next: Boolean) -> Boolean = { _, _ -> true },
    onToggle: (isActive: Boolean) -> Unit
) {
    var localIsActive by remember { mutableStateOf(isActive) }

    LaunchedEffect(isActive) {
        localIsActive = isActive
    }

    val (burstAnimation, burstVisual) = rememberBurstDotsModule(
        config = BurstDotsConfig(color = MaterialTheme.colorScheme.primary)
    )
    val (ringAnimation, ringVisual) = rememberRingModule(
        config = RingConfig(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
    )
    val (iconScale, iconScaleAnimation) = rememberIconScaleModule()

    BurstIconButton(
        modifier = Modifier.graphicsLayer {
            alpha = if (isDisabled) 0.6f else 1f
        },
        enabled = !isDisabled,
        onClick = {
            if (isDisabled) {
                onDisabled()
                return@BurstIconButton false
            }

            // Let callers intercept transitions (e.g. open manage sheet before unbookmark).
            val nextState = !localIsActive
            if (!beforeToggle(localIsActive, nextState)) {
                return@BurstIconButton false
            }
            localIsActive = nextState
            onToggle(nextState)
            true
        },
        animations = listOfNotNull(
            burstAnimation,
            ringAnimation,
            iconScaleAnimation
        ),
        visuals = listOfNotNull(
            burstVisual,
            ringVisual
        ),
    ) {
        AnimatedIcon(
            isActive = localIsActive,
            activeIconResId = activeIconResId,
            inactiveIconResId = inactiveIconResId,
            activeColor = MaterialTheme.colorScheme.primary,
            inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant,
            iconScale = iconScale
        )
    }
}
