package com.meninocoiso.bscm.presentation.ui.components.chart

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.data.remote.dto.ContributorUserDto
import com.meninocoiso.bscm.domain.model.Chart
import com.meninocoiso.bscm.domain.model.Contributor
import com.meninocoiso.bscm.presentation.screens.details.OnNavigateToDetails
import com.meninocoiso.bscm.presentation.ui.components.layout.Avatar
import com.meninocoiso.bscm.presentation.ui.components.layout.CoverArt
import com.meninocoiso.bscm.presentation.ui.modifiers.debouncedClickable
import com.meninocoiso.bscm.util.DateUtils
import java.time.LocalDate

@Composable
fun ChartAuthors(
    authors: List<Contributor>,
    avatarSize: Dp = 18.dp,
) {
    if (authors.isEmpty()) return

    BoxWithConstraints {
        val maxWidthFraction = 0.7f // 70% of the parent's width
        val maxWidthDp = this.maxWidth * maxWidthFraction

        Box(
            modifier = Modifier.border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                RoundedCornerShape(150.dp)
            )
        ) {
            Row(
                modifier = Modifier
                    .padding(start = 6.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy((-4).dp)) {
                    for (author in authors) {
                        Avatar(
                            url = author.user.imageUrl,
                            alt = author.user.username.first().toString(),
                            size = avatarSize
                        )
                    }
                }
                Text(
                    style = MaterialTheme.typography.bodySmall,
                    text = "Chart by @${authors[0].user.username}${if (authors.size > 1) " and others" else ""}",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.sizeIn(maxWidth = maxWidthDp)
                )
            }
        }
    }
}

@Preview
@Composable
fun ChartAuthorsPreview() {
    ChartAuthors(
        authors = listOf(
            Contributor(
                user = ContributorUserDto(
                    id = "1",
                    username = "user1",
                    imageUrl = "https://example.com/image1.jpg",
                    createdAt = LocalDate.now(),
                ),
                chartId = "1",
                roles = emptyList(),
                joinedAt = LocalDate.now()
            ),
        )
    )
}

private fun TrackTitle(
    track: String,
    isExplicit: Boolean,
    isDeluxe: Boolean
): AnnotatedString {
    return buildAnnotatedString {
        append(track)

        if (isExplicit) {
            // Add spacing before the icon
            append("  ") // Two spaces for approximate spacing
            appendInlineContent("explicit", "[E]")
        }
        if (isDeluxe) {
            // Add spacing before the icon
            append("  ") // Two spaces for approximate spacing
            appendInlineContent("deluxe", "[D]")
        }
    }
}

private fun TrackTitleInlineContent(
    track: String,
    isExplicit: Boolean,
    isDeluxe: Boolean
): Map<String, InlineTextContent> {
    val inlineContent = mutableMapOf<String, InlineTextContent>()
    if (isExplicit) {
        inlineContent["explicit"] = InlineTextContent(
            Placeholder(
                width = 14.sp,
                height = 14.sp,
                placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
            )
        ) {
            Icon(
                modifier = Modifier.size(14.dp),
                painter = painterResource(R.drawable.explicit),
                contentDescription = "Explicit"
            )
        }
    }
    if (isDeluxe) {
        inlineContent["deluxe"] = InlineTextContent(
            Placeholder(
                width = 14.sp,
                height = 14.sp,
                placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
            )
        ) {
            Icon(
                modifier = Modifier.size(14.dp),
                painter = painterResource(R.drawable.deluxe),
                contentDescription = "Deluxe"
            )
        }
    }
    return inlineContent
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChartPreview(
    chart: Chart,
    modifier: Modifier? = Modifier,
    isBlocked: Boolean = false,
    onBlocked: () -> Unit = {},
    onNavigateToDetails: () -> Unit
) {
    Box(
        modifier = (modifier ?: Modifier)
            .fillMaxWidth()
            .graphicsLayer {
                alpha = if (chart.isInstalled == true || isBlocked == true) 0.5f else 1f
            }
            .debouncedClickable(onClick = {
                if (isBlocked == true) {
                    onBlocked()
                    return@debouncedClickable
                }
                onNavigateToDetails()
            })
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CoverArt(
                difficulty = chart.difficulty,
                url = chart.coverUrl,
                isInstalled = chart.isInstalled
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.titleMedium,
                                lineHeight = 20.sp,
                                text = TrackTitle(
                                    track = chart.track,
                                    isExplicit = chart.isExplicit,
                                    isDeluxe = chart.isDeluxe
                                ),
                                inlineContent = TrackTitleInlineContent(
                                    track = chart.track,
                                    isExplicit = chart.isExplicit,
                                    isDeluxe = chart.isDeluxe
                                ),
                            )

                            Text(
                                modifier = Modifier.padding(start = 8.dp),
                                style = MaterialTheme.typography.labelMedium,
                                text = DateUtils.toRelativeString(chart.latestVersion.publishedAt)
                            )
                        }
                    }
                    Text(style = MaterialTheme.typography.labelMedium, text = chart.artist)
                }
                ChartAuthors(authors = chart.contributors)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LocalChartPreview(
    chart: Chart,
    modifier: Modifier? = Modifier,
    onNavigateToDetails: OnNavigateToDetails
) {
    Box(
        modifier = (modifier ?: Modifier)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable() {
                onNavigateToDetails(
                    chart.copy()
                    /* TODO */
                )
            }
    ) {
        Row(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CoverArt(
                difficulty = chart.difficulty,
                url = chart.coverUrl,
                borderRadius = 8.dp
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Column {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                            text = TrackTitle(
                                track = chart.track,
                                isExplicit = chart.isExplicit,
                                isDeluxe = chart.isDeluxe
                            ),
                            inlineContent = TrackTitleInlineContent(
                                track = chart.track,
                                isExplicit = chart.isExplicit,
                                isDeluxe = chart.isDeluxe
                            )
                        )
                        Text(
                            style = MaterialTheme.typography.labelLarge,
                            text = "v${chart.latestVersion.index + 1}"
                        )
                    }
                    Text(style = MaterialTheme.typography.labelMedium, text = chart.artist)
                }
                ChartAuthors(authors = chart.contributors)
            }
        }
    }
}