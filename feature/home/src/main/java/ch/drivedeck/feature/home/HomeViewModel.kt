package ch.drivedeck.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.drivedeck.core.model.DemoDriveData
import ch.drivedeck.core.preferences.PreferencesRepository
import ch.drivedeck.integration.media.MediaPlayback
import ch.drivedeck.integration.media.MediaRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDateTime

data class HomeUiState(val now: LocalDateTime = LocalDateTime.now(), val demo: DemoDriveData = DemoDriveData.Preview, val media: MediaPlayback = MediaPlayback.Unavailable, val editMode: Boolean = false)
class HomeViewModel(private val preferences: PreferencesRepository, private val mediaRepository: MediaRepository) : ViewModel() {
    private val now = MutableStateFlow(LocalDateTime.now())
    val state = combine(now, preferences.preferences, mediaRepository.playback) { time, prefs, media -> HomeUiState(time, media = media, editMode = prefs.editModeEnabled) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())
    init { viewModelScope.launch { while (isActive) { now.value = LocalDateTime.now(); delay(30_000) } } }
    fun playPause() = mediaRepository.playPause()
    fun previous() = mediaRepository.previous()
    fun next() = mediaRepository.next()
    fun toggleEditMode() { viewModelScope.launch { preferences.setEditMode(!state.value.editMode) } }
}
