package ch.drivedeck.integration.media

import kotlinx.coroutines.flow.StateFlow

interface MediaRepository {
    val playback: StateFlow<MediaPlayback>
    fun refresh()
    fun playPause()
    fun previous()
    fun next()
}
