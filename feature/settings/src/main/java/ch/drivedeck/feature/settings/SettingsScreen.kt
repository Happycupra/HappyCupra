package ch.drivedeck.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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

@Composable
fun SettingsScreen(
    preferences: UserPreferences,
    isDefaultLauncher: Boolean,
    hasMediaAccess: Boolean,
    onTheme: (ThemeMode) -> Unit,
    onOpenHomeSettings: () -> Unit,
    onOpenMediaAccess: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.padding(24.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        Column(Modifier.widthIn(min = 240.dp, max = 340.dp)) { Text("Einstellungen", style = MaterialTheme.typography.headlineLarge); Spacer(Modifier.height(16.dp)); listOf("Dashboard", "Design", "Apps", "Navigation", "Media", "Radio", "Telefon", "Fahrzeug", "System", "Über").forEach { Text(it, Modifier.padding(vertical = 9.dp), color = if (it == "Design") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface) } }
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { Text("Design", style = MaterialTheme.typography.headlineMedium) }
            item { AutomotiveCard { Column { Text("Darstellung", style = MaterialTheme.typography.titleLarge); Spacer(Modifier.height(12.dp)); Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { ThemeMode.entries.forEach { mode -> FilterChip(selected = preferences.themeMode == mode, onClick = { onTheme(mode) }, label = { Text(when(mode) { ThemeMode.DARK -> "Nacht"; ThemeMode.LIGHT -> "Tag"; ThemeMode.AUTO -> "Auto" }) }, leadingIcon = { Icon(when(mode) { ThemeMode.DARK -> Icons.Rounded.DarkMode; ThemeMode.LIGHT -> Icons.Rounded.LightMode; ThemeMode.AUTO -> Icons.Rounded.BrightnessAuto }, null) }) } } } } }
            item { Text("Media", style = MaterialTheme.typography.headlineMedium) }
            item { AutomotiveCard(onClick = if (hasMediaAccess) null else onOpenMediaAccess, highlighted = !hasMediaAccess) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(if (hasMediaAccess) Icons.Rounded.CheckCircle else Icons.Rounded.NotificationsActive, null, Modifier.size(42.dp), tint = MaterialTheme.colorScheme.primary); Column(Modifier.padding(start = 16.dp)) { Text(if (hasMediaAccess) "Medienzugriff aktiv" else "Medienzugriff erlauben", style = MaterialTheme.typography.titleLarge); Text(if (hasMediaAccess) "Aktive MediaSessions können gesteuert werden." else "Erforderlich für Titel, Albumcover und Wiedergabesteuerung.", color = MaterialTheme.colorScheme.secondary) } } } }
            item { Text("System", style = MaterialTheme.typography.headlineMedium) }
            item { AutomotiveCard(onClick = if (isDefaultLauncher) null else onOpenHomeSettings, highlighted = !isDefaultLauncher) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(if (isDefaultLauncher) Icons.Rounded.CheckCircle else Icons.Rounded.Home, null, Modifier.size(42.dp), tint = MaterialTheme.colorScheme.primary); Column(Modifier.padding(start = 16.dp)) { Text(if (isDefaultLauncher) "DriveDeck ist Standard-Launcher" else "DriveDeck als Standard festlegen", style = MaterialTheme.typography.titleLarge); Text(if (isDefaultLauncher) "HOME ist korrekt eingerichtet." else "Antippen, um die Android HOME-Einstellungen zu öffnen.", color = MaterialTheme.colorScheme.secondary) } } } }
            item { AutomotiveCard { Column { Text("Phase 1", style = MaterialTheme.typography.titleLarge); Text("Radio-, Telefon-, GPS- und Fahrzeugadapter sind bewusst noch nicht aktiv.", color = MaterialTheme.colorScheme.secondary) } } }
        }
    }
}
