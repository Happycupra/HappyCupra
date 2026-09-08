package ch.drivedeck.feature.apps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.drivedeck.core.model.LaunchableApp
import ch.drivedeck.core.preferences.PreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AppsUiState(val apps: List<LaunchableApp> = emptyList(), val favorites: Set<String> = emptySet(), val query: String = "", val loading: Boolean = true)

class AppsViewModel(private val appsRepository: InstalledAppsRepository, private val preferencesRepository: PreferencesRepository) : ViewModel() {
    private val apps = MutableStateFlow<List<LaunchableApp>>(emptyList())
    private val query = MutableStateFlow("")
    val state = combine(apps, query, preferencesRepository.preferences) { all, term, prefs ->
        AppsUiState(all.filter { it.label.contains(term, ignoreCase = true) }, prefs.favoritePackages, term, false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppsUiState())
    init { viewModelScope.launch { apps.value = appsRepository.installedApps() } }
    fun search(value: String) { query.value = value }
    fun toggleFavorite(packageName: String) { viewModelScope.launch { preferencesRepository.toggleFavorite(packageName) } }
    fun launch(app: LaunchableApp) = appsRepository.launch(app)
}
