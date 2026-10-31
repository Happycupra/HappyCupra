package com.carlauncherc.launcher.media

import android.content.ComponentName
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.service.notification.NotificationListenerService
import com.carlauncherc.launcher.core.Prefs
import com.carlauncherc.launcher.util.IntentUtil

class YMusicNotificationListener : NotificationListenerService() {

    private lateinit var sessionManager: MediaSessionManager

    private val sessionListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        attachYMusic(controllers.orEmpty())
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        sessionManager = getSystemService(MediaSessionManager::class.java)
        val component = ComponentName(this, YMusicNotificationListener::class.java)
        sessionManager.addOnActiveSessionsChangedListener(sessionListener, component)
        refresh(component)
    }

    override fun onListenerDisconnected() {
        if (::sessionManager.isInitialized) {
            sessionManager.removeOnActiveSessionsChangedListener(sessionListener)
        }
        YMusicMediaBridge.detach()
        super.onListenerDisconnected()
    }

    private fun refresh(component: ComponentName) {
        val sessions = runCatching {
            sessionManager.getActiveSessions(component)
        }.getOrDefault(emptyList())
        attachYMusic(sessions)
    }

    private fun attachYMusic(controllers: List<MediaController>) {
        val selectedPackage = Prefs.musicPackage?.let(IntentUtil::packageOf)
        val activePackage = YMusicPackages.selectSessionPackage(
            controllers.map { it.packageName }, selectedPackage
        )
        YMusicMediaBridge.attach(controllers.firstOrNull { it.packageName == activePackage })
    }
}
