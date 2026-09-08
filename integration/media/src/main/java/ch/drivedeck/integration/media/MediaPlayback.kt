package ch.drivedeck.integration.media

import android.graphics.Bitmap

data class MediaPlayback(
    val appName: String = "",
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val artwork: Bitmap? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val available: Boolean = false,
) {
    companion object {
        val Unavailable = MediaPlayback()
        val Preview = MediaPlayback(
            appName = "Demo Player", title = "Midnight Drive", artist = "Demo Artist",
            album = "Night Roads", isPlaying = true, positionMs = 84_000, durationMs = 224_000,
            available = true,
        )
    }
}
