package com.victorious.mango.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val NeonCyan = Color(0xFF00F0FF)
val NeonMagenta = Color(0xFFFF00E5)
val NearBlack = Color(0xFF0A0A0F)
val Surface1 = Color(0xFF14141C)
val TextHi = Color(0xFFE8F9FF)

private val Scheme = darkColorScheme(
    primary = NeonCyan, onPrimary = NearBlack,
    secondary = NeonMagenta, onSecondary = NearBlack,
    tertiary = NeonMagenta,
    background = NearBlack, onBackground = TextHi,
    surface = Surface1, onSurface = TextHi,
    surfaceVariant = Color(0xFF1C1C28), onSurfaceVariant = Color(0xFFB0C4CC),
    outline = NeonCyan.copy(alpha = 0.5f),
)

@Composable
fun VictorMangoTheme(content: @Composable () -> Unit) = MaterialTheme(colorScheme = Scheme, content = content)
