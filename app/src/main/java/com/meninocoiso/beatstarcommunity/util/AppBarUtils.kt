package com.meninocoiso.beatstarcommunity.util

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

class AppBarUtils {
	class CollapsingAppBarNestedScrollConnection(
		private val appBarMaxHeight: Int
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

	companion object {
		@Composable
		fun getStatusBarHeight(): Dp {
			return WindowInsets.systemBars.asPaddingValues().calculateTopPadding()
		}

		@Composable
		fun getConnection(
			collapsableHeight: Dp,
			fixedHeight: Dp = 0.dp,
		): Triple<CollapsingAppBarNestedScrollConnection, Dp, Dp> {
			val density = LocalDensity.current
			val statusBarHeight = getStatusBarHeight()

			val appBarMaxHeight = collapsableHeight + fixedHeight + statusBarHeight
			val appBarMaxHeightPx = with(LocalDensity.current) {
				appBarMaxHeight.roundToPx()
			}

			val fixedAppBarHeightPx = if (fixedHeight == 0.dp) {
				appBarMaxHeightPx
			} else {
				with(LocalDensity.current) {
					(fixedHeight + statusBarHeight).roundToPx()
				}
			}

			val connection = remember(appBarMaxHeightPx) {
				CollapsingAppBarNestedScrollConnection(fixedAppBarHeightPx)
			}

			val spaceHeight by remember(density) {
				derivedStateOf {
					with(density) {
						(appBarMaxHeightPx + connection.appBarOffset).toDp()
					}
				}
			}

			return Triple(connection, spaceHeight, statusBarHeight)
		}
	}
}