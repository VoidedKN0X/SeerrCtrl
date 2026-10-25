package com.vknxworks.seerrctrl.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.vknxworks.seerrctrl.ui.theme.CtrlColors
import com.vknxworks.seerrctrl.ui.theme.CtrlMaterialShapes
import com.vknxworks.seerrctrl.ui.theme.CtrlPalette
import com.vknxworks.seerrctrl.ui.theme.CtrlTypography
import com.vknxworks.seerrctrl.ui.theme.LocalCtrlColors
import com.vknxworks.seerrctrl.ui.theme.ctrlColorScheme

/** Colours for availability and request states, shown as mono status words with a square dot. */
@Immutable
data class StatusColors(
    val available: Color,
    val partial: Color,
    val processing: Color,
    val pending: Color,
    val declined: Color,
    val inactive: Color,
    val onBadge: Color,
)

private fun statusColors(c: CtrlPalette) = StatusColors(
    available = c.accentText,
    partial = c.accentText,
    processing = c.text,
    pending = c.inactive,
    declined = c.error,
    inactive = c.inactive,
    onBadge = c.onAccent,
)

val LocalStatusColors = staticCompositionLocalOf { statusColors(CtrlColors.Dark) }

val Status: StatusColors
    @Composable get() = LocalStatusColors.current

/** Follows the device's dark/light setting; there's no theme setting in the app and no wallpaper colours. */
@Composable
fun isAppInDarkTheme(): Boolean = isSystemInDarkTheme()

@Composable
fun SeerrTheme(content: @Composable () -> Unit) {
    val palette = if (isAppInDarkTheme()) CtrlColors.Dark else CtrlColors.Light
    val scheme = remember(palette) { ctrlColorScheme(palette) }
    val status = remember(palette) { statusColors(palette) }
    CompositionLocalProvider(LocalCtrlColors provides palette, LocalStatusColors provides status) {
        MaterialTheme(colorScheme = scheme, shapes = CtrlMaterialShapes, typography = CtrlTypography, content = content)
    }
}
