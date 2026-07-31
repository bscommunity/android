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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.domain.model.Track
import com.meninocoiso.bscm.presentation.ui.components.layout.CoverArt

/**
 * Single tracklist entry of a tour pass: square cover art with a floating
 * play/pause button (bottom-left) and the track title/artist below.
 */
@Composable
fun TourPassTrackPreview(
    track: Track,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit
) {
    val hasPreview = track.previewUrl != null

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(Color.Red)
        ) {
            CoverArt(
                url = track.coverUrl ?: "",
                modifier = Modifier.fillMaxSize(),
                borderRadius = 0.dp,
                size = 117.dp,
            )
            if (hasPreview) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .border(1.5.dp, Color.White, CircleShape)
                        .clickable(onClick = onTogglePlay),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(
                            if (isPlaying) R.drawable.pause_24px
                            else R.drawable.play_arrow_24px
                        ),
                        contentDescription = stringResource(
                            if (isPlaying) R.string.stop_preview else R.string.play_preview
                        ),
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
        Text(
            text = track.title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
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
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
