package com.meninocoiso.beatstarcommunity.presentation.ui.components.layout

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil3.compose.AsyncImage
import com.meninocoiso.beatstarcommunity.R
import com.meninocoiso.beatstarcommunity.domain.enums.Difficulty
import com.meninocoiso.beatstarcommunity.domain.lists.difficultiesList
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.coil3.CoilImage
import com.skydoves.landscapist.components.rememberImageComponent
import com.skydoves.landscapist.placeholder.shimmer.Shimmer
import com.skydoves.landscapist.placeholder.shimmer.ShimmerPlugin
import java.util.Locale

@Composable
fun imageLoaderSingleton(): ImageLoader {
    val context = LocalContext.current
    return ImageLoader.Builder(context)
        .allowHardware(false) // Disable hardware bitmaps for shared transitions
        .crossfade(true)
        .memoryCache(
            coil.memory.MemoryCache.Builder(context)
                .maxSizePercent(0.25)
                .build()
        )
        .build()
}

@Composable
fun CoverArt(
    difficulty: Difficulty? = null,
    isInstalled: Boolean? = null,
    borderRadius: Dp = 0.dp,
    size: Dp = 76.dp,
    url: String
) {
    val sizeInPx = with(LocalDensity.current) { size.roundToPx() }

    val difficultyIcon =
        if (difficulty != null) difficultiesList.first { it.difficulty == difficulty }.icon
        else null

    Box(
        modifier = Modifier
			.size(size)
			.clip(RoundedCornerShape(borderRadius)),
        contentAlignment = Alignment.BottomEnd
    ) {
        CoilImage(
            // DEBUG: Slow image loading for Shimmer testing
            // imageModel = { "http://10.255.255.1/slow.jpg" },
            imageModel = { url },
            modifier = Modifier
                .size(size),
            imageOptions = ImageOptions(
                contentScale = ContentScale.Fit,
                alignment = Alignment.Center,
                requestSize = IntSize(sizeInPx, sizeInPx)
            ),
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
                        .matchParentSize(),
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
        if (isInstalled == true) {
            Box(
                modifier = Modifier
                    .fillMaxSize().align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.rounded_download_done_24),
                    contentDescription = "Already downloaded chart indicator",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(40.dp),
                )
            }
        }
        if (difficultyIcon != null) {
            Box(
                modifier = Modifier
                    .size(40.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.corner),
                    contentDescription = "Corner for chart song cover art",
                    tint = Color.Black,
                    modifier = Modifier
                        .size(40.dp),
                )
                Image(
                    painter = painterResource(id = difficultyIcon),
                    modifier = Modifier
						.size(24.dp)
						.offset(x = 0.8.dp),
                    contentDescription = "Difficulty icon for chart",
                )
            }
        }
    }
}

@Composable
fun Avatar(
    modifier: Modifier = Modifier,
    size: Dp = 18.dp,
    url: String? = null,
    alt: String
) {
    if (url != null) {
        AsyncImage(
            model = url,
            contentDescription = null,
            modifier = modifier
                .size(size)
                .clip(CircleShape),
            contentScale = ContentScale.Fit,
            alignment = Alignment.Center,
        )
    } else {
        AvatarPlaceholder(size = size, alt = alt)
    }
}

@Composable
fun AvatarPlaceholder(size: Dp = 18.dp, alt: String) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = alt.uppercase(Locale.getDefault()),
            style = (size > 24.dp).let {
                if (it) MaterialTheme.typography.titleMedium
                else MaterialTheme.typography.labelSmall
            },
        )
    }
}