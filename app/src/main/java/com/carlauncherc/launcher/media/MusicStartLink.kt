package com.carlauncherc.launcher.media

import java.net.URI

/** One safe, canonical web link received from Android Share. No arbitrary URI schemes. */
data class MusicStartLink(val url: String, val kind: Kind) {
    enum class Kind { TRACK, PLAYLIST }

    companion object {
        private val urlPattern = Regex("""https?://[^\s<>"']+""", RegexOption.IGNORE_CASE)
        private val hasVideoQuery = Regex("""(?:^|&)v=[^&]+""", RegexOption.IGNORE_CASE)
        private val hasPlaylistQuery = Regex("""(?:^|&)list=[^&]+""", RegexOption.IGNORE_CASE)

        fun parse(text: String?): MusicStartLink? {
            val url = urlPattern.find(text.orEmpty())?.value
                ?.trimEnd('.', ',', ';', ')', ']', '!', ':') ?: return null
            val uri = runCatching { URI(url) }.getOrNull() ?: return null
            if (uri.scheme?.lowercase() !in listOf("http", "https")) return null
            val host = uri.host?.lowercase() ?: return null
            val youtube = host == "youtube.com" || host.endsWith(".youtube.com")
            val shortLink = host == "youtu.be"
            val ymusic = host == "ymusic.io" || host == "www.ymusic.io" ||
                host == "ymusicapp.com" || host == "www.ymusicapp.com"
            if (!youtube && !shortLink && !ymusic) return null

            val path = uri.path.orEmpty()
            val query = uri.rawQuery.orEmpty()
            val track = (shortLink && path.length > 1) ||
                ((youtube || ymusic) && (
                    (path == "/watch" && hasVideoQuery.containsMatchIn(query)) ||
                    path.startsWith("/shorts/") || path.startsWith("/live/")
                ))
            val playlist = (youtube || ymusic) &&
                path == "/playlist" && hasPlaylistQuery.containsMatchIn(query)
            return when {
                track -> MusicStartLink(url, Kind.TRACK)
                playlist -> MusicStartLink(url, Kind.PLAYLIST)
                else -> null
            }
        }

        /** The first saved-link boot sets a flag; subsequent boots use YMusic's own resume. */
        fun shouldOpenLink(mode: String, alreadyOpened: Boolean): Boolean = when (mode) {
            "always" -> true
            "once" -> !alreadyOpened
            else -> false
        }
    }
}
