package com.meninocoiso.bscm.presentation.ui.components.details

import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.meninocoiso.bscm.R
import kotlinx.coroutines.delay

@Composable
fun LikeButton(
    chartId: String,
    defaultValue: Boolean = false,
) {
    var isLiked by remember { mutableStateOf(defaultValue) }

    // transient trigger used to play the AVD once
    var avdTrigger by remember { mutableStateOf(false) }

    // load animated vector drawable
    val image: AnimatedImageVector = AnimatedImageVector.animatedVectorResource(
        id = R.drawable.baseline_animator_favorite_24
    )

    val painter = rememberAnimatedVectorPainter(animatedImageVector = image, atEnd = avdTrigger)

    // reset transient trigger after animation duration (400ms pulse + short pop)
    LaunchedEffect(avdTrigger) {
        if (avdTrigger) {
            // total duration should be >= the longest animator (400ms here)
            delay(450)
            avdTrigger = false
        }
    }

    Image(
        painter = painter,
        contentDescription = if (isLiked) "Liked" else "Not liked",
        modifier = Modifier
            .size(48.dp)
            .clickable {
                val newValue = !isLiked
                if (newValue) {
                    // only fire animation on like (not on unlike)
                    avdTrigger = true
                }
                isLiked = newValue
            }
    )
}