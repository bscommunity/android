package com.meninocoiso.bscm.util

import android.media.MediaPlayer
import java.io.IOException

/**
 * Minimal audio preview player used to stream the preview clips of tracklist entries.
 * Only one preview is played at a time; playing a new one stops the previous.
 */
class AudioPreviewPlayer(
    private val onPlayingUrlChange: (String?) -> Unit
) {
    private var mediaPlayer: MediaPlayer? = null
    private var currentUrl: String? = null

    fun toggle(url: String) {
        if (url == currentUrl && mediaPlayer?.isPlaying == true) {
            stop()
            onPlayingUrlChange(null)
            return
        }

        stop()
        currentUrl = url
        onPlayingUrlChange(url)

        val player = MediaPlayer()
        player.setOnPreparedListener(MediaPlayer::start)
        player.setOnCompletionListener {
            reset()
        }
        player.setOnErrorListener { _, _, _ ->
            reset()
            true
        }

        try {
            player.setDataSource(url)
            player.prepareAsync()
            mediaPlayer = player
        } catch (e: IOException) {
            reset()
        } catch (e: SecurityException) {
            reset()
        } catch (e: IllegalArgumentException) {
            reset()
        }
    }

    fun stop() {
        mediaPlayer?.let { player ->
            try {
                player.stop()
            } catch (_: IllegalStateException) {
                // Player was not in a started state
            }
            player.release()
        }
        mediaPlayer = null
        currentUrl = null
    }

    private fun reset() {
        stop()
        onPlayingUrlChange(null)
    }
}
