package ch.drivedeck.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ch.drivedeck.core.design.AutomotiveCard
import ch.drivedeck.core.model.ThemeMode
import ch.drivedeck.core.model.UserPreferences
import ch.drivedeck.core.model.QuickAction
import ch.drivedeck.integration.gps.GpsStatus
import ch.drivedeck.integration.gps.LocationReading

@Composable
fun SettingsScreen(
    preferences: UserPreferences,
    isDefaultLauncher: Boolean,
    hasMediaAccess: Boolean,
    location: LocationReading,
    appVersion: String,
    onTheme: (ThemeMode) -> Unit,
    onBrightness: (Float) -> Unit,
    onOpenHomeSettings: () -> Unit,
    onOpenMediaAccess: () -> Unit,
    onRequestLocationPermission: () -> Unit,
    onCycleQuickAction: (Int) -> Unit,
    onMoveQuickAction: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.padding(24.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        Column(Modifier.widthIn(min = 240.dp, max = 340.dp)) { Text("Einstellungen", style = MaterialTheme.typography.headlineLarge); Spacer(Modifier.height(16.dp)); listOf("Dashboard", "Design", "Apps", "Navigation", "Media", "Radio", "Telefon", "Fahrzeug", "System", "Über").forEach { Text(it, Modifier.padding(vertical = 9.dp), color = if (it == "Design") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface) } }
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { Text("Design", style = MaterialTheme.typography.headlineMedium) }
            item { AutomotiveCard { Column { Text("Darstellung", style = MaterialTheme.typography.titleLarge); Spacer(Modifier.height(12.dp)); Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { ThemeMode.entries.forEach { mode -> FilterChip(selected = preferences.themeMode == mode, onClick = { onTheme(mode) }, label = { Text(when(mode) { ThemeMode.DARK -> "Nacht"; ThemeMode.LIGHT -> "Tag"; ThemeMode.AUTO -> "Auto" }) }, leadingIcon = { Icon(when(mode) { ThemeMode.DARK -> Icons.Rounded.DarkMode; ThemeMode.LIGHT -> Icons.Rounded.LightMode; ThemeMode.AUTO -> Icons.Rounded.BrightnessAuto }, null) }) } } } } }
            item {
                AutomotiveCard {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Brightness6, null, tint = MaterialTheme.colorScheme.primary)
                            Text("UI-Helligkeit", Modifier.weight(1f).padding(start = 16.dp), style = MaterialTheme.typography.titleLarge)
                            Text("${(preferences.uiBrightness * 100).toInt()} %", color = MaterialTheme.colorScheme.secondary)
                        }
                        Slider(value = preferences.uiBrightness, onValueChange = onBrightness, valueRange = 0.2f..1f, modifier = Modifier.heightIn(min = 56.dp))
                    }
                }
            }
            item { Text("Favoritenleiste", style = MaterialTheme.typography.headlineMedium) }
            items(preferences.quickActions.size) { index ->
                val action = preferences.quickActions[index]
                AutomotiveCard {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(action.icon, null, Modifier.size(38.dp), tint = MaterialTheme.colorScheme.primary)
                        Column(Modifier.weight(1f).padding(horizontal = 16.dp)) { Text("Position ${index + 1}", color = MaterialTheme.colorScheme.secondary); Text(action.label, style = MaterialTheme.typography.titleLarge) }
                        IconButton(onClick = { onMoveQuickAction(index, -1) }, enabled = index > 0, modifier = Modifier.size(56.dp)) { Icon(Icons.Rounded.ArrowBack, "Nach links") }
                        IconButton(onClick = { onMoveQuickAction(index, 1) }, enabled = index < preferences.quickActions.lastIndex, modifier = Modifier.size(56.dp)) { Icon(Icons.Rounded.ArrowForward, "Nach rechts") }
                        FilledTonalButton(onClick = { onCycleQuickAction(index) }, modifier = Modifier.heightIn(min = 56.dp)) { Text("Ändern") }
                    }
                }
            }
            item { Text("Media", style = MaterialTheme.typography.headlineMedium) }
            item { AutomotiveCard(onClick = if (hasMediaAccess) null else onOpenMediaAccess, highlighted = !hasMediaAccess) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(if (hasMediaAccess) Icons.Rounded.CheckCircle else Icons.Rounded.NotificationsActive, null, Modifier.size(42.dp), tint = MaterialTheme.colorScheme.primary); Column(Modifier.padding(start = 16.dp)) { Text(if (hasMediaAccess) "Medienzugriff aktiv" else "Medienzugriff erlauben", style = MaterialTheme.typography.titleLarge); Text(if (hasMediaAccess) "Aktive MediaSessions können gesteuert werden." else "Erforderlich für Titel, Albumcover und Wiedergabesteuerung.", color = MaterialTheme.colorScheme.secondary) } } } }
            item { Text("GPS", style = MaterialTheme.typography.headlineMedium) }
            item {
                val permissionMissing = location.status == GpsStatus.PERMISSION_REQUIRED
                AutomotiveCard(onClick = if (permissionMissing) onRequestLocationPermission else null, highlighted = permissionMissing) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (location.hasFix) Icons.Rounded.GpsFixed else Icons.Rounded.GpsNotFixed, null, Modifier.size(42.dp), tint = MaterialTheme.colorScheme.primary)
                        Column(Modifier.padding(start = 16.dp)) {
                            Text(location.status.title, style = MaterialTheme.typography.titleLarge)
                            Text(location.status.detail, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            }
            item { Text("System", style = MaterialTheme.typography.headlineMedium) }
            item { AutomotiveCard(onClick = if (isDefaultLauncher) null else onOpenHomeSettings, highlighted = !isDefaultLauncher) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(if (isDefaultLauncher) Icons.Rounded.CheckCircle else Icons.Rounded.Home, null, Modifier.size(42.dp), tint = MaterialTheme.colorScheme.primary); Column(Modifier.padding(start = 16.dp)) { Text(if (isDefaultLauncher) "DriveDeck ist Standard-Launcher" else "DriveDeck als Standard festlegen", style = MaterialTheme.typography.titleLarge); Text(if (isDefaultLauncher) "HOME ist korrekt eingerichtet." else "Antippen, um die Android HOME-Einstellungen zu öffnen.", color = MaterialTheme.colorScheme.secondary) } } } }
            item { AutomotiveCard { Column { Text("DriveDeck Debug $appVersion", style = MaterialTheme.typography.titleLarge); Text("Radio-, Telefon- und Fahrzeugadapter sind bewusst noch nicht aktiv.", color = MaterialTheme.colorScheme.secondary) } } }
        }
    }
}

