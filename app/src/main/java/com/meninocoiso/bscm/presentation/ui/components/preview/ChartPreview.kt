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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.domain.model.Chart
import com.meninocoiso.bscm.presentation.ui.components.layout.CoverArt
import com.meninocoiso.bscm.presentation.ui.modifiers.debouncedClickable
import com.meninocoiso.bscm.util.PreviewUtils.localContainer
import com.meninocoiso.bscm.util.PreviewUtils.titleContent
import com.meninocoiso.bscm.util.StringUtils

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChartPreview(
    chart: Chart,
    modifier: Modifier = Modifier,
    isLocal: Boolean = false,
    isDisabled: Boolean = false,
    onDisabled: () -> Unit = {},
    onNavigateToDetails: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .localContainer(isLocal)
            .graphicsLayer {
                alpha =
                    if ((chart.isInstalled == true || isDisabled) && !isLocal) 0.5f else 1f
            }
            .debouncedClickable(onClick = {
                if (isDisabled) {
                    onDisabled()
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
                difficulty = chart.latestVersion.difficulty,
                url = chart.coverUrl,
                borderRadius = if (isLocal) 8.dp else 0.dp,
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Column {
                    if (isLocal) {
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
                PreviewAuthors(
                    contentString = stringResource(
                        R.string.chart_by,
                        chart.contributors[0].user.username
                    ), 
                    authors = chart.contributors
                )
                if (!isLocal && chart.isInstalled == true) PreviewInstalledTag()
            }
        }
    }
}