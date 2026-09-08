package ch.drivedeck.launcher

import android.app.Application
import ch.drivedeck.core.preferences.DataStorePreferencesRepository
import ch.drivedeck.feature.apps.AndroidInstalledAppsRepository

class DriveDeckApplication : Application() {
    val container by lazy(LazyThreadSafetyMode.NONE) { AppContainer(this) }
}
class AppContainer(application: Application) {
    val preferences = DataStorePreferencesRepository(application)
    val apps = AndroidInstalledAppsRepository(application)
}
