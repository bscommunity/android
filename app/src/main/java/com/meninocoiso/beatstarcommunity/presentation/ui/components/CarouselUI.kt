package com.meninocoiso.beatstarcommunity.presentation.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.meninocoiso.beatstarcommunity.presentation.ui.components.details.YoutubeVideoPlayer
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.coil.CoilImage

sealed class CarouselItem {
	data class ImageItem(
		val imageUrl: String,
		/*@DrawableRes val imageResId: Int,
		@StringRes val contentDescriptionResId: Int,
		val clickUrl: String // URL to open when tapped*/
	) : CarouselItem()

	data class VideoItem(
		val videoId: String,
	) : CarouselItem()
}

val carouselItems =
	listOf(
		CarouselItem.ImageItem("https://is1-ssl.mzstatic.com/image/thumb/Music211/v4/92/9f/69/929f69f1-9977-3a44-d674-11f70c852d1b/24UMGIM36186.rgb.jpg/60x60bb.jpg"),
		CarouselItem.VideoItem("yX8QhVeBXkg"),
	)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestCarousel() {
	HorizontalMultiBrowseCarousel(
		state = rememberCarouselState { carouselItems.count() },
		modifier = Modifier
			.padding(horizontal = 16.dp)
			//.background(Color.Red)
			.height(256.dp)
			.fillMaxWidth(),
		preferredItemWidth = 256.dp,
		itemSpacing = 8.dp,
	) { i ->
		when (val item = carouselItems[i]) {
			is CarouselItem.ImageItem -> {
				// Render a square image (1:1 ratio)
				CoilImage(
					imageModel = { item.imageUrl },
					modifier = Modifier
						//.background(Color.Yellow)
						.height(256.dp)
						.maskClip(MaterialTheme.shapes.extraLarge),
					imageOptions = ImageOptions(
						contentScale = ContentScale.Fit,
						alignment = Alignment.Center,
					),
				)
			}
			is CarouselItem.VideoItem -> {
				// Render the Youtube video preview in 9:16 aspect ratio
				YoutubeVideoPlayer(
					videoId = item.videoId,
					modifier = Modifier
						//.background(Color.Green)
						.height(256.dp)
						.maskClip(MaterialTheme.shapes.extraLarge)
					,
				)
			}
		}
	}
}