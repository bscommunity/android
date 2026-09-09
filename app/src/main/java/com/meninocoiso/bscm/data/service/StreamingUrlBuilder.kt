package com.meninocoiso.bscm.data.service

import com.meninocoiso.bscm.domain.enums.StreamingPlatform
import javax.inject.Inject

/**
 * Service for constructing streaming platform URLs from paths
 */
class StreamingUrlBuilder @Inject constructor() {

    /**
     * Construct full URL from platform and path.
     * If path already contains protocol or domain, use as-is, otherwise prepend base URL.
     */
    fun buildUrl(platform: StreamingPlatform, path: String): String {
        // If path already has protocol or looks like a full URL, use as-is
        if (path.startsWith("http://") || path.startsWith("https://") || path.contains("://")) {
            return path
        }

        // If path contains a domain (has dots and no slash before first dot), add protocol
        if (path.contains(".") && !path.substringBefore(".").contains("/")) {
            return "https://$path"
        }

        // Otherwise, construct from base URL
        val baseUrl = getBaseUrl(platform)
        return "$baseUrl/$path"
    }

    private fun getBaseUrl(platform: StreamingPlatform): String {
        return when (platform) {
            StreamingPlatform.SPOTIFY -> "https://open.spotify.com"
            StreamingPlatform.APPLE_MUSIC -> "https://music.apple.com"
            StreamingPlatform.YOUTUBE_MUSIC -> "https://music.youtube.com"
            StreamingPlatform.DEEZER -> "https://www.deezer.com"
            StreamingPlatform.TIDAL -> "https://listen.tidal.com"
            StreamingPlatform.AMAZON_MUSIC -> "https://music.amazon.com"
            StreamingPlatform.SOUNDCLOUD -> "https://soundcloud.com"
            StreamingPlatform.LAST_FM -> "https://www.last.fm"
            else -> ""
        }
    }
}

