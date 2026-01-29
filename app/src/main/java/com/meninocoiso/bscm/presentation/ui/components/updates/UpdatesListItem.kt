package com.meninocoiso.bscm.presentation.ui.components.updates

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.domain.model.Chart
import com.meninocoiso.bscm.presentation.ui.components.layout.CoverArt
import com.meninocoiso.bscm.domain.state.DownloadState

@Composable
internal fun UpdateListItem(
    chart: Chart,
    downloadState: DownloadState,
    onUpdateClick: () -> Unit
) {
    ListItem(
        modifier = Modifier.clip(RoundedCornerShape(16.dp)),
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        leadingContent = {
            CoverArt(
                difficulty = null,
                borderRadius = 2.dp,
                url = chart.coverUrl,
                size = 40.dp
            )
        },
        headlineContent = {
            Text(
                text = chart.track,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                lineHeight = TextUnit(1f, TextUnitType.Em)
            )
        },
        supportingContent = {
            Text(
                text = stringResource(
                    R.string.update_from_to,
                    chart.latestVersion.index,
                    chart.availableVersion?.index ?: 0
                ),
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = TextUnit(1f, TextUnitType.Em)
            )
        },
        trailingContent = {
            IconButton(
                onClick = onUpdateClick,
                enabled = downloadState !is DownloadState.Downloading &&
                        downloadState !is DownloadState.Extracting,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = Color.Transparent,
                )
            ) {
                when(downloadState) {
                    is DownloadState.Downloading, is DownloadState.Extracting -> {
                        CircularProgressIndicator(
                            progress = {
                                when(downloadState) {
                                    is DownloadState.Downloading -> downloadState.progress
                                    is DownloadState.Extracting -> downloadState.progress
                                    else -> 0f
                                }
                            },
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    is DownloadState.Error -> {
                        Icon(
                            painter = painterResource(id = R.drawable.rounded_error_24),
                            contentDescription = null
                        )
                    }
                    else -> {
                        Icon(
                            painter = painterResource(id = R.drawable.rounded_download_24),
                            contentDescription = stringResource(R.string.update)
                        )
                    }
                }
            }
        })
}