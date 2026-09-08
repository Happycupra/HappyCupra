package ch.drivedeck.feature.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ch.drivedeck.core.design.AutomotiveCard
import ch.drivedeck.core.model.DashboardElementSize
import ch.drivedeck.core.model.DashboardElementType
import ch.drivedeck.core.model.DashboardItem
import ch.drivedeck.integration.media.MediaPlayback
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.abs

@Composable
fun HomeScreen(
    state: HomeUiState,
    onNavigation: () -> Unit,
    onEditMode: () -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onMoveItem: (Int, Int) -> Unit,
    onResizeItem: (DashboardItem) -> Unit,
    onToggleItem: (DashboardElementType) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
        val columns = if (maxWidth >= 1500.dp) 4 else if (maxWidth >= 900.dp) 3 else 2
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Header(state, onEditMode)
            if (state.editMode) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("EDIT-MODUS · Lange drücken und ziehen · Größe an der Kachel ändern", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DashboardElementType.entries.forEach { type ->
                            FilterChip(
                                selected = state.dashboardItems.any { it.type == type },
                                onClick = { onToggleItem(type) },
                                label = { Text(type.displayName) },
                                leadingIcon = { Icon(if (state.dashboardItems.any { it.type == type }) Icons.Rounded.Check else Icons.Rounded.Add, null) },
                            )
                        }
                    }
                }
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                itemsIndexed(
                    items = state.dashboardItems,
                    key = { _, item -> item.type.name },
                    span = { _, item -> GridItemSpan(item.size.columnSpan.coerceAtMost(maxLineSpan)) },
                ) { index, item ->
                    DashboardTile(
                        item = item,
                        state = state,
                        modifier = Modifier
                            .height(item.size.tileHeight)
                            .dashboardDrag(
                                enabled = state.editMode,
                                movePrevious = { onMoveItem(index, index - 1) },
                                moveNext = { onMoveItem(index, index + 1) },
                            ),
                        onNavigation = onNavigation,
                        onPlayPause = onPlayPause,
                        onPrevious = onPrevious,
                        onNext = onNext,
                        onResize = { onResizeItem(item) },
                    )
                }
            }
        }
    }
}

@Composable
private fun Header(state: HomeUiState, onEditMode: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(state.now.format(DateTimeFormatter.ofPattern("HH:mm")), style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Light)
            Text(state.now.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(Locale.getDefault())), color = MaterialTheme.colorScheme.secondary)
        }
        if (state.editMode) FilledTonalButton(onClick = onEditMode, modifier = Modifier.heightIn(min = 56.dp)) {
            Icon(Icons.Rounded.Done, null); Spacer(Modifier.width(8.dp)); Text("Fertig")
        } else IconButton(onClick = onEditMode, modifier = Modifier.size(64.dp)) { Icon(Icons.Rounded.Edit, "Dashboard bearbeiten") }
    }
}

