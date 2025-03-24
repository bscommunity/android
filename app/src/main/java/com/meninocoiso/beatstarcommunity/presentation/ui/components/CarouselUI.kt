package com.meninocoiso.beatstarcommunity.presentation.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.meninocoiso.beatstarcommunity.presentation.ui.components.details.GameplayPreview
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.coil3.CoilImage
import com.skydoves.landscapist.components.rememberImageComponent
import com.skydoves.landscapist.placeholder.shimmer.Shimmer
import com.skydoves.landscapist.placeholder.shimmer.ShimmerPlugin

sealed class CarouselItem {
	data class ImageItem(
		val imageUrl: String,
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
	BoxWithConstraints(
		modifier = Modifier
			.fillMaxWidth()
			.padding(horizontal = 16.dp)
	) {
		val gap = 12.dp
		// Compute carousel height:
		// h + gap + (h * 9/16) = maxWidth  ==> h = (maxWidth - gap) * (16/25)
		val carouselHeight = maxWidth * (16f / 25f) + gap

		HorizontalMultiBrowseCarousel(
			state = rememberCarouselState { carouselItems.size },
			modifier = Modifier
				.fillMaxWidth()
				.height(carouselHeight),
			preferredItemWidth = carouselHeight,
			itemSpacing = gap,
		) { i ->
			when (val item = carouselItems[i]) {
				is CarouselItem.ImageItem -> {
					// Square image: size equals carouselHeight
					CoilImage(
						imageModel = { item.imageUrl },
						component = rememberImageComponent {
							+ShimmerPlugin(
								Shimmer.Resonate(
									baseColor = Color(0xFFF4F4E8),
									highlightColor = Color(0xFFE9E9DD)
								)
							)
						},
						modifier = Modifier
							.maskClip(MaterialTheme.shapes.extraLarge),
						imageOptions = ImageOptions(
							contentScale = ContentScale.Fit,
							alignment = Alignment.Center,
						),
					)
				}
				is CarouselItem.VideoItem -> {
					// Video: height equals carouselHeight and width is scaled by 9:16 ratio
					GameplayPreview(
						videoId = item.videoId,
						modifier = Modifier
							.maskClip(MaterialTheme.shapes.extraLarge)
					)
				}
			}
		}
	}
}
