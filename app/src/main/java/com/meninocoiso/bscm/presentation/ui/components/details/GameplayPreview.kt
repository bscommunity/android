package com.meninocoiso.bscm.presentation.ui.components.details

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.net.toUri
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.coil3.CoilImage
import com.skydoves.landscapist.placeholder.shimmer.Shimmer
import com.skydoves.landscapist.placeholder.shimmer.ShimmerContainer

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun GameplayPreview(
    videoId: String,
    context: Context,
    modifier: Modifier = Modifier,
) {
    var isLoading by remember { mutableStateOf(true) }

    // --- 1️⃣ Keep one WebView instance per Composable lifecycle ---
    val webView = remember {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            //  HTML with origin + referrer policy
            val customHtml = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <meta name="referrer" content="strict-origin-when-cross-origin">
            <style>
                html, body {
                    margin: 0; padding: 0;
                    width: 100%; height: 100%;
                    overflow: hidden;
                    background-color: transparent;
                }
                #player {
                    position: absolute;
                    left: -50%;
                    width: 200%;
                    height: 100%;
                    border: none;
                }
            </style>
        </head>
        <body>
            <div id="player"></div>
            <script>
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
                            'playlist': '$videoId',
                            'origin': 'https://www.youtube-nocookie.com' // ✅ required
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
                    if (event.data === YT.PlayerState.ENDED || event.data === YT.PlayerState.PAUSED) {
                        player.playVideo();
                    }
                }
            </script>
        </body>
        </html>
    """.trimIndent()
            
            // WebView config tuned for YouTube embeds
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                mediaPlaybackRequiresUserGesture = false
                allowFileAccess = false
                allowContentAccess = false
                javaScriptCanOpenWindowsAutomatically = false
                setSupportMultipleWindows(false)
                cacheMode = WebSettings.LOAD_DEFAULT
                setLayerType(WebView.LAYER_TYPE_HARDWARE, null)
                userAgentString = "$userAgentString Chrome/123"
            }

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    isLoading = false
                }

                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    // block navigation out of the embed
                    return true
                }
            }

            webChromeClient = WebChromeClient()

            // Load the player
            loadDataWithBaseURL(
                "https://www.youtube-nocookie.com",
                customHtml,
                "text/html",
                "UTF-8",
                null
            )
        }
    }

    // --- Cleanup on Compose disposal ---
    DisposableEffect(Unit) {
        onDispose {
            webView.apply {
                stopLoading()
                clearHistory()
                removeAllViews()
                destroy()
            }
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { webView },
            modifier = Modifier.fillMaxSize()
        )

        // ✅ Loading shimmer / thumbnail overlay
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest),
                contentAlignment = Alignment.Center
            ) {
                ShimmerContainer(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(2f),
                    shimmer = Shimmer.Resonate(
                        baseColor = Color.Transparent,
                        highlightColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                )
                GameplayPreviewThumbnail(videoId = videoId)
            }
        }

        // ✅ Clickable overlay to open YouTube app
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable {
                    context.startActivity(openLinkIntent(videoId))
                }
        )
    }
}

// --- 8️⃣ Helpers ---
fun openLinkIntent(videoId: String): Intent =
    Intent(Intent.ACTION_VIEW, "https://youtu.be/$videoId".toUri())

@Composable
fun GameplayPreviewThumbnail(videoId: String, modifier: Modifier = Modifier) {
    CoilImage(
        imageModel = { "https://img.youtube.com/vi/$videoId/maxresdefault.jpg" },
        modifier = modifier
            .zIndex(1f)
            .fillMaxSize(),
        imageOptions = ImageOptions(
            contentScale = ContentScale.Crop,
            alignment = Alignment.Center,
        ),
    )
}