package com.carlauncherc.launcher.media

import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class NowPlayingInfo(
    val title: String = "",
    val artist: String = "",
    val isPlaying: Boolean = false,
    val artwork: Bitmap? = null,
    val durationMs: Long = 0L,
    val positionMs: Long = 0L
)

object YMusicMediaBridge {
    const val YMUSIC_PACKAGE = "com.kapp.youtube.final"

    private val _nowPlaying = MutableStateFlow(NowPlayingInfo())
    val nowPlaying = _nowPlaying.asStateFlow()

    private val handler = Handler(Looper.getMainLooper())
    private var controller: MediaController? = null

    private val progressTicker = object : Runnable {
        override fun run() {
            val c = controller ?: return
            publish(c.metadata, c.playbackState)
            handler.postDelayed(this, 1_000L)
        }
    }

    private val callback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            publish(metadata, controller?.playbackState)
        }

        override fun onPlaybackStateChanged(state: PlaybackState?) {
            publish(controller?.metadata, state)
        }

        override fun onSessionDestroyed() {
            detach()
        }
    }

    fun attach(newController: MediaController?) {
        if (controller?.sessionToken == newController?.sessionToken) {
            publish(newController?.metadata, newController?.playbackState)
            return
        }

        controller?.unregisterCallback(callback)
        handler.removeCallbacks(progressTicker)
        controller = newController
        newController?.registerCallback(callback, handler)
        publish(newController?.metadata, newController?.playbackState)
        if (newController != null) handler.post(progressTicker)
    }

    fun detach() {
        controller?.unregisterCallback(callback)
        handler.removeCallbacks(progressTicker)
        controller = null
        _nowPlaying.value = NowPlayingInfo()
    }

    fun play(): Boolean {
        val c = controller ?: return false
        c.transportControls.play()
        return true
    }

    fun previous(): Boolean {
        val c = controller ?: return false
        c.transportControls.skipToPrevious()
        return true
    }

    fun next(): Boolean {
        val c = controller ?: return false
        c.transportControls.skipToNext()
        return true
    }

    fun togglePlayPause(): Boolean {
        val c = controller ?: return false
        val state = c.playbackState?.state
        if (state == PlaybackState.STATE_PLAYING ||
            state == PlaybackState.STATE_BUFFERING ||
            state == PlaybackState.STATE_CONNECTING
        ) {
            c.transportControls.pause()
        } else {
            c.transportControls.play()
        }
        return true
    }

    private fun publish(metadata: MediaMetadata?, playbackState: PlaybackState?) {
        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            ?: ""
        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)
            ?: ""
        val artwork = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)

        val duration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
        val playing = playbackState?.state == PlaybackState.STATE_PLAYING
        val basePosition = playbackState?.position?.coerceAtLeast(0L) ?: 0L
        val updatedAt = playbackState?.lastPositionUpdateTime ?: 0L
        val speed = playbackState?.playbackSpeed ?: 1f
        val position = if (playing && updatedAt > 0L) {
            val elapsed = (SystemClock.elapsedRealtime() - updatedAt).coerceAtLeast(0L)
            (basePosition + (elapsed * speed).toLong()).coerceAtLeast(0L)
        } else {
            basePosition
        }.let { if (duration > 0L) it.coerceAtMost(duration) else it }

        _nowPlaying.value = NowPlayingInfo(
            title = title,
            artist = artist,
            isPlaying = playing,
            artwork = artwork,
            durationMs = duration,
            positionMs = position
        )
    }
}
