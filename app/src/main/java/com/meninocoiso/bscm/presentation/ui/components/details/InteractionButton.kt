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

@Composable
fun InteractionButton(
    activeIconResId: Int,
    inactiveIconResId: Int,
    isActive: Boolean = false,
    isDisabled: Boolean = false,
    onDisabled: () -> Unit = {},
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
        isActive = localIsActive,
        enabled = !isDisabled,
        onClick = {
            if (isDisabled) {
                onDisabled()
                return@BurstIconButton
            }

            val nextState = !localIsActive
            localIsActive = nextState
            onToggle(nextState)
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
