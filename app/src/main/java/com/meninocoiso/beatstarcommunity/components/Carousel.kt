package com.meninocoiso.beatstarcommunity.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.coil.CoilImage

@Composable
fun Carousel(carouselItems: List<String>) {
	LazyRow(
		contentPadding = PaddingValues(horizontal = 16.dp),
		horizontalArrangement = Arrangement.spacedBy(16.dp),
		modifier = Modifier
			.fillMaxWidth()
			.padding(vertical = 16.dp)
	) {
		for (item in carouselItems) {
			item {
				CarouselItem(imageUrl = item)
			}
		}
	}
}

@Composable
fun CarouselItem(imageUrl: String) {
	val imageModifier = Modifier
		.padding(8.dp)
		.clip(RoundedCornerShape(48.dp))
		.background(Color.Gray) // Placeholder color while loading

	Box(
		modifier = Modifier
			.fillMaxWidth()
			.height(250.dp) // Set height for carousel items
	) {
		CoilImage(
			imageModel = { imageUrl },
			modifier = imageModifier,
			imageOptions = ImageOptions(
				contentScale = ContentScale.Crop,
				alignment = Alignment.Center,
			)
		)
	}
}