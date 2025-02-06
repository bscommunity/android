package com.meninocoiso.beatstarcommunity.presentation.ui.components

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.android.material.carousel.CarouselLayoutManager
import com.google.android.material.carousel.CarouselSnapHelper
import com.google.android.material.carousel.HeroCarouselStrategy
import com.google.android.material.carousel.MaskableFrameLayout
import com.google.android.material.shape.ShapeAppearanceModel
import com.meninocoiso.beatstarcommunity.R

@Composable
fun CarouselUI(imageUrls: List<String>) {
	val context = LocalContext.current

	AndroidView(
		factory = {
			val recyclerView = RecyclerView(context).apply {
				clipChildren = false
				clipToPadding = false
				layoutManager = CarouselLayoutManager(HeroCarouselStrategy())
				adapter = CarouselAdapter(imageUrls)
				CarouselSnapHelper().attachToRecyclerView(this)
			}
			recyclerView
		},
		modifier = Modifier
			.fillMaxWidth()
			.padding(12.dp)
			.height(256.dp)
	)
}

class CarouselAdapter(private val imageUrls: List<String>) :
	RecyclerView.Adapter<CarouselAdapter.ViewHolder>() {

	inner class ViewHolder(private val maskableFrameLayout: MaskableFrameLayout) :
		RecyclerView.ViewHolder(maskableFrameLayout) {
		val imageView: ImageView = maskableFrameLayout.findViewById(R.id.carousel_image_view)
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		val maskableFrameLayout = LayoutInflater.from(parent.context)
			.inflate(R.layout.carousel_item, parent, false) as MaskableFrameLayout

		val newShape = ShapeAppearanceModel().toBuilder().setAllCornerSizes(48f).build()
		maskableFrameLayout.shapeAppearanceModel = newShape

		return ViewHolder(maskableFrameLayout)
	}

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		holder.imageView.load(imageUrls[position]) {
			crossfade(true)
		}
	}

	override fun getItemCount(): Int = imageUrls.size
}