package ch.drivedeck.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.drivedeck.core.model.DemoDriveData
import ch.drivedeck.core.preferences.PreferencesRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDateTime

data class HomeUiState(val now: LocalDateTime = LocalDateTime.now(), val demo: DemoDriveData = DemoDriveData.Preview, val editMode: Boolean = false)
class HomeViewModel(private val preferences: PreferencesRepository) : ViewModel() {
    private val now = MutableStateFlow(LocalDateTime.now())
    val state = combine(now, preferences.preferences) { time, prefs -> HomeUiState(time, editMode = prefs.editModeEnabled) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())
    init { viewModelScope.launch { while (isActive) { now.value = LocalDateTime.now(); delay(30_000) } } }
    fun toggleEditMode() { viewModelScope.launch { preferences.setEditMode(!state.value.editMode) } }
}
