package com.meninocoiso.bscm.util

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object AppBarUtils {
    class CollapsingAppBarNestedScrollConnection(
        private val appBarMaxHeight: Int,
        private val additionalHeight: Int?
    ) : NestedScrollConnection {
        var appBarOffset: Int by mutableIntStateOf(0)
            internal set

        var appBarAdditionalOffset: Int by mutableIntStateOf(additionalHeight ?: 0)
            internal set

        var appBarOpacity: Float by mutableFloatStateOf(1f)
            internal set

        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            val delta = available.y.toInt()
            val newOffset = appBarOffset + delta
            val previousOffset = appBarOffset

            appBarOffset = newOffset.coerceIn(-appBarMaxHeight, 0)

            if (additionalHeight != null) {
                val additionalOffset = appBarAdditionalOffset + delta
                appBarAdditionalOffset =
                    additionalOffset.coerceIn(-additionalHeight * 2, additionalHeight)
            }

            val consumed = appBarOffset - previousOffset
            appBarOpacity = 1f + (appBarOffset / appBarMaxHeight.toFloat())
            return Offset(0f, consumed.toFloat())
        }
    }

    @Composable
    fun getStatusBarHeight(): Dp {
        return WindowInsets.systemBars.asPaddingValues().calculateTopPadding()
    }

    @Composable
    fun getConnection(
        collapsableHeight: Dp,
        fixedHeight: Dp = 0.dp,
        bottomCollapsableHeight: State<Dp?> = remember { mutableStateOf(null) }
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

        val bottomCollapsableHeightPx = bottomCollapsableHeight.value?.let {
            with(density) { it.roundToPx() }
        }

        val connection = rememberSaveable(
            appBarMaxHeightPx, bottomCollapsableHeightPx, saver = listSaver(
                save = {
                    listOf(
                        it.appBarOffset,
                        it.appBarAdditionalOffset,
                        it.appBarOpacity
                    )
                },
                restore = {
                    CollapsingAppBarNestedScrollConnection(
                        fixedAppBarHeightPx,
                        bottomCollapsableHeightPx
                    ).apply {
                        appBarOffset = it[0] as Int
                        appBarAdditionalOffset = it[1] as Int
                        appBarOpacity = it[2] as Float
                    }
                }
            )) {
            CollapsingAppBarNestedScrollConnection(
                fixedAppBarHeightPx,
                bottomCollapsableHeightPx
            )
        }

        val spaceHeight by remember(density, connection, bottomCollapsableHeightPx) {
            derivedStateOf {
                with(density) {
                    (appBarMaxHeightPx + (connection.appBarAdditionalOffset.takeIf { bottomCollapsableHeight.value != null }
                        ?: connection.appBarOffset)).toDp()
                }
            }
        }

        return Triple(connection, spaceHeight, statusBarHeight)
    }
}