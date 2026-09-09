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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.data.remote.dto.activity.ActivityItemResponse
import com.meninocoiso.bscm.data.remote.dto.activity.ChartActivityItem
import com.meninocoiso.bscm.data.remote.dto.activity.ThemeActivityItem
import com.meninocoiso.bscm.data.remote.dto.activity.TourPassActivityItem
import com.meninocoiso.bscm.domain.result.ContentState
import com.meninocoiso.bscm.presentation.screen.details.OnNavigateToDetails
import com.meninocoiso.bscm.presentation.ui.components.StatusMessageSize
import com.meninocoiso.bscm.presentation.ui.components.StatusMessageUI
import com.meninocoiso.bscm.presentation.ui.components.preview.ChartPreview
import com.meninocoiso.bscm.presentation.ui.components.preview.ThemePreview
import com.meninocoiso.bscm.presentation.ui.components.preview.TourPassPreview
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Composable
fun ProfileActivity(
    items: List<ActivityItemResponse>,
    state: ContentState,
    onFetch: (reset: Boolean) -> Unit,
    listState: LazyListState,
    isLoadingMore: Boolean,
    hasMore: Boolean,
    onLoadMore: () -> Unit,
    onNavigateToDetails: OnNavigateToDetails,
    modifier: Modifier = Modifier,
) {
    OnScrollLoadMore(
        listState = listState,
        hasMore = hasMore,
        isLoadingMore = isLoadingMore,
        onLoadMore = onLoadMore
    )

    BaseContainer(
        isEmpty = items.isEmpty(),
        state = state,
        onRetry = onFetch,
        empty = {
            StatusMessageUI(
                modifier = Modifier.fillMaxWidth().padding(top = 36.dp),
                size = StatusMessageSize.Medium,
                message = stringResource(R.string.no_recent_activity),
                icon = R.drawable.rounded_update_disabled_24
            )
        }
    ) {
        LazyColumn(
            modifier = modifier,
            state = listState,
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 24.dp),
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
                            text = getActivityText(item),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = getRelativeTime(item.createdAt),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Content preview
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            when (item) {
                                is ChartActivityItem -> {
                                    ChartPreview(
                                        chart = item.chart,
                                        isSecondary = true,
                                        onPress = { onNavigateToDetails(item.chart) }
                                    )
                                }
                                is ThemeActivityItem -> {
                                    ThemePreview(
                                        theme = item.theme,
                                        isSecondary = true,
                                        onPress = { onNavigateToDetails(item.theme) }
                                    )
                                }
                                is TourPassActivityItem -> {
                                    TourPassPreview(
                                        tourPass = item.tourPass,
                                        isSecondary = true,
                                        onPress = { onNavigateToDetails(item.tourPass) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            pagination(
                isLoadingMore = isLoadingMore,
                showMessage = !hasMore
            )
        }
    }
}

@Composable
private fun getActivityText(item: ActivityItemResponse): String {
    return when (item) {
        is ChartActivityItem -> {
            val name = item.chart.track.title
            stringResource(R.string.activity_created_content, stringResource(R.string.content_type_chart), name)
        }
        is ThemeActivityItem -> {
            val name = item.theme.name
            stringResource(R.string.activity_created_content, stringResource(R.string.content_type_theme), name)
        }
        is TourPassActivityItem -> {
            val name = item.tourPass.name
            stringResource(R.string.activity_created_content, stringResource(R.string.content_type_tour_pass), name)
        }
    }
}

/**
 * Returns a relative time string (e.g., "5 days ago")
 */
@Composable
private fun getRelativeTime(createdAt: LocalDateTime): String {
    val now = LocalDateTime.now()
    val days = ChronoUnit.DAYS.between(createdAt, now).toInt()
    val hours = ChronoUnit.HOURS.between(createdAt, now).toInt()
    val minutes = ChronoUnit.MINUTES.between(createdAt, now).toInt()

    return when {
        days > 0 -> pluralStringResource(R.plurals.days_ago, days, days)
        hours > 0 -> pluralStringResource(R.plurals.hours_ago, hours, hours)
        minutes > 0 -> pluralStringResource(R.plurals.minutes_ago, minutes, minutes)
        else -> stringResource(R.string.just_now)
    }
}

