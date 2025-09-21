package com.meninocoiso.bscm.presentation.ui.components.details

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
    val scope = rememberCoroutineScope()
    
    var isLiked by remember { mutableStateOf(defaultValue) }

    // Heart scale animation
    val heartScale = remember { Animatable(1f) }

    Box(contentAlignment = Alignment.Center) {

        // ❤️ Heart button
        IconButton(
            onClick = {
                isLiked = !isLiked
                onLikeChanged(isLiked)
                // Heart scale animation: scale up then down
                scope.launch {
                    heartScale.animateTo(1.3f, tween(120))
                    heartScale.animateTo(1f, tween(180))
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