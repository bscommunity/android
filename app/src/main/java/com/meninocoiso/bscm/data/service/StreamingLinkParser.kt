package com.meninocoiso.bscm.data.service

import android.util.Log
import com.meninocoiso.bscm.domain.enums.StreamingPlatform
import com.meninocoiso.bscm.domain.model.StreamingRef
import javax.inject.Inject

private const val TAG = "StreamingLinkParser"

class StreamingLinkParser @Inject constructor(
    private val urlBuilder: StreamingUrlBuilder
) {

    fun parseLinks(streaming: String?): List<StreamingRef> {
        if (streaming.isNullOrBlank()) return emptyList()

        return try {
            streaming.split("||")
                .filter { it.isNotBlank() }
                .mapNotNull { item ->
                    val parts = item.split("|", limit = 2)
                    if (parts.size == 2) {
                        val platformId = parts[0].toIntOrNull()
                        val path = parts[1]

                        val platform = mapPlatform(platformId)
                        if (platform != null && path.isNotBlank()) {
                            val fullUrl = urlBuilder.buildUrl(platform, path)
                            StreamingRef(platform = platform, url = fullUrl)
                        } else {
                            null
                        }
                    } else {
                        null
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing streaming links: $streaming", e)
            emptyList()
        }
    }

    private fun mapPlatform(platformId: Int?): StreamingPlatform? {
        return when (platformId) {
            0 -> StreamingPlatform.SPOTIFY
            1 -> StreamingPlatform.APPLE_MUSIC
            2 -> StreamingPlatform.YOUTUBE_MUSIC
            3 -> StreamingPlatform.DEEZER
            4 -> StreamingPlatform.TIDAL
            5 -> StreamingPlatform.AMAZON_MUSIC
            6 -> StreamingPlatform.SOUNDCLOUD
            7 -> StreamingPlatform.LAST_FM
            else -> null
        }
    }
}
