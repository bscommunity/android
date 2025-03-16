package com.meninocoiso.beatstarcommunity.presentation.ui.components.details

import android.annotation.SuppressLint
import android.content.Intent
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.net.toUri
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.coil.CoilImage
import com.skydoves.landscapist.placeholder.shimmer.Shimmer
import com.skydoves.landscapist.placeholder.shimmer.ShimmerContainer

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun GameplayPreview(
    videoId: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }

    val customHtml = """
                        <!DOCTYPE html>
                        <html>
                        <head>
                            <meta name="viewport" content="width=device-width, initial-scale=1.0">
                            <style>
                                body, html { margin: 0; padding: 0; width: 100%; height: 100%; overflow: hidden; background-color: transparent; }
                                #player { position: absolute; width: 100%; height: 100%; border: none }
                            </style>
                        </head>
                        <body>
                            <div id="player"></div>
                            <script>
                                // Create YouTube player once API is ready
                                var tag = document.createElement('script');
                                tag.src = "https://www.youtube.com/iframe_api";
                                var firstScriptTag = document.getElementsByTagName('script')[0];
                                firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);
                                
                                var player;
                                function onYouTubeIframeAPIReady() {
                                    player = new YT.Player('player', {
                                        videoId: '$videoId',
                                        playerVars: {
                                            'autoplay': 1,
                                            'controls': 0,
                                            'showinfo': 0,
                                            'modestbranding': 1,
                                            'loop': 1,
                                            'rel': 0,
                                            'fs': 0,
                                            'playsinline': 1,
                                            'mute': 1,
                                            'disablekb': 1,
                                            'playlist': '$videoId' // Required for looping
                                        },
                                        events: {
                                            'onReady': onPlayerReady,
                                            'onStateChange': onPlayerStateChange
                                        }
                                    });
                                }
                                
                                function onPlayerReady(event) {
                                    event.target.playVideo();
                                }
                                
                                function onPlayerStateChange(event) {
                                    // If video ends, restart it (backup for loop)
                                    // If video pauses for any reason, resume it
                                    if (event.data === YT.PlayerState.ENDED || event.data === YT.PlayerState.PAUSED) {
                                        player.playVideo();
                                    }
                                }
                            </script>
                        </body>
                        </html>
                    """.trimIndent()

    Box(modifier = modifier) {
        // WebView to load and play YouTube video
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )

                    // Configure WebView settings for optimal performance
                    settings.apply {
                        javaScriptEnabled = true
                        mediaPlaybackRequiresUserGesture = false
                        domStorageEnabled = true

                        // Optimize for video playback
                        cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
                        layoutAlgorithm = WebSettings.LayoutAlgorithm.NORMAL

                        // Hardware acceleration
                        setLayerType(WebView.LAYER_TYPE_HARDWARE, null)
                    }

                    // Custom WebViewClient to track loading state
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView,
                            request: WebResourceRequest
                        ): Boolean {
                            // Prevent navigation outside the player
                            return true
                        }

                        override fun onPageFinished(view: WebView, url: String) {
                            super.onPageFinished(view, url)
                            isLoading = false
                        }
                    }

                    // Set WebChromeClient to capture events
                    webChromeClient = WebChromeClient()

                    // Load the custom HTML with embedded YouTube player
                    loadDataWithBaseURL(
                        "https://www.youtube.com",
                        customHtml,
                        "text/html",
                        "UTF-8",
                        null
                    )
                }
            }
        )

        // Loading indicator
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
            ) {
                /*ShimmerContainer(
                    modifier = Modifier.fillMaxSize(),
                    shimmer = Shimmer.Resonate(
                        baseColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        highlightColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                )*/
                ShimmerContainer(
                    modifier = Modifier.fillMaxSize().zIndex(2f),
                    shimmer = Shimmer.Resonate(
                        baseColor = Color.Transparent,
                        highlightColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                CoilImage(
                    imageModel = { "https://img.youtube.com/vi/$videoId/0.jpg" },
                    modifier = Modifier
                        .zIndex(1f)
                        .matchParentSize(),
                    imageOptions = ImageOptions(
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.Center,
                    ),
                )
            }
        }

        // Clickable overlay to open YouTube app
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable {
                    val intent = Intent(Intent.ACTION_VIEW, "https://youtu.be/$videoId".toUri())
                    context.startActivity(intent)
                }
        )

        // Clean up WebView resources when component is disposed
        DisposableEffect(Unit) {
            onDispose {
                // Nothing specific to clean up here as Compose will handle the WebView lifecycle
            }
        }
    }
}