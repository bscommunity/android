package com.meninocoiso.bscm.presentation.ui.components.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.domain.model.Chart
import com.meninocoiso.bscm.domain.state.DownloadState
import com.meninocoiso.bscm.presentation.ui.components.layout.CoverArt

/**
 * Single tracklist entry of a tour pass: square cover art with a floating
 * play/pause button (bottom-left) and the track title/artist below.
 */
@Composable
fun TourPassTrackPreview(
    chart: Chart,
    downloadState: DownloadState = DownloadState.Idle,
    isDisabled: Boolean = false,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    onDisabled: () -> Unit = {},
    onClick: () -> Unit
) {
    val track = chart.track
    val hasPreview = track.previewUrl != null

    val isInstalling = downloadState is DownloadState.Downloading ||
        downloadState is DownloadState.Extracting
    val isInstalled = downloadState is DownloadState.Installed
    val downloadProgress = when (downloadState) {
        is DownloadState.Downloading -> downloadState.progress
        is DownloadState.Extracting -> downloadState.progress
        else -> null
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = if (isDisabled) 0.5f else 1f }
            .clickable(onClick = {
                if (isDisabled) {
                    onDisabled()
                } else {
                    onClick()
                }
            })
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            contentAlignment = Alignment.BottomStart
        ) {
            CoverArt(
                url = track.coverUrl ?: "",
                difficulty = chart.difficulty,
                floatingDifficulty = true,
                modifier = Modifier.fillMaxSize(),
                borderRadius = 0.dp,
                width = Dp.Unspecified,
                height = Dp.Unspecified,
            )
            if (hasPreview || isInstalling || isInstalled) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .then(
                            if (isInstalling) Modifier
                            else Modifier.border(1.5.dp, Color.White, CircleShape)
                        )
                        .clickable(
                            enabled = !isInstalling && !isInstalled && !isDisabled,
                            onClick = onTogglePlay
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isInstalling -> {
                            Icon(
                                painter = painterResource(R.drawable.rounded_download_24),
                                contentDescription = stringResource(
                                    if (downloadState is DownloadState.Downloading) {
                                        R.string.downloading
                                    } else {
                                        R.string.extracting
                                    }
                                ),
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                            val progress = downloadProgress
                            if (progress != null && progress > 0f) {
                                CircularProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier.fillMaxSize(),
                                    color = Color.White,
                                    trackColor = Color.White.copy(alpha = 0.3f),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                CircularProgressIndicator(
                                    modifier = Modifier.fillMaxSize(),
                                    color = Color.White,
                                    trackColor = Color.White.copy(alpha = 0.3f),
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                        isInstalled -> {
                            Icon(
                                painter = painterResource(R.drawable.rounded_download_done_24),
                                contentDescription = stringResource(R.string.installed),
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        else -> {
                            Icon(
                                painter = painterResource(
                                    if (isPlaying) R.drawable.pause_24px
                                    else R.drawable.play_arrow_24px
                                ),
                                contentDescription = stringResource(
                                    if (isPlaying) R.string.stop_preview else R.string.play_preview
                                ),
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
        }
        Text(
            text = track.title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
        )
        Text(
            text = track.artist,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            maxLines = 1,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
