package com.meninocoiso.bscm.presentation.ui.components.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.domain.model.Chart
import com.meninocoiso.bscm.presentation.ui.components.layout.CoverArt
import com.meninocoiso.bscm.presentation.ui.components.layout.GradientPlaceholder
import com.meninocoiso.bscm.presentation.ui.modifiers.debouncedClickable
import com.meninocoiso.bscm.util.PreviewUtils.localContainer
import com.meninocoiso.bscm.util.PreviewUtils.titleContent
import com.meninocoiso.bscm.util.StringUtils

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChartPreview(
    chart: Chart,
    modifier: Modifier = Modifier,
    isInstalled: Boolean? = null,
    isDisabled: Boolean = false,
    onDisabled: () -> Unit = {},
    onPress: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .localContainer(isInstalled != null)
            .graphicsLayer {
                alpha =
                    if (isInstalled == true || isDisabled) 0.5f else 1f
            }
            .debouncedClickable(onClick = {
                if (isDisabled) {
                    onDisabled()
                    return@debouncedClickable
                }
                onPress()
            })
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (chart.coverUrl.isEmpty() && chart.colors?.isNotEmpty() == true) {
                GradientPlaceholder(
                    colors = chart.colors.map {
                        Color("#$it".toColorInt())
                    },
                    borderRadius = if (isInstalled == true) 8.dp else 0.dp,
                )
            } else {
                CoverArt(
                    difficulty = chart.latestVersion.difficulty,
                    url = chart.coverUrl,
                    borderRadius = if (isInstalled == true) 8.dp else 0.dp,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Column {
                    if (isInstalled == true) {
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            titleContent(
                                chart.track,
                                chart.latestVersion.isExplicit,
                                chart.latestVersion.isDeluxe
                            )
                            Text(
                                style = MaterialTheme.typography.labelLarge,
                                text = "v${chart.latestVersion.index}"
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            titleContent(
                                chart.track,
                                chart.latestVersion.isExplicit,
                                chart.latestVersion.isDeluxe
                            )
                            Text(
                                modifier = Modifier.padding(start = 8.dp),
                                style = MaterialTheme.typography.labelMedium,
                                text = StringUtils.toRelativeString(chart.latestVersion.publishedAt)
                            )
                        }
                    }
                    Text(style = MaterialTheme.typography.labelMedium, text = chart.artist)
                }
                if (chart.contributors.isNotEmpty()) {
                    PreviewAuthors(
                        contentString = stringResource(
                            R.string.chart_by,
                            chart.contributors[0].user.username
                        ),
                        authors = chart.contributors
                    )
                }
                if (isInstalled != null) PreviewInstalledTag(isInstalled)
            }
        }
    }
}