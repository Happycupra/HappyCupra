package ch.drivedeck.integration.media

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AndroidMediaRepository(
    private val context: Context,
    private val listenerComponent: ComponentName,
) : MediaRepository {
    private val manager = context.getSystemService(MediaSessionManager::class.java)
    private val mutablePlayback = MutableStateFlow(MediaPlayback.Unavailable)
    override val playback: StateFlow<MediaPlayback> = mutablePlayback
    private var controller: MediaController? = null
    private val callback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) = publish()
        override fun onPlaybackStateChanged(state: PlaybackState?) = publish()
        override fun onSessionDestroyed() { refresh() }
    }
    private val sessionsChanged = MediaSessionManager.OnActiveSessionsChangedListener { selectController(it.orEmpty()) }

    fun connect() {
        runCatching { manager.addOnActiveSessionsChangedListener(sessionsChanged, listenerComponent) }
        refresh()
    }

    fun disconnect() {
        runCatching { manager.removeOnActiveSessionsChangedListener(sessionsChanged) }
        controller?.unregisterCallback(callback)
        controller = null
    }

    override fun refresh() {
        val controllers = runCatching { manager.getActiveSessions(listenerComponent) }.getOrDefault(emptyList())
        selectController(controllers)
    }

    private fun selectController(controllers: List<MediaController>) {
        val selected = controllers.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: controllers.firstOrNull()
        if (selected?.sessionToken == controller?.sessionToken) { publish(); return }
        controller?.unregisterCallback(callback)
        controller = selected
        selected?.registerCallback(callback)
        publish()
    }

    private fun publish() {
        val active = controller ?: run { mutablePlayback.value = MediaPlayback.Unavailable; return }
        val metadata = active.metadata
        val state = active.playbackState
        val applicationLabel = runCatching {
            val info = context.packageManager.getApplicationInfo(active.packageName, 0)
            context.packageManager.getApplicationLabel(info).toString()
        }.getOrDefault(active.packageName)
        mutablePlayback.value = MediaPlayback(
            appName = applicationLabel,
            title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE).orEmpty(),
            artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST).orEmpty(),
            album = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM).orEmpty(),
            artwork = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART),
            isPlaying = state?.state == PlaybackState.STATE_PLAYING,
            positionMs = state?.position?.coerceAtLeast(0) ?: 0,
            durationMs = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION)?.coerceAtLeast(0) ?: 0,
            available = true,
        )
    }

    override fun playPause() {
        controller?.transportControls?.let { if (playback.value.isPlaying) it.pause() else it.play() }
    }
    override fun previous() { controller?.transportControls?.skipToPrevious() }
    override fun next() { controller?.transportControls?.skipToNext() }
}
