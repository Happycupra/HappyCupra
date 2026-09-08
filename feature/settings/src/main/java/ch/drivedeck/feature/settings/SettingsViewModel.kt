package ch.drivedeck.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.drivedeck.core.model.ThemeMode
import ch.drivedeck.core.preferences.PreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: PreferencesRepository) : ViewModel() {
    val preferences = repository.preferences.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ch.drivedeck.core.model.UserPreferences())
    fun setTheme(mode: ThemeMode) { viewModelScope.launch { repository.setThemeMode(mode) } }
}
