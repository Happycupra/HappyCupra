package ch.drivedeck.launcher

import android.app.Application
import android.content.ComponentName
import ch.drivedeck.integration.media.AndroidMediaRepository
import ch.drivedeck.integration.media.DriveDeckNotificationListener
import ch.drivedeck.integration.media.MediaRepositoryOwner
import ch.drivedeck.core.preferences.DataStorePreferencesRepository
import ch.drivedeck.feature.apps.AndroidInstalledAppsRepository

class DriveDeckApplication : Application(), MediaRepositoryOwner {
    val container by lazy(LazyThreadSafetyMode.NONE) { AppContainer(this) }
    override val mediaRepository by lazy(LazyThreadSafetyMode.NONE) {
        AndroidMediaRepository(this, ComponentName(this, DriveDeckNotificationListener::class.java))
    }
}
class AppContainer(application: DriveDeckApplication) {
    val preferences = DataStorePreferencesRepository(application)
    val apps = AndroidInstalledAppsRepository(application)
    val media = application.mediaRepository
}
