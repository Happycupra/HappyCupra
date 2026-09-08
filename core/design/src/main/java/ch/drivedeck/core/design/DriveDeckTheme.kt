package ch.drivedeck.core.design

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val DriveDeckAccent = Color(0xFF47D7C8)
val DriveDeckCard = Color(0xFF172127)
val AutomotiveShape = RoundedCornerShape(24.dp)

private val DarkColors = darkColorScheme(
    primary = DriveDeckAccent, onPrimary = Color(0xFF00201D), background = Color(0xFF071014),
    onBackground = Color(0xFFF2F7F7), surface = DriveDeckCard, onSurface = Color(0xFFF2F7F7),
    secondary = Color(0xFF93AEB0), outline = Color(0xFF405257), error = Color(0xFFFFB4AB),
)
private val LightColors = lightColorScheme(primary = Color(0xFF006A62), background = Color(0xFFF3FAF9), surface = Color.White)

@Composable
fun DriveDeckTheme(darkTheme: Boolean = true, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, typography = Typography(), content = content)
}