private val QuickAction.label get() = when (this) {
    QuickAction.HOME -> "Home"
    QuickAction.NAVIGATION -> "Navigation"
    QuickAction.MUSIC -> "Musik"
    QuickAction.PHONE -> "Telefon"
    QuickAction.APPS -> "Apps"
    QuickAction.SETTINGS -> "Einstellungen"
}
private val QuickAction.icon get() = when (this) {
    QuickAction.HOME -> Icons.Rounded.Home
    QuickAction.NAVIGATION -> Icons.Rounded.Navigation
    QuickAction.MUSIC -> Icons.Rounded.MusicNote
    QuickAction.PHONE -> Icons.Rounded.Phone
    QuickAction.APPS -> Icons.Rounded.Apps
    QuickAction.SETTINGS -> Icons.Rounded.Settings
}
private val GpsStatus.title get() = when (this) {
    GpsStatus.PERMISSION_REQUIRED -> "Standortzugriff erlauben"
    GpsStatus.DISABLED -> "GPS ist ausgeschaltet"
    GpsStatus.SEARCHING -> "GPS sucht Satelliten"
    GpsStatus.FIXED -> "GPS-Signal verfügbar"
    GpsStatus.UNAVAILABLE -> "GPS nicht verfügbar"
}
private val GpsStatus.detail get() = when (this) {
    GpsStatus.PERMISSION_REQUIRED -> "Erforderlich für Geschwindigkeit und Richtung."
    GpsStatus.DISABLED -> "GPS bitte in den Android-Einstellungen aktivieren."
    GpsStatus.SEARCHING -> "Für den ersten Fix freie Sicht zum Himmel sicherstellen."
    GpsStatus.FIXED -> "Geschwindigkeit wird lokal berechnet; kein Netzwerkzugriff."
    GpsStatus.UNAVAILABLE -> "DriveDeck bleibt ohne GPS vollständig bedienbar."
}
