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
import com.meninocoiso.bscm.domain.model.TourPass
import com.meninocoiso.bscm.presentation.ui.components.layout.CoverArt
import com.meninocoiso.bscm.presentation.ui.modifiers.debouncedClickable
import com.meninocoiso.bscm.util.PreviewUtils.secondaryContainer
import com.meninocoiso.bscm.util.PreviewUtils.titleContent
import com.meninocoiso.bscm.util.StringUtils

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TourPassPreview(
    tourPass: TourPass,
    modifier: Modifier = Modifier,
    isLocal: Boolean = false,
    isSecondary: Boolean = false,
    isDisabled: Boolean = false,
    onDisabled: () -> Unit = {},
    onPress: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .secondaryContainer(isSecondary)
            .graphicsLayer {
                alpha = if ((tourPass.isInstalled == true || isDisabled) && !isLocal) 0.5f else 1f
            }
            .debouncedClickable(onClick = {
                if (isDisabled) {
                    onDisabled()
                    return@debouncedClickable
                }
                onPress()
            })
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CoverArt(
                modifier = Modifier.fillMaxWidth(),
                url = tourPass.coverUrl ?: "",
                borderRadius = if (isLocal) 8.dp else 0.dp,
                width = 400.dp,
                height = 100.dp
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
                            titleContent(tourPass.name, false, false)
                            Text(
                                style = MaterialTheme.typography.labelLarge,
                                text = tourPass.updatedAt?.let { StringUtils.toRelativeString(it) } ?: ""
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
                            titleContent(tourPass.name, false, false)
                            Text(
                                modifier = Modifier.padding(start = 8.dp),
                                style = MaterialTheme.typography.labelMedium,
                                text = stringResource(R.string.charts_count, tourPass.charts.size)
                            )
                        }
                    }
                    tourPass.artist?.let {
                        Text(style = MaterialTheme.typography.labelMedium, text = tourPass.artist)

                    }
                }
                val contributors = tourPass.contributors
                if (contributors.isNotEmpty()) {
                    PreviewAuthors(
                        contentString = stringResource(
                            R.string.chart_by,
                            contributors[0].user.username
                        ),
                        authors = contributors)
                }
                if (!isLocal && tourPass.isInstalled == true) PreviewInstalledTag(false)
            }
        }
    }
}