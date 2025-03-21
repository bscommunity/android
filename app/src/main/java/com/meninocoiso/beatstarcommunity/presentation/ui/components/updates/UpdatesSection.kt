package com.meninocoiso.beatstarcommunity.presentation.ui.components.updates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.meninocoiso.beatstarcommunity.R
import com.meninocoiso.beatstarcommunity.presentation.ui.components.Size
import com.meninocoiso.beatstarcommunity.presentation.ui.components.StatusMessageUI
import com.meninocoiso.beatstarcommunity.presentation.ui.components.layout.Section
import com.meninocoiso.beatstarcommunity.presentation.ui.components.layout.SectionWrapper
import com.meninocoiso.beatstarcommunity.presentation.ui.modifiers.fabScrollObserver
import com.meninocoiso.beatstarcommunity.presentation.ui.modifiers.shimmerLoading
import com.meninocoiso.beatstarcommunity.presentation.viewmodel.UpdatesState

private object SectionWrapperDefaults {
    val contentPadding = PaddingValues(horizontal = 16.dp)
    val verticalArrangement = Arrangement.spacedBy(8.dp)
    val horizontalAlignment = Alignment.CenterHorizontally
}

@Composable
fun UpdatesSection(
    updatesState: UpdatesState,
    onFetchUpdates: () -> Unit,
    onFabStateChange: (Boolean) -> Unit,
    nestedScrollConnection: NestedScrollConnection,
) {
    Section(
        title = when (updatesState) {
            is UpdatesState.Success -> "Updates available (${updatesState.charts.size})"
            else -> null
        },
        titleModifier = Modifier.padding(start = 16.dp, bottom = 12.dp),
        thickness = 0.dp,
    ) {
        when (updatesState) {
            is UpdatesState.Error -> {
                UpdatesPanel {
                    StatusMessageUI(
                        title = "We couldn't fetch updates...",
                        message = "Please check your connection and try again later",
                        icon = R.drawable.rounded_hourglass_disabled_24,
                        size = Size.Small,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            is UpdatesState.Loading -> {
                UpdatesPanel {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(20.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .shimmerLoading(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surfaceContainerLow,
                                    MaterialTheme.colorScheme.surfaceContainerHigh,
                                    MaterialTheme.colorScheme.surfaceContainerLow
                                )
                            )
                    )
                }
            }
            is UpdatesState.Success -> {
                val chartsList = updatesState.charts
                if (chartsList.isEmpty()) {
                    UpdatesPanel {
                        Text(text = "No updates available")
                    }
                } else {
                    SectionWrapper(
                        modifier = Modifier
                            .nestedScroll(nestedScrollConnection)
                            .fabScrollObserver(onFabStateChange),
                        contentPadding = SectionWrapperDefaults.contentPadding,
                        verticalArrangement = SectionWrapperDefaults.verticalArrangement,
                        horizontalAlignment = SectionWrapperDefaults.horizontalAlignment
                    ) {
                        items(chartsList) { chart ->
                            UpdateListItem(chart)
                        }
                    }
                }
            }
        }
        UpdatesButton(updatesState = updatesState, onFetchUpdates = onFetchUpdates)
    }
}