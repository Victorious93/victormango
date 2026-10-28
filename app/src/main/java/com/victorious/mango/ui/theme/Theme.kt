package com.victorious.mango.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.victorious.mango.ui.theme.fleet.FleetPalette
import com.victorious.mango.ui.theme.fleet.FleetThemeState

val NeonCyan = Color(0xFF00F0FF)
val NeonMagenta = Color(0xFFFF00E5)
val NearBlack = Color(0xFF0A0A0F)
val Surface1 = Color(0xFF14141C)
val TextHi = Color(0xFFE8F9FF)

/** Built-in neon theme; used whenever the fleet theme is unavailable. */
val NeonScheme: ColorScheme = darkColorScheme(
    primary = NeonCyan, onPrimary = NearBlack,
    secondary = NeonMagenta, onSecondary = NearBlack,
    tertiary = NeonMagenta,
    background = NearBlack, onBackground = TextHi,
    surface = Surface1, onSurface = TextHi,
    surfaceVariant = Color(0xFF1C1C28), onSurfaceVariant = Color(0xFFB0C4CC),
    outline = NeonCyan.copy(alpha = 0.5f),
)

/** Maps VictorLaunch's palette.* onto a Material scheme. */
fun schemeFor(p: FleetPalette): ColorScheme {
    val bg = Color(p.background.toInt())
    val light = bg.luminance() > 0.5f
    val primary = Color(p.primary.toInt())
    val secondary = Color(p.secondary.toInt())
    val onBg = Color(p.onBackground.toInt())
    val muted = p.muted?.let { Color(it.toInt()) } ?: onBg.copy(alpha = 0.7f)
    val base = if (light) lightColorScheme() else darkColorScheme()
    return base.copy(
        primary = primary, onPrimary = bg,
        secondary = secondary, onSecondary = bg,
        tertiary = secondary,
        background = bg, onBackground = onBg,
        surface = Color(p.surface.toInt()), onSurface = onBg,
        surfaceVariant = Color(p.surface.toInt()), onSurfaceVariant = muted,
        outline = primary.copy(alpha = 0.5f),
        error = p.danger?.let { Color(it.toInt()) } ?: base.error,
    )
}

fun schemeFor(state: FleetThemeState?): ColorScheme =
    (state as? FleetThemeState.Applied)?.let { schemeFor(it.palette) } ?: NeonScheme

@Composable
fun VictorMangoTheme(fleet: FleetThemeState? = null, content: @Composable () -> Unit) =
    MaterialTheme(colorScheme = schemeFor(fleet), content = content)
