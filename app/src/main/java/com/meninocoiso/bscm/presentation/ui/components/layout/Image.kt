package com.meninocoiso.bscm.presentation.ui.components.layout

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.domain.enums.Difficulty
import com.meninocoiso.bscm.domain.lists.getDifficultiesList
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.coil3.CoilImage
import com.skydoves.landscapist.components.rememberImageComponent
import com.skydoves.landscapist.placeholder.shimmer.Shimmer
import com.skydoves.landscapist.placeholder.shimmer.ShimmerPlugin
import java.util.Locale

@Composable
fun CoverArt(
    modifier: Modifier = Modifier,
    difficulty: Difficulty? = null,
    borderRadius: Dp = 0.dp,
    size: Dp = 76.dp,
    url: String,
    contentScale: ContentScale = ContentScale.Crop
) {
    CoverArt(
        modifier = modifier,
        difficulty = difficulty,
        borderRadius = borderRadius,
        width = size,
        height = size,
        url = url,
        contentScale = contentScale
    )
}

@Composable
fun CoverArt(
    modifier: Modifier = Modifier,
    difficulty: Difficulty? = null,
    floatingDifficulty: Boolean = false,
    borderRadius: Dp = 0.dp,
    width: Dp = 76.dp,
    height: Dp = 76.dp,
    url: String,
    contentScale: ContentScale = ContentScale.Crop
) {
    val hasExplicitSize = width != Dp.Unspecified && height != Dp.Unspecified
    val sizeModifier = if (hasExplicitSize) Modifier.size(width, height) else Modifier
    val imageModifier = if (hasExplicitSize) sizeModifier else Modifier.fillMaxSize()
    val sizeInPx = if (hasExplicitSize) {
        with(LocalDensity.current) { width.roundToPx() to height.roundToPx() }
    } else {
        0 to 0
    }

    val difficultiesList = getDifficultiesList()

    val difficultyIcon =
        if (difficulty != null) difficultiesList.first { it.id == difficulty }.icon
        else null

    Box(
        modifier = modifier
            .then(sizeModifier)
            .clip(RoundedCornerShape(borderRadius)),
        contentAlignment = Alignment.BottomEnd
    ) {
        val imageOptions = if (hasExplicitSize) {
            ImageOptions(
                contentScale = contentScale,
                alignment = Alignment.Center,
                requestSize = IntSize(sizeInPx.first, sizeInPx.second)
            )
        } else {
            ImageOptions(
                contentScale = contentScale,
                alignment = Alignment.Center
            )
        }

        CoilImage(
            // DEBUG: Slow image loading for Shimmer testing
            // imageModel = { "http://10.255.255.1/slow.jpg" },
            imageModel = { url },
            modifier = imageModifier,
            imageOptions = imageOptions,
            component = rememberImageComponent {
                +ShimmerPlugin(
                    Shimmer.Resonate(
                        baseColor = MaterialTheme.colorScheme.background,
                        highlightColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                )
            },
            failure = {
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp),
                        painter = painterResource(R.drawable.rounded_emergency_home_24),
                        contentDescription = null
                    )
                }
            }
        )
        if (difficultyIcon != null) {
            when (floatingDifficulty) {
                true -> Box(
                    contentAlignment = Alignment.TopEnd,
                    modifier = Modifier.matchParentSize()
                ) {
                    Image(
                        painter = painterResource(id = difficultyIcon),
                        modifier = Modifier
                            .size(40.dp)
                            .offset(x = 10.dp, y = (-5).dp),
                        contentDescription = null,
                    )
                }

                false -> Box(
                    modifier = Modifier
                        .size(40.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.corner),
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier
                            .size(40.dp),
                    )
                    Image(
                        painter = painterResource(id = difficultyIcon),
                        modifier = Modifier
                            .size(24.dp)
                            .offset(x = 0.8.dp),
                        contentDescription = null,
                    )
                }
            }
        }
    }
}

@Composable
fun Avatar(
    modifier: Modifier = Modifier,
    size: Dp = 18.dp,
    url: String? = null,
    alt: String? = null,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        AvatarPlaceholder(size = size, alt = alt)
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize(),
                contentScale = ContentScale.Fit,
                alignment = Alignment.Center,
            )
        }
    }
}

@Composable
fun AvatarPlaceholder(size: Dp = 18.dp, alt: String?) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        if (alt != null) {
            Text(
                text = alt.uppercase(Locale.getDefault()),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = when {
                        size > 40.dp -> 24.sp
                        size > 24.dp -> 16.sp
                        else -> 12.sp
                    }
                ),
            )
        } else {
            Icon(
                painter = painterResource(id = R.drawable.rounded_person_24px),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .size(size * 0.4f)
            )
        }
    }
}