package com.meninocoiso.bscm.presentation.ui.components.details

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.presentation.ui.components.AnimatedIcon
import com.meninocoiso.bscm.presentation.ui.components.BurstDotsConfig
import com.meninocoiso.bscm.presentation.ui.components.BurstIconButton
import com.meninocoiso.bscm.presentation.ui.components.RingConfig
import com.meninocoiso.bscm.presentation.ui.components.rememberBurstDotsModule
import com.meninocoiso.bscm.presentation.ui.components.rememberIconScaleModule
import com.meninocoiso.bscm.presentation.ui.components.rememberRingModule
import com.meninocoiso.bscm.presentation.viewmodel.InteractionViewModel

@Composable
fun OfflineLikeButton(
    contentId: String,
    defaultValue: Boolean = false,
    viewModel: InteractionViewModel = hiltViewModel()
) {
    var isLiked by remember { mutableStateOf(defaultValue) }

    val (burstAnimation, burstVisual) = rememberBurstDotsModule(
        config = BurstDotsConfig(color = MaterialTheme.colorScheme.primary)
    )
    val (ringAnimation, ringVisual) = rememberRingModule(
        config = RingConfig(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
    )
    val (iconScale, iconScaleAnimation) = rememberIconScaleModule()

    BurstIconButton(
        enabled = false,
        isActive = isLiked,
        onClick = { isLiked = !isLiked },
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
            isActive = isLiked,
            activeIconResId = R.drawable.baseline_favorite_24,
            inactiveIconResId = R.drawable.rounded_favorite_24,
            activeColor = MaterialTheme.colorScheme.primary,
            inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant,
            iconScale = iconScale
        )
    }
}
