package ch.drivedeck.integration.media

import android.service.notification.NotificationListenerService

class DriveDeckNotificationListener : NotificationListenerService() {
    private val repository get() = (application as MediaRepositoryOwner).mediaRepository
    override fun onListenerConnected() { super.onListenerConnected(); repository.connect() }
    override fun onListenerDisconnected() { repository.disconnect(); super.onListenerDisconnected() }
}

interface MediaRepositoryOwner { val mediaRepository: AndroidMediaRepository }
