package ch.drivedeck.launcher

import android.app.role.RoleManager
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.drivedeck.core.design.DriveDeckTheme
import ch.drivedeck.core.model.ThemeMode
import ch.drivedeck.core.model.QuickAction
import ch.drivedeck.core.model.DayNightPolicy
import ch.drivedeck.feature.apps.*
import ch.drivedeck.feature.home.*
import ch.drivedeck.feature.settings.*
import kotlinx.coroutines.delay
import java.time.LocalTime

class MainActivity : ComponentActivity() {
    private val container get() = (application as DriveDeckApplication).container

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { DriveDeckRoot(container, ::openHomeSettings) }
    }
    override fun onStart() { super.onStart(); container.location.start() }
    override fun onStop() { container.location.stop(); super.onStop() }
    private fun openHomeSettings() {
        val intent = if (android.os.Build.VERSION.SDK_INT >= 29) getSystemService(RoleManager::class.java)?.createRequestRoleIntent(RoleManager.ROLE_HOME) else null
        startActivity(intent ?: Intent(Settings.ACTION_HOME_SETTINGS))
    }
}

private enum class Destination { HOME, APPS, SETTINGS }

@Composable
private fun DriveDeckRoot(container: AppContainer, openHomeSettings: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val settingsVm: SettingsViewModel = viewModel(factory = factory { SettingsViewModel(container.preferences) })
    val prefs by settingsVm.preferences.collectAsStateWithLifecycle()
    val location by container.location.reading.collectAsStateWithLifecycle()
    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { container.location.start() }
    val automaticDarkTheme = rememberAutomaticDarkTheme()
    SideEffect {
        (context as? Activity)?.window?.let { window ->
            val attributes = window.attributes
            attributes.screenBrightness = prefs.uiBrightness
            window.attributes = attributes
        }
    }
    DriveDeckTheme(darkTheme = when (prefs.themeMode) { ThemeMode.DARK -> true; ThemeMode.LIGHT -> false; ThemeMode.AUTO -> automaticDarkTheme }) {
        var destination by rememberSaveable { mutableStateOf(Destination.HOME) }
        var selectedAction by rememberSaveable { mutableStateOf(QuickAction.HOME) }
        Surface(Modifier.fillMaxSize()) {
            Column {
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    when (destination) {
                        Destination.HOME -> {
                            val vm: HomeViewModel = viewModel(factory = factory { HomeViewModel(container.preferences, container.media, container.location) })
                            val state by vm.state.collectAsStateWithLifecycle()
                            HomeScreen(state, onNavigation = { selectedAction = QuickAction.NAVIGATION; destination = Destination.APPS }, onEditMode = vm::toggleEditMode, onPlayPause = vm::playPause, onPrevious = vm::previous, onNext = vm::next, onMoveItem = vm::moveItem, onResizeItem = vm::resizeItem, onToggleItem = vm::toggleItem, modifier = Modifier.fillMaxSize())
                        }
                        Destination.APPS -> {
                            val vm: AppsViewModel = viewModel(factory = factory { AppsViewModel(container.apps, container.preferences) })
                            val state by vm.state.collectAsStateWithLifecycle()
                            AppsScreen(state, vm::search, vm::launch, vm::toggleFavorite, Modifier.fillMaxSize())
                        }
                        Destination.SETTINGS -> SettingsScreen(prefs, isDefaultHome(), hasMediaAccess(), location, BuildConfig.VERSION_NAME, settingsVm::setTheme, settingsVm::setBrightness, openHomeSettings, { context.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")) }, { locationPermission.launch(android.Manifest.permission.ACCESS_FINE_LOCATION) }, settingsVm::cycleQuickAction, settingsVm::moveQuickAction, Modifier.fillMaxSize())
                    }
                    IconButton(onClick = { selectedAction = QuickAction.SETTINGS; destination = Destination.SETTINGS }, Modifier.align(Alignment.TopEnd).padding(14.dp).size(64.dp)) { Icon(Icons.Rounded.Settings, "Einstellungen") }
                }
                FavoriteBar(prefs.quickActions, selectedAction) { action ->
                    selectedAction = action
                    destination = when (action) {
                        QuickAction.HOME -> Destination.HOME
                        QuickAction.SETTINGS -> Destination.SETTINGS
                        else -> Destination.APPS
                    }
                }
            }
        }
    }
}

@Composable private fun FavoriteBar(actions: List<QuickAction>, selected: QuickAction, onSelect: (QuickAction) -> Unit) {
    NavigationBar(modifier = Modifier.height(88.dp), tonalElevation = 0.dp) {
        actions.forEach { action ->
            BarItem(action.label, action.icon, action == selected) { onSelect(action) }
        }
    }
}
private val QuickAction.label get() = when (this) { QuickAction.HOME -> "Home"; QuickAction.NAVIGATION -> "Navigation"; QuickAction.MUSIC -> "Musik"; QuickAction.PHONE -> "Telefon"; QuickAction.APPS -> "Apps"; QuickAction.SETTINGS -> "Einstellungen" }
private val QuickAction.icon get() = when (this) { QuickAction.HOME -> Icons.Rounded.Home; QuickAction.NAVIGATION -> Icons.Rounded.Navigation; QuickAction.MUSIC -> Icons.Rounded.MusicNote; QuickAction.PHONE -> Icons.Rounded.Phone; QuickAction.APPS -> Icons.Rounded.Apps; QuickAction.SETTINGS -> Icons.Rounded.Settings }
@Composable private fun RowScope.BarItem(label: String, icon: ImageVector, selected: Boolean, action: () -> Unit) {
    NavigationBarItem(selected = selected, onClick = action, icon = { Icon(icon, label, Modifier.size(30.dp)) }, label = { Text(label) }, modifier = Modifier.fillMaxHeight())
}
@Composable private fun isDefaultHome(): Boolean {
    val context = androidx.compose.ui.platform.LocalContext.current
    return remember(context) {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        context.packageManager.resolveActivity(intent, 0)?.activityInfo?.packageName == context.packageName
    }
}
@Composable private fun hasMediaAccess(): Boolean {
    val context = androidx.compose.ui.platform.LocalContext.current
    return remember(context) {
        val enabled = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners").orEmpty()
        enabled.split(':').any { android.content.ComponentName.unflattenFromString(it)?.packageName == context.packageName }
    }
}
private inline fun <reified T : ViewModel> factory(crossinline create: () -> T) = object : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST") override fun <VM : ViewModel> create(modelClass: Class<VM>): VM = create() as VM
}

@Composable private fun rememberAutomaticDarkTheme(): Boolean {
    var dark by remember { mutableStateOf(isNightTime()) }
    LaunchedEffect(Unit) {
        while (true) {
            dark = isNightTime()
            delay(60_000)
        }
    }
    return dark
}

private fun isNightTime(time: LocalTime = LocalTime.now()): Boolean = DayNightPolicy.isNight(time.hour)
