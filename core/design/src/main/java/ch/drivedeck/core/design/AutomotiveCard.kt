package ch.drivedeck.core.design

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AutomotiveCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    highlighted: Boolean = false,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.defaultMinSize(minWidth = 96.dp, minHeight = 72.dp).then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = AutomotiveShape,
        colors = CardDefaults.cardColors(),
        border = if (highlighted) BorderStroke(2.dp, androidx.compose.material3.MaterialTheme.colorScheme.primary) else null,
    ) { Box(Modifier.padding(PaddingValues(20.dp))) { content() } }
}
