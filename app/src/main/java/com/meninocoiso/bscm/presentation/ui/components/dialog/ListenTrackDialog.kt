package com.meninocoiso.bscm.presentation.ui.components.dialog

import android.content.Intent
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.domain.enums.StreamingPlatform
import com.meninocoiso.bscm.domain.model.StreamingLink
import com.meninocoiso.bscm.presentation.ui.components.RadioGroupUI

val StreamingPlatformStrings = mapOf(
    StreamingPlatform.SPOTIFY to "Spotify",
    StreamingPlatform.APPLE_MUSIC to "Apple Music",
    StreamingPlatform.YOUTUBE_MUSIC to "Youtube Music",
    StreamingPlatform.DEEZER to "Deezer",
    StreamingPlatform.TIDAL to "Tidal",
    StreamingPlatform.SOUNDCLOUD to "SoundCloud",
    StreamingPlatform.AMAZON_MUSIC to "Amazon Music"
)

val StreamingPlatformIcons = mapOf(
    StreamingPlatform.SPOTIFY to R.drawable.platform_spotify,
    StreamingPlatform.APPLE_MUSIC to R.drawable.platform_apple_music,
    StreamingPlatform.YOUTUBE_MUSIC to R.drawable.platform_youtube_music,
    StreamingPlatform.DEEZER to R.drawable.platform_deezer,
    StreamingPlatform.TIDAL to R.drawable.platform_tidal,
    StreamingPlatform.AMAZON_MUSIC to R.drawable.platform_amazonmusic,
    StreamingPlatform.SOUNDCLOUD to R.drawable.platform_soundcloud
)

fun getAvailablePlatforms(
    streamingLinks: List<StreamingLink>
): List<Triple<StreamingPlatform, String, Int?>> {
    return streamingLinks.mapNotNull { link ->
        val name = StreamingPlatformStrings[link.platform]
        val icon = StreamingPlatformIcons[link.platform]
        if (name != null) Triple(link.platform, name, icon) else null
    }
}

private const val TAG = "ListenTrackDialog"

@Composable
fun ListenTrackDialog(
    onDismiss: () -> Unit,
    streamingLinks: List<StreamingLink>
) {
    var platform by remember { mutableStateOf<StreamingPlatform?>(null) }

    val context = LocalContext.current

    val availablePlatforms = getAvailablePlatforms(streamingLinks)
    val platformsNames = availablePlatforms.map { it.second }
    val platformsIcons = availablePlatforms.map { it.third }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Support the artist")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "Check out the full song from this chart on your favorite streaming service and support the artist work")
                if (platformsNames.isNotEmpty()) {
                    RadioGroupUI(
                        initialSelected = "",
                        radioOptions = platformsNames,
                        trailingElements = platformsIcons.map {
                            {
                                it?.let { icon ->
                                    Image(
                                        painter = painterResource(id = icon),
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        },
                        onOptionSelected = { _, name ->
                            platform = availablePlatforms.find { it.second == name }?.first
                            Log.d(TAG, "Selected platform: ${StreamingPlatformStrings[platform]}")
                        }
                    )
                } else {
                    Text(text = "No streaming links available")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = {
                onDismiss()
                platform = null
            }) {
                Text(text = "Cancel")
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onDismiss()

                    platform?.let { selectedPlatform ->
                        val url = streamingLinks.find {
                            it.platform == selectedPlatform
                        }?.url

                        Log.d(TAG, "String: ${StreamingPlatformStrings[selectedPlatform]}")
                        Log.d(TAG, "Platform: $selectedPlatform")

                        url?.let {
                            Log.d(TAG, "Opening link: $it")
                            val intent = Intent(Intent.ACTION_VIEW, it.toUri())
                            context.startActivity(intent)
                        }
                    }

                    platform = null
                },
                enabled = platform != null
            ) {
                Text(text = "Listen")
            }
        }
    )
}