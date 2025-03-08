package com.meninocoiso.beatstarcommunity.presentation.ui.components.details

import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

val iFramePlayerOptions = IFramePlayerOptions.Builder()
    .controls(0)
    .ivLoadPolicy(0)
    .ccLoadPolicy(0)
    .autoplay(0)
    .mute(1)
    .build()

@Composable
fun YoutubeVideoPlayer(
    videoId: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // The YouTubePlayerView is wrapped inside a Box with an overlay to capture taps.
    Box {
        AndroidView(
            modifier = modifier,
            factory = { ctx ->
            val youTubePlayerView = YouTubePlayerView(ctx)
                .apply {
                    enableAutomaticInitialization = false

                    // Disable direct touch interactions
                    isClickable = false
                    isFocusable = false

                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )

                    initialize(
                        object : AbstractYouTubePlayerListener() {
                            override fun onReady(youTubePlayer: YouTubePlayer) {
                                // Start playing the video
                                youTubePlayer.loadVideo(videoId, 0f)
                            }
                        },
                        iFramePlayerOptions
                    )
                }

            youTubePlayerView
        })
        // Transparent overlay that intercepts taps.
        Box(modifier = Modifier
            .matchParentSize()
            .clickable {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(
                    "https://youtu.be/$videoId"
                ))
                context.startActivity(intent)
            }
        )
    }
}
