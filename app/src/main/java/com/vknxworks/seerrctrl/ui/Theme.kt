package com.vknxworks.seerrctrl.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.vknxworks.seerrctrl.ui.theme.CtrlColorScheme
import com.vknxworks.seerrctrl.ui.theme.CtrlColors
import com.vknxworks.seerrctrl.ui.theme.CtrlMaterialShapes
import com.vknxworks.seerrctrl.ui.theme.CtrlTypography

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

private val SignalStatus = StatusColors(
    available = CtrlColors.Accent,
    partial = CtrlColors.Accent,
    processing = CtrlColors.Text,
    pending = CtrlColors.Inactive,
    declined = CtrlColors.Error,
    inactive = CtrlColors.Inactive,
    onBadge = CtrlColors.OnAccent,
)

val LocalStatusColors = staticCompositionLocalOf { SignalStatus }

val Status: StatusColors
    @Composable get() = LocalStatusColors.current

/** The Signal look is dark only (no light theme, no wallpaper colours). */
@Composable
fun isAppInDarkTheme(): Boolean = true

@Composable
fun SeerrTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalStatusColors provides SignalStatus) {
        MaterialTheme(colorScheme = CtrlColorScheme, shapes = CtrlMaterialShapes, typography = CtrlTypography, content = content)
    }
}
