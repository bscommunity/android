package com.meninocoiso.beatstarcommunity.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.meninocoiso.beatstarcommunity.data.Chart

@Composable
fun ChartPreview(
	modifier: Modifier = Modifier,
	chart: Chart,
	isFeatured: Boolean,
	isRanked: Boolean,
	isAdquired: Boolean,
) {
	Box(modifier = Modifier) {
		Row {

		}
	}
}

@Composable
fun LocalChartPreview(
	modifier: Modifier = Modifier,
	chart: Chart,
	version: Int,
) {

}