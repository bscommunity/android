package com.meninocoiso.bscm.presentation.ui.components.details

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.meninocoiso.bscm.presentation.ui.components.AnimatedIcon
import com.meninocoiso.bscm.presentation.ui.components.BurstDotsConfig
import com.meninocoiso.bscm.presentation.ui.components.BurstIconButton
import com.meninocoiso.bscm.presentation.ui.components.RingConfig
import com.meninocoiso.bscm.presentation.ui.components.rememberBurstDotsModule
import com.meninocoiso.bscm.presentation.ui.components.rememberIconScaleModule
import com.meninocoiso.bscm.presentation.ui.components.rememberRingModule
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun InteractionButton(
    activeIconResId: Int,
    inactiveIconResId: Int,
    isActive: Boolean = false,
    debounceMillis: Long = 600L,
    onToggle: (isActive: Boolean) -> Unit = {},
) {
    var localIsActive by remember { mutableStateOf(isActive) }

    LaunchedEffect(isActive) {
        localIsActive = isActive
    }

    val scope = rememberCoroutineScope()
    var debounceJob by remember { mutableStateOf<Job?>(null) }

    val (burstAnimation, burstVisual) = rememberBurstDotsModule(
        config = BurstDotsConfig(color = MaterialTheme.colorScheme.primary)
    )
    val (ringAnimation, ringVisual) = rememberRingModule(
        config = RingConfig(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
    )
    val (iconScale, iconScaleAnimation) = rememberIconScaleModule()

    BurstIconButton(
        enabled = true,
        isActive = localIsActive,
        onClick = {
            localIsActive = !localIsActive
            debounceJob?.cancel()
            debounceJob = scope.launch {
                delay(debounceMillis)
                onToggle(localIsActive)
            }
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
