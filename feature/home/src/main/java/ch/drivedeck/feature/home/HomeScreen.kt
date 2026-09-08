package ch.drivedeck.feature.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ch.drivedeck.core.design.AutomotiveCard
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
fun HomeScreen(state: HomeUiState, onNavigation: () -> Unit, onEditMode: () -> Unit, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.padding(24.dp)) {
        val wide = maxWidth >= 1100.dp
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(state.now.format(DateTimeFormatter.ofPattern("HH:mm")), style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Light)
                    Text(state.now.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(Locale.getDefault())), color = MaterialTheme.colorScheme.secondary)
                }
                if (state.editMode) AssistChip(onClick = onEditMode, label = { Text("Bearbeiten beenden") }, leadingIcon = { Icon(Icons.Rounded.LockOpen, null) })
                else IconButton(onClick = onEditMode, modifier = Modifier.size(64.dp)) { Icon(Icons.Rounded.Edit, "Dashboard bearbeiten") }
            }
            if (state.editMode) Text("EDIT-MODUS · Verschieben folgt in Phase 2", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                AutomotiveCard(Modifier.weight(if (wide) 1.2f else 1f).fillMaxHeight(), onClick = onNavigation, highlighted = state.editMode) {
                    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                        Icon(Icons.Rounded.Navigation, null, Modifier.size(52.dp), tint = MaterialTheme.colorScheme.primary)
                        Column { Text("NAVIGATION", color = MaterialTheme.colorScheme.secondary); Text(state.demo.destination, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold); Text("Bevorzugte App öffnen") }
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    AutomotiveCard(Modifier.weight(1f).fillMaxWidth(), highlighted = state.editMode) {
                        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(88.dp), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Album, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary) }
                            Column(Modifier.weight(1f).padding(horizontal = 16.dp)) { Text(state.demo.song, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text(state.demo.artist, color = MaterialTheme.colorScheme.secondary); Text("Demo-Mediendaten", style = MaterialTheme.typography.labelSmall) }
                            Row { IconButton(onClick = {}, modifier = Modifier.size(64.dp)) { Icon(Icons.Rounded.SkipPrevious, "Zurück") }; IconButton(onClick = {}, modifier = Modifier.size(64.dp)) { Icon(Icons.Rounded.PlayArrow, "Abspielen") }; IconButton(onClick = {}, modifier = Modifier.size(64.dp)) { Icon(Icons.Rounded.SkipNext, "Weiter") } }
                        }
                    }
                    Row(Modifier.weight(0.72f), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        InfoTile("TEMPO", "${state.demo.speedKmh}", "km/h", Icons.Rounded.Speed, Modifier.weight(1f), state.editMode)
                        InfoTile("WETTER", state.demo.weather, "Demo", Icons.Rounded.WbSunny, Modifier.weight(1f), state.editMode)
                    }
                }
            }
        }
    }
}

@Composable private fun InfoTile(label: String, value: String, suffix: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, edit: Boolean) {
    AutomotiveCard(modifier.fillMaxHeight(), highlighted = edit) { Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary); Text(label, color = MaterialTheme.colorScheme.secondary); Row(verticalAlignment = Alignment.Bottom) { Text(value, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold); Text(" $suffix") } } }
}
