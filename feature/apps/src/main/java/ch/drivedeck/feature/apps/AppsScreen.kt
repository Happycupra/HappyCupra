package ch.drivedeck.feature.apps

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ch.drivedeck.core.design.AutomotiveCard

@Composable
fun AppsScreen(state: AppsUiState, onSearch: (String) -> Unit, onLaunch: (ch.drivedeck.core.model.LaunchableApp) -> Unit, onFavorite: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Text("Apps", style = MaterialTheme.typography.headlineLarge)
            OutlinedTextField(value = state.query, onValueChange = onSearch, label = { Text("App suchen") }, singleLine = true, modifier = Modifier.widthIn(max = 420.dp))
        }
        if (!state.loading && state.apps.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Keine passenden Apps gefunden") }
        LazyVerticalGrid(columns = GridCells.Adaptive(180.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(state.apps, key = { it.packageName }) { app ->
                AutomotiveCard(onClick = { onLaunch(app) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Apps, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                        Text(app.label, Modifier.weight(1f).padding(horizontal = 12.dp), maxLines = 2)
                        IconButton(onClick = { onFavorite(app.packageName) }, modifier = Modifier.size(56.dp)) {
                            Icon(if (app.packageName in state.favorites) Icons.Rounded.Star else Icons.Rounded.StarBorder, "Favorit")
                        }
                    }
                }
            }
        }
    }
}
