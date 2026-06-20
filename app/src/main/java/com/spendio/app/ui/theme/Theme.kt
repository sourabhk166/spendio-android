package com.spendio.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Spendio brand palette ────────────────────────────────────────────────────
val KhTeal = Color(0xFF1D9E75)
val KhTealLight = Color(0xFFE1F5EE)
val KhTealDark = Color(0xFF5DD3A9)
val KhTealContainerDark = Color(0xFF085041)

private val LightColors = lightColorScheme(
    primary = KhTeal,
    onPrimary = Color.White,
    primaryContainer = KhTealLight,
    onPrimaryContainer = Color(0xFF0F6E56),
    secondary = Color(0xFF4C6A5D),
    onSecondary = Color.White,
    secondaryContainer = KhTealLight,
    onSecondaryContainer = Color(0xFF0F6E56),
    // Amber — used for review / warning banners
    tertiary = Color(0xFFBA7517),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFF4E0),
    onTertiaryContainer = Color(0xFF633806),
    background = Color(0xFFF2F1ED),
    onBackground = Color(0xFF1A1918),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1918),
    surfaceVariant = Color(0xFFF2F1ED),
    onSurfaceVariant = Color(0xFF7A7871),
    outline = Color(0xFFE8E7E2),
    error = Color(0xFFE24B4A),
    onError = Color.White,
    errorContainer = Color(0xFFFCEBEB),
    onErrorContainer = Color(0xFFA32D2D),
)

private val DarkColors = darkColorScheme(
    primary = KhTealDark,
    onPrimary = Color(0xFF033325),
    primaryContainer = KhTealContainerDark,
    onPrimaryContainer = Color(0xFF9FE1CB),
    secondary = Color(0xFF9FE1CB),
    onSecondary = Color(0xFF003826),
    secondaryContainer = KhTealContainerDark,
    onSecondaryContainer = Color(0xFF9FE1CB),
    tertiary = Color(0xFFEFC97A),
    onTertiary = Color(0xFF3A2200),
    tertiaryContainer = Color(0xFF53360A),
    onTertiaryContainer = Color(0xFFFAC775),
    background = Color(0xFF1A1918),
    onBackground = Color(0xFFEEEDE9),
    surface = Color(0xFF252320),
    onSurface = Color(0xFFEEEDE9),
    surfaceVariant = Color(0xFF2E2C29),
    onSurfaceVariant = Color(0xFFB4B2A9),
    outline = Color(0xFF3D3C38),
    error = Color(0xFFEF9595),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF79141E),
    onErrorContainer = Color(0xFFFFB4AB),
)

@Composable
fun SpendioTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // Dynamic color disabled — we use Spendio teal as the brand colour,
    // not the user's wallpaper.
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
