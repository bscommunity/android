package com.meninocoiso.bscm.presentation.ui.modifiers

// Modifier personalizado
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

fun Modifier.animatedBurstButton(
    isActive: Boolean,
    burstColor: Color,
    numDots: Int = 8,
    dotSize: Dp = 2.dp,
    burstRadius: Dp = 20.dp,
    burstDuration: Int = 350,
    scaleUp: Float = 1.3f,
    scaleDuration: Int = 180,
    onAnimationEnd: () -> Unit = {}
): Modifier = composed {
    val scope = rememberCoroutineScope()
    val heartScale = remember { Animatable(1f) }
    val burstProgress = remember { Animatable(0f) }
    val burstAlpha = remember { Animatable(0f) }

    LaunchedEffect(isActive) {
        if (isActive) {
            burstProgress.snapTo(0f)
            burstAlpha.snapTo(1f)
            val burstJob = scope.launch { burstProgress.animateTo(1f, tween(burstDuration)) }
            val alphaJob = scope.launch {
                kotlinx.coroutines.delay((burstDuration / 2).toLong())
                burstAlpha.animateTo(0f, tween(burstDuration * 2 / 3))
            }
            val heartJob = scope.launch {
                heartScale.animateTo(
                    scaleUp,
                    spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
                heartScale.animateTo(1f, tween(scaleDuration))
            }
            burstJob.join()
            alphaJob.join()
            heartJob.join()
            onAnimationEnd()
        }
    }

    this
        .graphicsLayer(
            scaleX = heartScale.value,
            scaleY = heartScale.value
        )
        .drawBehind {
            if (burstAlpha.value > 0f) {
                val center = size / 2f
                val radiusPx = burstRadius.toPx() * burstProgress.value
                for (i in 0 until numDots) {
                    val angle = (2 * Math.PI * i) / numDots
                    val x = center.width + radiusPx * kotlin.math.cos(angle).toFloat()
                    val y = center.height + radiusPx * kotlin.math.sin(angle).toFloat()
                    drawCircle(
                        color = burstColor.copy(alpha = burstAlpha.value),
                        radius = dotSize.toPx() * burstAlpha.value,
                        center = Offset(x, y)
                    )
                }
            }
        }
}