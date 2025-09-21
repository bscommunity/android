package com.meninocoiso.bscm.presentation.ui.components.details

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.meninocoiso.bscm.R

@Composable
fun LikeButton(
    chartId: String,
    defaultValue: Boolean = false,
) {
    val haptic = LocalHapticFeedback.current

    var isLiked by remember { mutableStateOf(defaultValue) }
    var animatePop by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (animatePop) 1.3f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        finishedListener = {
            animatePop = false // reset after bounce
        },
        label = "heartScale"
    )

    IconButton(onClick = {
        val newValue = !isLiked
        if (newValue) {
            animatePop = true // trigger bounce only on like
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        isLiked = newValue
    }) {
        Icon(
            painter = painterResource(
                if (isLiked) R.drawable.baseline_favorite_24
                else R.drawable.rounded_favorite_24
            ),
            contentDescription = null,
            tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale
                )
        )
    }
}