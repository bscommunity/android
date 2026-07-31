package com.meninocoiso.bscm.presentation.screen.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.domain.model.TourPass
import com.meninocoiso.bscm.presentation.ui.components.details.StatListItem
import com.meninocoiso.bscm.presentation.ui.components.layout.CoverArt
import com.meninocoiso.bscm.presentation.ui.components.layout.Section
import com.meninocoiso.bscm.presentation.ui.components.preview.PreviewContributors
import com.meninocoiso.bscm.presentation.ui.components.preview.TourPassTrackPreview
import com.meninocoiso.bscm.util.AudioPreviewPlayer
import com.meninocoiso.bscm.util.StringUtils

/**
 * Details screen for one tour pass: cover art, credits, stats and a
 * tracklist grid with audio previews.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TourPassDetailsScreen(
    tourPass: TourPass,
    onReturn: () -> Unit
) {
    var playingUrl by remember { mutableStateOf<String?>(null) }
    val audioPreviewPlayer = remember { AudioPreviewPlayer { playingUrl = it } }

    DisposableEffect(Unit) {
        onDispose { audioPreviewPlayer.stop() }
    }

    val totalMinutesText = pluralStringResource(
        R.plurals.tour_pass_minutes_total,
        (tourPass.charts.sumOf { it.track.duration.toLong() } / 60).toInt(),
        (tourPass.charts.sumOf { it.track.duration.toLong() } / 60).toInt()
    )
    val songsText = pluralStringResource(
        R.plurals.songs_count,
        tourPass.charts.size,
        tourPass.charts.size
    )
    val downloadsText = pluralStringResource(
        R.plurals.downloads_amount,
        tourPass.downloadsSum,
        tourPass.downloadsSum
    )
    val uploadedText = stringResource(
        R.string.uploaded_at,
        StringUtils.toRelativeString(tourPass.updatedAt ?: tourPass.createdAt)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.padding(horizontal = 8.dp),
                navigationIcon = {
                    IconButton(
                        modifier = Modifier.padding(end = 12.dp),
                        onClick = onReturn
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            tint = MaterialTheme.colorScheme.onSurface,
                            contentDescription = stringResource(R.string.return_screen)
                        )
                    }
                },
                title = {
                    Column {
                        Text(tourPass.name, style = MaterialTheme.typography.titleLarge)
                        tourPass.artist?.let {
                            Text(it, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)) {
                CoverArt(
                    url = tourPass.coverUrl ?: "",
                    size = 300.dp,
                    borderRadius = 16.dp
                )
            }

            if (tourPass.contributors.isNotEmpty()) {
                PreviewContributors(tourPass.contributors)
            }

            Section(title = stringResource(R.string.stats)) {
                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    StatListItem(title = totalMinutesText, icon = R.drawable.rounded_hourglass_24)
                    StatListItem(title = songsText, icon = R.drawable.rounded_music_note_24)
                    if (tourPass.downloadsSum > 0) {
                        StatListItem(title = downloadsText, icon = R.drawable.rounded_download_24)
                    }
                    StatListItem(title = uploadedText, icon = R.drawable.rounded_calendar_today_24)
                }
            }

            Section(title = stringResource(R.string.tracklist)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    tourPass.charts.chunked(3).forEach { rowCharts ->
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            rowCharts.forEach { chart ->
                                Column(Modifier.weight(1f)) {
                                    TourPassTrackPreview(
                                        track = chart.track,
                                        isPlaying = playingUrl != null && playingUrl == chart.track.previewUrl,
                                        onTogglePlay = {
                                            chart.track.previewUrl?.let { url ->
                                                audioPreviewPlayer.toggle(url)
                                            }
                                        }
                                    )
                                }
                            }
                            // Keep every row at 3 columns so a partial last row
                            // leaves a blank cell instead of stretching its items.
                            repeat(3 - rowCharts.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}
