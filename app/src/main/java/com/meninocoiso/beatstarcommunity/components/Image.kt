package com.meninocoiso.beatstarcommunity.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.meninocoiso.beatstarcommunity.R
import com.meninocoiso.beatstarcommunity.data.difficultiesList
import com.meninocoiso.beatstarcommunity.data.enums.DifficultyEnum
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.coil.CoilImage
import com.skydoves.landscapist.components.rememberImageComponent
import com.skydoves.landscapist.placeholder.shimmer.Shimmer
import com.skydoves.landscapist.placeholder.shimmer.ShimmerPlugin

@Composable
fun CoverArt(
	difficulty: DifficultyEnum,
	url: String
) {
	val difficultyIcon = difficultiesList.first { it.id == difficulty }.icon

	Box(modifier = Modifier.size(76.dp), contentAlignment = Alignment.BottomEnd) {
		CoilImage(
			imageModel = {
				url
			},
			modifier = Modifier.size(76.dp),
			imageOptions = ImageOptions(
				contentScale = ContentScale.Fit,
				alignment = Alignment.Center,
				alpha = 1.0f
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
	size: Dp = 18.dp,
	url: String
) {
	val pixelSize = with(LocalDensity.current) { size.toPx() }.toInt()

	CoilImage(
		imageModel = {
			url
		},
		modifier = Modifier
			.size(size)
			.clip(RoundedCornerShape(150.dp)),
		imageOptions = ImageOptions(
			contentScale = ContentScale.Fit,
			alignment = Alignment.Center,
			alpha = 1.0f,
			requestSize = IntSize(
				pixelSize,
				pixelSize
			)
		),
	)
}