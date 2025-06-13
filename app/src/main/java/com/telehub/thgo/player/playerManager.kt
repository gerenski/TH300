package com.telehub.thgo.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.ui.PlayerView

class playerManager(
    private val context: Context,
    private val mac: String,
    private val referer: String
) {
    private val player = ExoPlayer.Builder(context).build()

    fun getPlayerView(): PlayerView {
        return PlayerView(context).apply {
            this.player = player
            useController = true
        }
    }

    fun play(url: String) {
        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setUserAgent("Mozilla/5.0 (QtEmbedded; U; Linux; C)")
            .setDefaultRequestProperties(
                mapOf(
                    "User-Agent" to "Mozilla/5.0 (QtEmbedded; U; Linux; C)",
                    "Referer" to referer,
                    "Cookie" to "mac=$mac; stb_lang=en; timezone=Europe/Sofia",
                    "Accept-Encoding" to "identity"
                )
            )

        val mediaSource = HlsMediaSource.Factory(dataSourceFactory)
            .createMediaSource(MediaItem.fromUri(url))

        player.setMediaSource(mediaSource)
        player.prepare()
        player.play()
    }

    fun release() {
        player.release()
    }
}
