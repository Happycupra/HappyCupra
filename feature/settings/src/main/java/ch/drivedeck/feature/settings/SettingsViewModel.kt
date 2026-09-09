package ch.drivedeck.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.drivedeck.core.model.ThemeMode
import ch.drivedeck.core.model.QuickAction
import ch.drivedeck.core.preferences.PreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: PreferencesRepository) : ViewModel() {
    val preferences = repository.preferences.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ch.drivedeck.core.model.UserPreferences())
    fun setTheme(mode: ThemeMode) { viewModelScope.launch { repository.setThemeMode(mode) } }
    fun setBrightness(value: Float) { viewModelScope.launch { repository.setUiBrightness(value) } }
    fun replaceQuickAction(index: Int, action: QuickAction) {
        val current = preferences.value.quickActions.toMutableList()
        if (index !in current.indices || action in current) return
        current[index] = action
        viewModelScope.launch { repository.setQuickActions(current) }
    }
    fun cycleQuickAction(index: Int) {
        val current = preferences.value.quickActions
        if (index !in current.indices) return
        val available = QuickAction.entries.filterNot { it in current }
        val next = available.firstOrNull { it.ordinal > current[index].ordinal } ?: available.firstOrNull() ?: return
        replaceQuickAction(index, next)
    }
    fun moveQuickAction(index: Int, direction: Int) {
        val current = preferences.value.quickActions.toMutableList()
        val target = index + direction
        if (index !in current.indices || target !in current.indices) return
        val moved = current.removeAt(index)
        current.add(target, moved)
        viewModelScope.launch { repository.setQuickActions(current) }
    }
}
