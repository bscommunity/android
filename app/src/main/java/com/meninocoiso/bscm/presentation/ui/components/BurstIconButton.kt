package com.meninocoiso.bscm.presentation.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Animation module type for modularity
 */
typealias IconButtonAnimation = suspend (CoroutineScope) -> Unit

/**
 * Visual module type for modularity
 */
typealias IconButtonVisual = @Composable () -> Unit

/**
 * Configuration for burst dots animation
 */
data class BurstAnimationConfig(
    val progressAnimation: AnimationSpec<Float> = tween(350),
    val alphaAnimation: AnimationSpec<Float> = tween(220),
    val alphaDelay: Long = 200
)

/**
 * Configuration for ring animation
 */
data class RingAnimationConfig(
    val radiusAnimation: AnimationSpec<Float> = tween(400),
    val alphaAnimation: AnimationSpec<Float> = tween(200),
    val initialAlpha: Float = 0.5f
)

/**
 * Configuration for icon scale animation
 */
data class IconScaleAnimationConfig(
    val scaleValue: Float = 1.3f,
    val scaleUpAnimation: AnimationSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    ),
    val scaleDownAnimation: AnimationSpec<Float> = tween(180)
)

/**
 * Configuration for burst dots visual
 */
data class BurstDotsConfig(
    val color: Color,
    val numDots: Int = 8,
    val dotSize: Dp = 2.dp,
    val burstRadius: Dp = 20.dp,
    val effectSize: Dp = 48.dp
)

/**
 * Configuration for ring visual
 */
data class RingConfig(
    val color: Color,
    val maxStrokeWidth: Dp = 16.dp,
    val maxRadius: Dp = 24.dp,
    val effectSize: Dp = 48.dp
)

/**
 * Burst dots animation module
 */
fun createBurstAnimation(
    burstProgress: Animatable<Float, *>,
    burstAlpha: Animatable<Float, *>,
    config: BurstAnimationConfig = BurstAnimationConfig()
): IconButtonAnimation = { scope ->
    scope.launch {
        burstProgress.snapTo(0f)
        burstAlpha.snapTo(1f)
        val burstJob = scope.launch { burstProgress.animateTo(1f, config.progressAnimation) }
        val alphaJob = scope.launch {
            kotlinx.coroutines.delay(config.alphaDelay)
            burstAlpha.animateTo(0f, config.alphaAnimation)
        }
        burstJob.join(); alphaJob.join()
    }
}

/**
 * Ring animation module
 */
fun createRingAnimation(
    ringRadius: Animatable<Float, *>,
    ringAlpha: Animatable<Float, *>,
    config: RingAnimationConfig = RingAnimationConfig()
): IconButtonAnimation = { scope ->
    scope.launch {
        ringRadius.snapTo(0f)
        ringAlpha.snapTo(config.initialAlpha)
        val ringJob = scope.launch {
            ringRadius.animateTo(1f, config.radiusAnimation)
            ringAlpha.animateTo(0f, config.alphaAnimation)
        }
        ringJob.join()
    }
}

/**
 * Icon scale animation module
 */
fun createIconScaleAnimation(
    iconScale: Animatable<Float, *>,
    config: IconScaleAnimationConfig = IconScaleAnimationConfig()
): IconButtonAnimation = { scope ->
    scope.launch {
        iconScale.animateTo(config.scaleValue, config.scaleUpAnimation)
        iconScale.animateTo(1f, config.scaleDownAnimation)
    }
}

/**
 * Burst dots visual module
 */
@Composable
fun createBurstDotsVisual(
    burstAlpha: Animatable<Float, *>,
    burstProgress: Animatable<Float, *>,
    config: BurstDotsConfig
): IconButtonVisual = {
    if (burstAlpha.value > 0f) {
        Canvas(modifier = Modifier.size(config.effectSize)) {
            val center = size / 2f
            val radiusPx = config.burstRadius.toPx() * burstProgress.value
            for (i in 0 until config.numDots) {
                val angle = (2 * Math.PI * i) / config.numDots
                val x = center.width + radiusPx * kotlin.math.cos(angle).toFloat()
                val y = center.height + radiusPx * kotlin.math.sin(angle).toFloat()
                drawCircle(
                    color = config.color.copy(alpha = burstAlpha.value),
                    radius = config.dotSize.toPx() * burstAlpha.value,
                    center = Offset(x, y)
                )
            }
        }
    }
}

/**
 * Ring visual module
 */
@Composable
fun createRingVisual(
    ringAlpha: Animatable<Float, *>,
    ringRadius: Animatable<Float, *>,
    config: RingConfig
): IconButtonVisual = {
    if (ringAlpha.value > 0f && ringRadius.value > 0f) {
        Canvas(modifier = Modifier.size(config.effectSize)) {
            val center = size / 2f
            drawCircle(
                color = config.color.copy(alpha = ringAlpha.value),
                radius = config.maxRadius.toPx() * ringRadius.value,
                center = Offset(center.width, center.height),
                style = Stroke(width = config.maxStrokeWidth.toPx() * (1 - ringRadius.value))
            )
        }
    }
}

@Composable
fun BurstIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconSize: Dp = 24.dp,
    activeIconResId: Int,
    inactiveIconResId: Int,
    activeColor: Color,
    inactiveColor: Color,
    isActive: Boolean,
    hapticFeedback: Boolean = true,
    enableBurst: Boolean = true,
    enableRing: Boolean = true,
    enableIconScale: Boolean = true,
    burstConfig: BurstDotsConfig = BurstDotsConfig(color = activeColor),
    ringConfig: RingConfig = RingConfig(color = activeColor.copy(alpha = 0.5f)),
    burstAnimationConfig: BurstAnimationConfig = BurstAnimationConfig(),
    ringAnimationConfig: RingAnimationConfig = RingAnimationConfig(),
    iconScaleConfig: IconScaleAnimationConfig = IconScaleAnimationConfig(),
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    // Animation states
    val iconScale = remember { Animatable(1f) }
    val burstProgress = remember { Animatable(0f) }
    val burstAlpha = remember { Animatable(0f) }
    val ringRadius = remember { Animatable(0f) }
    val ringAlpha = remember { Animatable(0f) }

    // Animation modules
    val burstAnimation = if (enableBurst) createBurstAnimation(burstProgress, burstAlpha, burstAnimationConfig) else null
    val ringAnimation = if (enableRing) createRingAnimation(ringRadius, ringAlpha, ringAnimationConfig) else null
    val iconScaleAnimation = if (enableIconScale) createIconScaleAnimation(iconScale, iconScaleConfig) else null
    
    // Visual modules
    val burstVisual = if (enableBurst) createBurstDotsVisual(burstAlpha, burstProgress, burstConfig) else null
    val ringVisual = if (enableRing) createRingVisual(ringAlpha, ringRadius, ringConfig) else null

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        burstVisual?.invoke()
        ringVisual?.invoke()
        IconButton(onClick = {
            onClick()
            if (!isActive) {
                if (hapticFeedback) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                scope.launch {
                    if (burstAnimation != null) burstAnimation(scope)
                    if (ringAnimation != null) ringAnimation(scope)
                    if (iconScaleAnimation != null) iconScaleAnimation(scope)
                }
            }
        }) {
            Icon(
                painter = painterResource(id = if (isActive) activeIconResId else inactiveIconResId),
                tint = if (isActive) activeColor else inactiveColor,
                contentDescription = null,
                modifier = Modifier
                    .size(iconSize)
                    .graphicsLayer(
                        scaleX = iconScale.value,
                        scaleY = iconScale.value
                    )
            )
        }
    }
}
