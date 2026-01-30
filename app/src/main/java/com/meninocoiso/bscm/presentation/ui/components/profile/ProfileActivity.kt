package com.meninocoiso.bscm.presentation.ui.components.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.domain.model.Chart
import com.meninocoiso.bscm.domain.result.ContentState
import com.meninocoiso.bscm.presentation.ui.components.StatusMessageUI
import com.meninocoiso.bscm.presentation.ui.components.preview.ChartPreview
import com.meninocoiso.bscm.presentation.viewmodel.ActivityItem

@Composable
fun ProfileActivity(
    items: List<ActivityItem>,
    state: ContentState,
    onFetch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BaseContainer(
        isEmpty = items.isEmpty(),
        state = state,
        onRetry = onFetch,
        empty = {
            StatusMessageUI(
                modifier = Modifier.fillMaxWidth(),
                message = "No recent activity",
                icon = R.drawable.rounded_update_disabled_24
            )
        }
    ) {
        LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.Top
        ) {
            items(items.size) { index ->
                val item = items[index]

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Timeline indicator
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Top,
                        modifier = Modifier
                            .width(72.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(
                                    MaterialTheme.colorScheme.onSecondaryContainer,
                                    shape = RoundedCornerShape(99.dp)
                                )
                        )
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.onSecondaryContainer)
                        )
                    }

                    // Content
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            "@meninocoiso liked ${item.content.size} charts",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "5 days ago",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Chart previews
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            item.content.forEach { chart ->
                                when (chart) {
                                    is Chart -> {
                                        ChartPreview(
                                            chart = chart,
                                            isSecondary = true,
                                            onPress = { /* Navigate to chart details */ }
                                        )
                                    }

                                    else -> { /* Handle other content types if necessary */
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}