@Composable
private fun DashboardTile(
    item: DashboardItem,
    state: HomeUiState,
    modifier: Modifier,
    onNavigation: () -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onResize: () -> Unit,
) {
    Box(modifier) {
        when (item.type) {
            DashboardElementType.MEDIA -> MediaTile(state, item.size, if (state.editMode) null else onPlayPause, onPrevious, onNext)
            DashboardElementType.NAVIGATION -> ValueTile("NAVIGATION", state.demo.destination, "Bevorzugte App öffnen", Icons.Rounded.Navigation, if (state.editMode) null else onNavigation)
            DashboardElementType.SPEED -> ValueTile("TEMPO", "${state.demo.speedKmh} km/h", "Demo", Icons.Rounded.Speed)
            DashboardElementType.WEATHER -> ValueTile("WETTER", state.demo.weather, "Demo", Icons.Rounded.WbSunny)
            DashboardElementType.PHONE -> ValueTile("TELEFON", "Verbinden", "Telefon-App auswählen", Icons.Rounded.Phone)
            DashboardElementType.RADIO -> ValueTile("RADIO", "Radio", "Radio-App auswählen", Icons.Rounded.Radio)
        }
        if (state.editMode) {
            Row(Modifier.align(Alignment.TopEnd).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                AssistChip(onClick = onResize, label = { Text(item.size.displayName) })
                Spacer(Modifier.width(8.dp)); Icon(Icons.Rounded.DragIndicator, "Zum Verschieben lange drücken", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable private fun ValueTile(label: String, value: String, detail: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: (() -> Unit)? = null) {
    AutomotiveCard(Modifier.fillMaxSize(), onClick = onClick) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Icon(icon, null, Modifier.size(42.dp), tint = MaterialTheme.colorScheme.primary)
            Column { Text(label, color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelLarge); Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, maxLines = 1); Text(detail, color = MaterialTheme.colorScheme.secondary, maxLines = 1) }
        }
    }
}

@Composable private fun MediaTile(state: HomeUiState, size: DashboardElementSize, onPlayPause: (() -> Unit)?, onPrevious: () -> Unit, onNext: () -> Unit) {
    val playback = state.media.takeIf { it.available } ?: MediaPlayback.Preview
    AutomotiveCard(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            val compact = size == DashboardElementSize.SMALL || size == DashboardElementSize.MEDIUM
            if (!compact && playback.artwork != null) Image(playback.artwork.asImageBitmap(), "Albumcover", Modifier.size(88.dp).clip(RoundedCornerShape(16.dp)), contentScale = ContentScale.Crop)
            else if (!compact) Box(Modifier.size(88.dp), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Album, null, Modifier.size(62.dp), tint = MaterialTheme.colorScheme.primary) }
            Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
                Text(playback.title.ifBlank { "Kein Titel" }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(playback.artist.ifBlank { playback.appName }, color = MaterialTheme.colorScheme.secondary, maxLines = 1)
                if (playback.durationMs > 0) LinearProgressIndicator(progress = { (playback.positionMs.toFloat() / playback.durationMs).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp))
                Text(if (state.media.available) playback.appName else "Demo · Medienzugriff aktivieren", style = MaterialTheme.typography.labelSmall)
            }
            Row {
                if (!compact) IconButton(onClick = onPrevious, enabled = state.media.available && !state.editMode, modifier = Modifier.size(56.dp)) { Icon(Icons.Rounded.SkipPrevious, "Zurück") }
                IconButton(onClick = { onPlayPause?.invoke() }, enabled = state.media.available && onPlayPause != null, modifier = Modifier.size(56.dp)) { Icon(if (playback.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, "Play/Pause") }
                if (!compact) IconButton(onClick = onNext, enabled = state.media.available && !state.editMode, modifier = Modifier.size(56.dp)) { Icon(Icons.Rounded.SkipNext, "Weiter") }
            }
        }
    }
}

private val DashboardElementSize.columnSpan get() = if (this == DashboardElementSize.WIDE || this == DashboardElementSize.LARGE) 2 else 1
private val DashboardElementSize.tileHeight get() = when (this) {
    DashboardElementSize.SMALL -> 146.dp
    DashboardElementSize.MEDIUM -> 176.dp
    DashboardElementSize.LARGE -> 214.dp
    DashboardElementSize.WIDE -> 176.dp
}
private val DashboardElementSize.displayName get() = when (this) {
    DashboardElementSize.SMALL -> "Klein"
    DashboardElementSize.MEDIUM -> "Mittel"
    DashboardElementSize.LARGE -> "Groß"
    DashboardElementSize.WIDE -> "Breit"
}
private val DashboardElementType.displayName get() = when (this) {
    DashboardElementType.NAVIGATION -> "Navigation"
    DashboardElementType.MEDIA -> "Medien"
    DashboardElementType.PHONE -> "Telefon"
    DashboardElementType.RADIO -> "Radio"
    DashboardElementType.SPEED -> "Tempo"
    DashboardElementType.WEATHER -> "Wetter"
}

private fun Modifier.dashboardDrag(enabled: Boolean, movePrevious: () -> Unit, moveNext: () -> Unit): Modifier {
    if (!enabled) return this
    return pointerInput(movePrevious, moveNext) {
        var movement = 0f
        detectDragGesturesAfterLongPress(
            onDragStart = { movement = 0f },
            onDragEnd = { movement = 0f },
            onDragCancel = { movement = 0f },
            onDrag = { change, amount ->
                change.consume()
                movement += if (abs(amount.x) > abs(amount.y)) amount.x else amount.y
                if (movement > 72f) { moveNext(); movement = 0f }
                else if (movement < -72f) { movePrevious(); movement = 0f }
            },
        )
    }
}
