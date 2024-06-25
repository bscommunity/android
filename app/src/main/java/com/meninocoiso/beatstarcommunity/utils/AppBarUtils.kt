package com.meninocoiso.beatstarcommunity.utils

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource

class CollapsingAppBarNestedScrollConnection(
	val appBarMaxHeight: Int
) : NestedScrollConnection {

	var appBarOffset: Int by mutableIntStateOf(0)
		private set

	var appBarOpacity: Float by mutableFloatStateOf(1f)
		private set

	override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
		val delta = available.y.toInt()
		val newOffset = appBarOffset + delta
		val previousOffset = appBarOffset
		appBarOffset = newOffset.coerceIn(-appBarMaxHeight, 0)
		val consumed = appBarOffset - previousOffset
		appBarOpacity = 1f + (appBarOffset / appBarMaxHeight.toFloat())
		return Offset(0f, consumed.toFloat())
	}
}