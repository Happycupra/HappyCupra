package ch.drivedeck.launcher

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.isSystemInDarkTheme
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
import ch.drivedeck.feature.apps.*
import ch.drivedeck.feature.home.*
import ch.drivedeck.feature.settings.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as DriveDeckApplication).container
        setContent { DriveDeckRoot(container, ::openHomeSettings) }
    }
    private fun openHomeSettings() {
        val intent = if (android.os.Build.VERSION.SDK_INT >= 29) getSystemService(RoleManager::class.java)?.createRequestRoleIntent(RoleManager.ROLE_HOME) else null
        startActivity(intent ?: Intent(Settings.ACTION_HOME_SETTINGS))
    }
}

private enum class Destination { HOME, APPS, SETTINGS }

@Composable
private fun DriveDeckRoot(container: AppContainer, openHomeSettings: () -> Unit) {
    val settingsVm: SettingsViewModel = viewModel(factory = factory { SettingsViewModel(container.preferences) })
    val prefs by settingsVm.preferences.collectAsStateWithLifecycle()
    DriveDeckTheme(darkTheme = when (prefs.themeMode) { ThemeMode.DARK -> true; ThemeMode.LIGHT -> false; ThemeMode.AUTO -> isSystemInDarkTheme() }) {
        var destination by rememberSaveable { mutableStateOf(Destination.HOME) }
        Surface(Modifier.fillMaxSize()) {
            Column {
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    when (destination) {
                        Destination.HOME -> {
                            val vm: HomeViewModel = viewModel(factory = factory { HomeViewModel(container.preferences) })
                            val state by vm.state.collectAsStateWithLifecycle()
                            HomeScreen(state, onNavigation = { destination = Destination.APPS }, onEditMode = vm::toggleEditMode, Modifier.fillMaxSize())
                        }
                        Destination.APPS -> {
                            val vm: AppsViewModel = viewModel(factory = factory { AppsViewModel(container.apps, container.preferences) })
                            val state by vm.state.collectAsStateWithLifecycle()
                            AppsScreen(state, vm::search, vm::launch, vm::toggleFavorite, Modifier.fillMaxSize())
                        }
                        Destination.SETTINGS -> SettingsScreen(prefs, isDefaultHome(), settingsVm::setTheme, openHomeSettings, Modifier.fillMaxSize())
                    }
                    IconButton(onClick = { destination = Destination.SETTINGS }, Modifier.align(Alignment.TopEnd).padding(14.dp).size(64.dp)) { Icon(Icons.Rounded.Settings, "Einstellungen") }
                }
                FavoriteBar(destination, onSelect = { destination = it })
            }
        }
    }
}

@Composable private fun FavoriteBar(selected: Destination, onSelect: (Destination) -> Unit) {
    NavigationBar(modifier = Modifier.height(88.dp), tonalElevation = 0.dp) {
        BarItem("Home", Icons.Rounded.Home, selected == Destination.HOME) { onSelect(Destination.HOME) }
        BarItem("Navigation", Icons.Rounded.Navigation, false) { onSelect(Destination.APPS) }
        BarItem("Musik", Icons.Rounded.MusicNote, false) { onSelect(Destination.APPS) }
        BarItem("Telefon", Icons.Rounded.Phone, false) { onSelect(Destination.APPS) }
        BarItem("Apps", Icons.Rounded.Apps, selected == Destination.APPS) { onSelect(Destination.APPS) }
    }
}
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
private inline fun <reified T : ViewModel> factory(crossinline create: () -> T) = object : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST") override fun <VM : ViewModel> create(modelClass: Class<VM>): VM = create() as VM
}
