package com.meninocoiso.bscm.presentation.ui.components.details

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.meninocoiso.bscm.R
import kotlinx.coroutines.launch

private enum class BoxState { Collapsed, Expanded }

@Composable
fun LikeButton(
    defaultValue: Boolean = false,
    onLikeChanged: (Boolean) -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    var isLiked by remember { mutableStateOf(defaultValue) }

    // Heart scale animation
    val heartScale = remember { Animatable(1f) }

    // Dots burst animation
    val burstProgress = remember { Animatable(0f) }
    val burstAlpha = remember { Animatable(0f) }
    val numDots = 8
    val dotColor = MaterialTheme.colorScheme.primary
    val dotSize = 2.dp
    val burstRadius = 20.dp

    // Circular ring animation
    val ringRadius = remember { Animatable(0f) }
    val ringAlpha = remember { Animatable(0f) }
    val ringMaxRadius = 24.dp
    val ringColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)

    Box(contentAlignment = Alignment.Center) {
        // Circular ring effect (behind burst dots)
        if (ringAlpha.value > 0f && ringRadius.value > 0f) {
            Canvas(modifier = Modifier.size(48.dp)) {
                val center = size / 2f
                drawCircle(
                    color = ringColor.copy(alpha = ringAlpha.value),
                    radius = ringMaxRadius.toPx() * ringRadius.value,
                    center = androidx.compose.ui.geometry.Offset(center.width, center.height),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.dp.toPx())
                )
            }
        }

        // Dots burst effect
        if (burstAlpha.value > 0f) {
            Canvas(modifier = Modifier.size(48.dp)) {
                val center = size / 2f
                val radiusPx = burstRadius.toPx() * burstProgress.value
                for (i in 0 until numDots) {
                    val angle = (2 * Math.PI * i) / numDots
                    val x = center.width + radiusPx * kotlin.math.cos(angle).toFloat()
                    val y = center.height + radiusPx * kotlin.math.sin(angle).toFloat()
                    drawCircle(
                        color = dotColor.copy(alpha = burstAlpha.value),
                        // Dot radius is in sync with alpha: 0 when transparent, full size when fully visible
                        radius = dotSize.toPx() * burstAlpha.value,
                        center = androidx.compose.ui.geometry.Offset(x, y)
                    )
                }
            }
        }

        // ❤️ Heart button
        IconButton(
            onClick = {
                // Toggle like state
                val newLiked = !isLiked
                isLiked = newLiked
                onLikeChanged(isLiked)

                scope.launch {
                    if (newLiked) {
                        // Haptic feedback
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                        // Animate heart, burst, and ring together only when liking
                        burstProgress.snapTo(0f)
                        burstAlpha.snapTo(1f)
                        ringRadius.snapTo(0f)
                        ringAlpha.snapTo(0.5f)
                        
                        val burstJob = launch {
                            burstProgress.animateTo(1f, tween(350))
                        }
                        
                        val alphaJob = launch {
                            kotlinx.coroutines.delay(200) // Start fading out a little after
                            burstAlpha.animateTo(0f, tween(220))
                        }
                        
                        val ringJob = launch {
                            ringRadius.animateTo(1f, tween(400))
                            ringAlpha.animateTo(0f, tween(200))
                        }
                        
                        val heartJob = launch {
                            heartScale.animateTo(
                                1.3f,
                                spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            )
                            heartScale.animateTo(1f, tween(180))
                        }
                        burstJob.join()
                        alphaJob.join()
                        ringJob.join()
                        heartJob.join()
                    }
                    // No animation when unliking
                }
            }
        ) {
            Icon(
                painter = painterResource(
                    if (isLiked) R.drawable.baseline_favorite_24
                    else R.drawable.rounded_favorite_24
                ),
                contentDescription = null,
                tint = if (isLiked) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer(
                        scaleX = heartScale.value,
                        scaleY = heartScale.value
                    )
            )
        }
    }
}