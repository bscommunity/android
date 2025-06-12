package com.meninocoiso.bscm.presentation.ui.components

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.meninocoiso.bscm.presentation.ui.components.details.GameplayPreview
import com.meninocoiso.bscm.presentation.ui.components.details.GameplayPreviewThumbnail
import com.meninocoiso.bscm.presentation.ui.components.details.OpenLinkIntent
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
        val videoId: String? = null,
    ) : CarouselItem()
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaCarousel(items: List<CarouselItem>, isVideoEnabled: Boolean) {
    val context = LocalContext.current

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
            state = rememberCarouselState { items.size },
            modifier = Modifier
                .fillMaxWidth()
                .height(carouselHeight),
            preferredItemWidth = carouselHeight,
            itemSpacing = gap,
        ) { i ->
            when (val item = items[i]) {
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
                    if (item.videoId.isNullOrEmpty()) {
                        Box(
                            modifier = Modifier
                                .maskClip(MaterialTheme.shapes.extraLarge)
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceContainer)
                                .zIndex(2f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No gameplay available",
                                modifier = Modifier.align(Alignment.Center).widthIn(max = 75.dp),
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else if (isVideoEnabled) {
                        // Video: height equals carouselHeight and width is scaled by 9:16 ratio
                        GameplayPreview(
                            videoId = item.videoId,
                            context = context,
                            modifier = Modifier
                                .maskClip(MaterialTheme.shapes.extraLarge)
                        )
                    } else {
                        GameplayPreviewThumbnail(
                            videoId = item.videoId,
                            modifier = Modifier
                                .maskClip(MaterialTheme.shapes.extraLarge)
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceContainer)
                                .zIndex(2f)
                                .clickable {
                                    context.startActivity(OpenLinkIntent(item.videoId))
                                }
                        )
                    }
                }
            }
        }
    }
}
