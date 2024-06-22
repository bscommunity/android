package com.meninocoiso.beatstarcommunity.components

import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
fun Carousel(imageUrls: List<String>) {
	val context = LocalContext.current

	val itemMargin = LocalDensity.current.run { 24.dp.toPx() }
	val startEndMargin = LocalDensity.current.run { 16.dp.toPx() }

	AndroidView(
		factory = {
			val recyclerView = RecyclerView(context).apply {
				clipChildren = false
				clipToPadding = false
				layoutManager = CarouselLayoutManager(HeroCarouselStrategy())
				adapter = CarouselAdapter(imageUrls)
				CarouselSnapHelper().attachToRecyclerView(this)

				addItemDecoration(
					CarouselItemDecoration(
						startEndMargin.toInt(),
						itemMargin.toInt()
					)
				)
			}
			recyclerView
		},
		modifier = Modifier
			.fillMaxWidth()
			.padding(vertical = 16.dp)
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

class CarouselItemDecoration(
	private val startEndMargin: Int,
	private val itemMargin: Int
) : RecyclerView.ItemDecoration() {

	override fun getItemOffsets(
		outRect: Rect,
		view: View,
		parent: RecyclerView,
		state: RecyclerView.State
	) {
		val position = parent.getChildAdapterPosition(view)
		val itemCount = parent.adapter?.itemCount ?: 0

		if (position == RecyclerView.NO_POSITION) {
			return
		}

		when (position) {
			0 -> {
				outRect.left = startEndMargin
				outRect.right = itemMargin / 2
			}

			itemCount - 1 -> {
				outRect.left = itemMargin / 2
				outRect.right = startEndMargin
			}

			else -> {
				outRect.left = itemMargin / 2
				outRect.right = itemMargin / 2
			}
		}
	}
}