package ch.drivedeck.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.drivedeck.core.model.DemoDriveData
import ch.drivedeck.core.model.DashboardItem
import ch.drivedeck.core.model.DashboardElementType
import ch.drivedeck.core.model.DefaultDashboardItems
import ch.drivedeck.core.preferences.PreferencesRepository
import ch.drivedeck.integration.media.MediaPlayback
import ch.drivedeck.integration.media.MediaRepository
import ch.drivedeck.integration.gps.LocationReading
import ch.drivedeck.integration.gps.LocationRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDateTime

data class HomeUiState(
    val now: LocalDateTime = LocalDateTime.now(),
    val demo: DemoDriveData = DemoDriveData.Preview,
    val media: MediaPlayback = MediaPlayback.Unavailable,
    val editMode: Boolean = false,
    val location: LocationReading = LocationReading(),
    val dashboardItems: List<DashboardItem> = ch.drivedeck.core.model.DefaultDashboardItems,
)
class HomeViewModel(private val preferences: PreferencesRepository, private val mediaRepository: MediaRepository, private val locationRepository: LocationRepository) : ViewModel() {
    private val now = MutableStateFlow(LocalDateTime.now())
    val state = combine(now, preferences.preferences, mediaRepository.playback, locationRepository.reading) { time, prefs, media, location ->
        HomeUiState(time, media = media, location = location, editMode = prefs.editModeEnabled, dashboardItems = prefs.dashboardItems)
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())
    init { viewModelScope.launch { while (isActive) { now.value = LocalDateTime.now(); delay(30_000) } } }
    fun playPause() = mediaRepository.playPause()
    fun previous() = mediaRepository.previous()
    fun next() = mediaRepository.next()
    fun toggleEditMode() { viewModelScope.launch { preferences.setEditMode(!state.value.editMode) } }
    fun moveItem(fromIndex: Int, toIndex: Int) {
        val current = state.value.dashboardItems
        if (fromIndex !in current.indices || toIndex !in current.indices || fromIndex == toIndex) return
        val changed = current.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
        viewModelScope.launch { preferences.setDashboardItems(changed) }
    }
    fun resizeItem(item: DashboardItem) {
        val changed = state.value.dashboardItems.map { current ->
            if (current.type == item.type) current.copy(size = current.size.next()) else current
        }
        viewModelScope.launch { preferences.setDashboardItems(changed) }
    }
    fun toggleItem(type: DashboardElementType) {
        val current = state.value.dashboardItems
        val changed = if (current.any { it.type == type }) {
            if (current.size == 1) return else current.filterNot { it.type == type }
        } else current + requireNotNull(DefaultDashboardItems.firstOrNull { it.type == type })
        viewModelScope.launch { preferences.setDashboardItems(changed) }
    }
}
