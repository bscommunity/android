package com.meninocoiso.beatstarcommunity.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import coil.request.ImageRequest
import com.meninocoiso.beatstarcommunity.R
import com.meninocoiso.beatstarcommunity.domain.enums.DifficultyEnum
import com.meninocoiso.beatstarcommunity.domain.lists.difficultiesList
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.coil.CoilImage
import com.skydoves.landscapist.components.rememberImageComponent
import com.skydoves.landscapist.placeholder.shimmer.Shimmer
import com.skydoves.landscapist.placeholder.shimmer.ShimmerPlugin

@Composable
fun CoverArt(
	difficulty: DifficultyEnum? = null,
	borderRadius: Dp = 0.dp,
	size: Dp = 76.dp,
	url: String
) {
	val sizeInPx = with(LocalDensity.current) { size.roundToPx() }

	val difficultyIcon =
		if (difficulty != null) difficultiesList.first { it.id == difficulty }.icon else null

	Box(
		modifier = Modifier
			.size(size)
			.clip(RoundedCornerShape(borderRadius)),
		contentAlignment = Alignment.BottomEnd
	) {
		CoilImage(
			imageModel = {
				url
			},
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
						highlightColor = MaterialTheme.colorScheme.surfaceContainerLow
					)
				)
			}
		)
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
	modifier: Modifier? = Modifier,
	size: Dp = 18.dp,
	url: String,
	key: String? = null
) {
	val context = LocalContext.current
	val pixelSize = with(LocalDensity.current) { size.toPx() }.toInt()

	val customModifier = (modifier ?: Modifier)
		.size(size)
		.clip(CircleShape)

	val imageOptions = ImageOptions(
		contentScale = ContentScale.Fit,
		alignment = Alignment.Center,
		requestSize = IntSize(
			pixelSize,
			pixelSize
		)
	)

	if (key != null) {
		CoilImage(
			imageRequest = {
				ImageRequest.Builder(context)
					.data(url)
					.crossfade(true)
					.placeholderMemoryCacheKey(key) //  same key as shared element key
					.memoryCacheKey(key) // same key as shared element key
					.build()
			},
			imageLoader = {
				ImageLoader.Builder(context)
					.memoryCache(
						coil.memory.MemoryCache.Builder(context)
							.maxSizePercent(1.0)
							.build()
					)
					.build()
			},
			modifier = customModifier,
			imageOptions = imageOptions,
		)
	} else {
		CoilImage(
			imageModel = { url },
			modifier = customModifier,
			imageOptions = imageOptions,
		)
	}
}