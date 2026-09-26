package com.carlauncherc.launcher.media

import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class NowPlayingInfo(
    val title: String = "",
    val artist: String = "",
    val isPlaying: Boolean = false
)

object YMusicMediaBridge {
    const val YMUSIC_PACKAGE = "com.kapp.youtube.final"

    private val _nowPlaying = MutableStateFlow(NowPlayingInfo())
    val nowPlaying = _nowPlaying.asStateFlow()

    private val handler = Handler(Looper.getMainLooper())
    private var controller: MediaController? = null

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
        controller = newController
        newController?.registerCallback(callback, handler)
        publish(newController?.metadata, newController?.playbackState)
    }

    fun detach() {
        controller?.unregisterCallback(callback)
        controller = null
        _nowPlaying.value = NowPlayingInfo()
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
        _nowPlaying.value = NowPlayingInfo(
            title = title,
            artist = artist,
            isPlaying = playbackState?.state == PlaybackState.STATE_PLAYING
        )
    }
}
