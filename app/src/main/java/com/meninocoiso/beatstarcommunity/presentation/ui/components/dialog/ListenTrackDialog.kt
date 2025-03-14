package com.meninocoiso.beatstarcommunity.presentation.ui.components.dialog

import android.content.Intent
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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.meninocoiso.beatstarcommunity.R
import com.meninocoiso.beatstarcommunity.domain.model.StreamingLink
import com.meninocoiso.beatstarcommunity.presentation.ui.components.RadioGroupUI

enum class StreamingPlatform {
    SPOTIFY,
    APPLE_MUSIC,
    YOUTUBE_MUSIC,
    DEEZER,
    TIDAL,
    // SOUNDCLOUD,
}

val StreamingPlatformStrings = mapOf<StreamingPlatform, String>(
    StreamingPlatform.SPOTIFY to "Spotify",
    StreamingPlatform.APPLE_MUSIC to "Apple Music",
    StreamingPlatform.YOUTUBE_MUSIC to "Youtube Music",
    StreamingPlatform.DEEZER to "Deezer",
    StreamingPlatform.TIDAL to "Tidal",
    // StreamingPlatform.SOUNDCLOUD to "SoundCloud"
)

val StreamingPlatformIcons = mapOf<StreamingPlatform, Int>(
    StreamingPlatform.SPOTIFY to R.drawable.platform_spotify,
    StreamingPlatform.APPLE_MUSIC to R.drawable.platform_apple_music,
    StreamingPlatform.YOUTUBE_MUSIC to R.drawable.platform_youtube_music,
    StreamingPlatform.DEEZER to R.drawable.platform_deezer,
    StreamingPlatform.TIDAL to R.drawable.platform_tidal,
    // StreamingPlatform.SOUNDCLOUD to "soundcloud"
)

@Composable
fun ListenTrackDialog(
    isOpened: MutableState<Boolean>,
    streamingLinks: List<StreamingLink>
) {
    var platform by remember { mutableStateOf<StreamingPlatform?>(null) }

    val context = LocalContext.current

    val platformsIcons = StreamingPlatform.entries
        .filter { it ->
            StreamingPlatformStrings[it] in streamingLinks.map { it.platform }
        }
        .map {
            StreamingPlatformIcons[it]
        }

    when {
        isOpened.value -> {
            AlertDialog(
                onDismissRequest = { isOpened.value = false },
                title = {
                    Text(text = "Support the artist")
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = "Check out the full song from this chart on your favorite streaming service and support the artist work")
                        RadioGroupUI(
                            initialSelected = "",
                            radioOptions = StreamingPlatform.entries
                                .filter { it ->
                                    StreamingPlatformStrings[it] in streamingLinks.map { it.platform }
                                }
                                .map {
                                    StreamingPlatformStrings[it]!!
                                },
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
                            onOptionSelected = { index, name ->
                                platform = StreamingPlatformStrings.entries.find { it.value == name }?.key
                                println("Selected platform: ${StreamingPlatformStrings[platform]}")
                            }
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        isOpened.value = false
                        platform = null
                    }) {
                        Text(text = "Cancel")
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            isOpened.value = false

                            if (platform == null) {

                                platform = null
                                return@Button
                            }

                            val url = streamingLinks.find { it.platform == StreamingPlatformStrings[platform] }?.url

                            println("String: ${StreamingPlatformStrings[platform]}")
                            println("platform: $platform")

                            if (url == null) {

                                platform = null
                                return@Button
                            }

                            // Open the link
                            println("Opening link: $url")
                            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                            context.startActivity(intent)

                            platform = null
                        },
                        enabled = platform != null
                    ) {
                        Text(text = "Listen")
                    }
                }
            )
        }
    }
}