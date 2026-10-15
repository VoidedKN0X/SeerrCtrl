package com.vknxworks.seerrctrl.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.vknxworks.seerrctrl.App
import com.vknxworks.seerrctrl.data.ThemeMode

/** Fixed meaning colours for availability and request badges, tuned per light/dark. */
@Immutable
data class StatusColors(
    val available: Color,
    val partial: Color,
    val processing: Color,
    val pending: Color,
    val declined: Color,
    val onBadge: Color,
)

private val DarkStatus = StatusColors(
    available = Color(0xFF34C77B),
    partial = Color(0xFF2BA8A0),
    processing = Color(0xFF7C6CF2),
    pending = Color(0xFFE5A92E),
    declined = Color(0xFFE5534B),
    onBadge = Color.White,
)
private val LightStatus = StatusColors(
    available = Color(0xFF15934F),
    partial = Color(0xFF0F7F79),
    processing = Color(0xFF5B4BD6),
    pending = Color(0xFFB57A00),
    declined = Color(0xFFC62828),
    onBadge = Color.White,
)

val LocalStatusColors = staticCompositionLocalOf { DarkStatus }

val Status: StatusColors
    @Composable get() = LocalStatusColors.current

// Brand fallback when Material You is off: Seerr's indigo.
private val BrandDark = darkColorScheme(
    primary = Color(0xFFC5C0FF),
    onPrimary = Color(0xFF2A1D8C),
    primaryContainer = Color(0xFF4136A3),
    onPrimaryContainer = Color(0xFFE4DFFF),
    secondary = Color(0xFFC8C3DC),
    onSecondary = Color(0xFF302E41),
    secondaryContainer = Color(0xFF474459),
    onSecondaryContainer = Color(0xFFE4DFF9),
    tertiary = Color(0xFFEBB9D0),
    onTertiary = Color(0xFF472638),
    tertiaryContainer = Color(0xFF603C4F),
    onTertiaryContainer = Color(0xFFFFD8E8),
    background = Color(0xFF12131A),
    onBackground = Color(0xFFE4E1EC),
    surface = Color(0xFF12131A),
    onSurface = Color(0xFFE4E1EC),
    surfaceVariant = Color(0xFF47464F),
    onSurfaceVariant = Color(0xFFC8C5D0),
    surfaceContainerLowest = Color(0xFF0D0E14),
    surfaceContainerLow = Color(0xFF1A1B22),
    surfaceContainer = Color(0xFF1F1F26),
    surfaceContainerHigh = Color(0xFF292931),
    surfaceContainerHighest = Color(0xFF34343C),
    outline = Color(0xFF928F99),
    outlineVariant = Color(0xFF47464F),
    error = Color(0xFFFFB4AB),
)

private val BrandLight = lightColorScheme(
    primary = Color(0xFF5548BD),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE4DFFF),
    onPrimaryContainer = Color(0xFF140067),
    secondary = Color(0xFF5E5C71),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE4DFF9),
    onSecondaryContainer = Color(0xFF1B192C),
    tertiary = Color(0xFF7A5366),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFD8E8),
    onTertiaryContainer = Color(0xFF2F1122),
    background = Color(0xFFFBF8FF),
    onBackground = Color(0xFF1B1B21),
    surface = Color(0xFFFBF8FF),
    onSurface = Color(0xFF1B1B21),
    surfaceVariant = Color(0xFFE4E1EC),
    onSurfaceVariant = Color(0xFF47464F),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF5F2FA),
    surfaceContainer = Color(0xFFEFECF4),
    surfaceContainerHigh = Color(0xFFE9E7EF),
    surfaceContainerHighest = Color(0xFFE4E1E9),
    outline = Color(0xFF787680),
    outlineVariant = Color(0xFFC8C5D0),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

private fun ColorScheme.pureBlack(): ColorScheme = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF0B0B0B),
    surfaceContainer = Color(0xFF121212),
    surfaceContainerHigh = Color(0xFF1A1A1A),
    surfaceContainerHighest = Color(0xFF222222),
)

@Composable
fun isAppInDarkTheme(): Boolean = when (App.settings.theme) {
    ThemeMode.System -> isSystemInDarkTheme()
    ThemeMode.Light -> false
    ThemeMode.Dark -> true
}

@Composable
fun SeerrTheme(content: @Composable () -> Unit) {
    val s = App.settings
    val dark = isAppInDarkTheme()
    val ctx = LocalContext.current
    var scheme = when {
        s.dynamicColor -> if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        dark -> BrandDark
        else -> BrandLight
    }
    if (dark && s.pureBlack) scheme = scheme.pureBlack()

    CompositionLocalProvider(LocalStatusColors provides if (dark) DarkStatus else LightStatus) {
        MaterialTheme(colorScheme = scheme, shapes = AppShapes, content = content)
    }
}
