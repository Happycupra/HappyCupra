package ch.drivedeck.core.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import ch.drivedeck.core.model.ThemeMode
import ch.drivedeck.core.model.UserPreferences
import ch.drivedeck.core.model.DashboardItem
import ch.drivedeck.core.model.DashboardLayoutCodec
import ch.drivedeck.core.model.QuickAction
import ch.drivedeck.core.model.QuickActionsCodec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

interface PreferencesRepository {
    val preferences: Flow<UserPreferences>
    suspend fun toggleFavorite(packageName: String)
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setEditMode(enabled: Boolean)
    suspend fun setDashboardItems(items: List<DashboardItem>)
    suspend fun setQuickActions(actions: List<QuickAction>)
    suspend fun setUiBrightness(brightness: Float)
}

private val Context.dataStore by preferencesDataStore("drive_deck_preferences")

class DataStorePreferencesRepository(private val context: Context) : PreferencesRepository {
    override val preferences = context.dataStore.data
        .catch { error -> if (error is IOException) emit(androidx.datastore.preferences.core.emptyPreferences()) else throw error }
        .map { values ->
            UserPreferences(
                favoritePackages = values[FAVORITES].orEmpty(),
                themeMode = values[THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.DARK,
                editModeEnabled = values[EDIT_MODE] ?: false,
                dashboardItems = DashboardLayoutCodec.decode(values[DASHBOARD_ITEMS]),
                quickActions = QuickActionsCodec.decode(values[QUICK_ACTIONS]),
                uiBrightness = values[UI_BRIGHTNESS]?.coerceIn(MIN_BRIGHTNESS, 1f) ?: 1f,
            )
        }

    override suspend fun toggleFavorite(packageName: String) {
        context.dataStore.edit { values ->
            val updated = values[FAVORITES].orEmpty().toMutableSet()
            if (!updated.add(packageName)) updated.remove(packageName)
            values[FAVORITES] = updated
        }
    }

    override suspend fun setThemeMode(mode: ThemeMode) { context.dataStore.edit { it[THEME] = mode.name } }
    override suspend fun setEditMode(enabled: Boolean) { context.dataStore.edit { it[EDIT_MODE] = enabled } }
    override suspend fun setDashboardItems(items: List<DashboardItem>) {
        context.dataStore.edit { it[DASHBOARD_ITEMS] = DashboardLayoutCodec.encode(items) }
    }
    override suspend fun setQuickActions(actions: List<QuickAction>) {
        context.dataStore.edit { it[QUICK_ACTIONS] = QuickActionsCodec.encode(actions) }
    }
    override suspend fun setUiBrightness(brightness: Float) {
        context.dataStore.edit { it[UI_BRIGHTNESS] = brightness.coerceIn(MIN_BRIGHTNESS, 1f) }
    }

    private companion object {
        val FAVORITES = stringSetPreferencesKey("favorite_packages")
        val THEME = stringPreferencesKey("theme_mode")
        val EDIT_MODE = booleanPreferencesKey("dashboard_edit_mode")
        val DASHBOARD_ITEMS = stringPreferencesKey("dashboard_items")
        val QUICK_ACTIONS = stringPreferencesKey("quick_actions")
        val UI_BRIGHTNESS = floatPreferencesKey("ui_brightness")
        const val MIN_BRIGHTNESS = 0.2f
    }
}
