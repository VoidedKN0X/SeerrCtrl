package com.vknxworks.seerrctrl.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.vknxworks.seerrctrl.R

// "Ctrl / Signal": the look shared by every Ctrl app. Dark and light palettes that follow the device, one
// accent colour, Space Grotesk for text and JetBrains Mono for numbers, labels and the wordmark.

/** One Signal palette. [CtrlColors.Dark] and [CtrlColors.Light] are the two the app switches between. */
@Immutable
data class CtrlPalette(
    val isDark: Boolean,
    /** Accent as a fill: hero cards, primary buttons, the selected tab, switch tracks, PIN cells. */
    val accent: Color,
    val onAccent: Color,
    /** Accent for text and icons on the background (status words, links, the wordmark slash, focus). */
    val accentText: Color,
    val background: Color,
    val surface: Color,
    val raised: Color,
    val outline: Color,
    val controlOutline: Color,
    val divider: Color,
    val text: Color,
    val muted: Color,
    val inactive: Color,
    val error: Color,
    val onError: Color,
    val switchOffTrack: Color,
    val progressTrack: Color,
    val pinEmpty: Color,
    val barFill: Color,
)

val LocalCtrlColors = staticCompositionLocalOf { CtrlColors.Dark }

/**
 * Every colour of the Signal look. [AccentFill] is the one accent of the whole series (change it here to
 * recolour the app); the rest come from the current palette ([Dark] or [Light], following the device).
 */
object CtrlColors {
    val AccentFill = Color(0xFFB48CFF)
    private val OnAccentFill = Color(0xFF08080A)

    val Dark = CtrlPalette(
        isDark = true,
        accent = AccentFill,
        onAccent = OnAccentFill,
        accentText = AccentFill,
        background = Color(0xFF08080A),
        surface = Color(0xFF141417),
        raised = Color(0xFF1C1C21),
        outline = Color(0xFF232328),
        controlOutline = Color(0xFF2A2A2F),
        divider = Color(0xFF1E1E23),
        text = Color(0xFFF4F4F2),
        muted = Color(0xFFA3A3AB),
        inactive = Color(0xFF8C8C94),
        error = Color(0xFFFF6B6B),
        onError = Color(0xFF08080A),
        switchOffTrack = Color(0xFF2A2A30),
        progressTrack = Color(0xFF26262C),
        pinEmpty = Color(0xFF3A3A41),
        barFill = Color(0xEB141417), // surface at ~92 %
    )

    val Light = CtrlPalette(
        isDark = false,
        accent = AccentFill,
        onAccent = OnAccentFill,
        // The fill is too pale for text on white: a deeper shade of the same violet.
        accentText = Color(0xFF6A3BDB),
        background = Color(0xFFF5F5F1),
        surface = Color(0xFFFFFFFF),
        raised = Color(0xFFECECE7),
        outline = Color(0xFFDEDED7),
        controlOutline = Color(0xFFD2D2CA),
        divider = Color(0xFFE6E6E0),
        text = Color(0xFF0B0B0D),
        muted = Color(0xFF5C5C64),
        inactive = Color(0xFF74747C),
        error = Color(0xFFD93636),
        onError = Color(0xFFFFFFFF),
        switchOffTrack = Color(0xFFDCDCD5),
        progressTrack = Color(0xFFE2E2DC),
        pinEmpty = Color(0xFFC9C9C1),
        barFill = Color(0xEBFFFFFF), // surface at ~92 %
    )

    private val p: CtrlPalette @Composable @ReadOnlyComposable get() = LocalCtrlColors.current

    val Accent: Color @Composable @ReadOnlyComposable get() = p.accent
    val OnAccent: Color @Composable @ReadOnlyComposable get() = p.onAccent
    val AccentText: Color @Composable @ReadOnlyComposable get() = p.accentText
    val Background: Color @Composable @ReadOnlyComposable get() = p.background
    val Surface: Color @Composable @ReadOnlyComposable get() = p.surface
    val Raised: Color @Composable @ReadOnlyComposable get() = p.raised
    val Outline: Color @Composable @ReadOnlyComposable get() = p.outline
    val ControlOutline: Color @Composable @ReadOnlyComposable get() = p.controlOutline
    val Divider: Color @Composable @ReadOnlyComposable get() = p.divider
    val Text: Color @Composable @ReadOnlyComposable get() = p.text
    val Muted: Color @Composable @ReadOnlyComposable get() = p.muted
    val Inactive: Color @Composable @ReadOnlyComposable get() = p.inactive
    val Error: Color @Composable @ReadOnlyComposable get() = p.error
    val OnError: Color @Composable @ReadOnlyComposable get() = p.onError
    val SwitchOffTrack: Color @Composable @ReadOnlyComposable get() = p.switchOffTrack
    /** Switch knob when off: the inactive grey. */
    val SwitchOffKnob: Color @Composable @ReadOnlyComposable get() = p.inactive
    val ProgressTrack: Color @Composable @ReadOnlyComposable get() = p.progressTrack
    val PinEmpty: Color @Composable @ReadOnlyComposable get() = p.pinEmpty
    val BarFill: Color @Composable @ReadOnlyComposable get() = p.barFill
}

/** Bundled fonts (SIL OFL 1.1, licences in assets/licenses). */
object CtrlFonts {
    val Grotesk = FontFamily(
        Font(R.font.space_grotesk_regular, FontWeight.Normal),
        Font(R.font.space_grotesk_medium, FontWeight.Medium),
        Font(R.font.space_grotesk_semibold, FontWeight.SemiBold),
        Font(R.font.space_grotesk_bold, FontWeight.Bold),
    )
    val Mono = FontFamily(
        Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
        Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
        Font(R.font.jetbrains_mono_semibold, FontWeight.SemiBold),
    )
}

/** Text styles of the Signal look that aren't one of Material's slots. */
object CtrlType {
    /** Section labels: mono, upper case (call [String.uppercase]), wide spacing, muted. */
    val Label = TextStyle(fontFamily = CtrlFonts.Mono, fontSize = 11.sp, letterSpacing = 0.16.em, fontWeight = FontWeight.Normal)
    val LabelStrong = Label.copy(fontWeight = FontWeight.SemiBold)
    /** Data readouts (server addresses, dates, counts) in a list or under a title. */
    val Data = TextStyle(fontFamily = CtrlFonts.Mono, fontSize = 12.sp, fontWeight = FontWeight.Normal)
    /** Trailing status words in lists ("PENDING", "AVAILABLE"). */
    val Status = TextStyle(fontFamily = CtrlFonts.Mono, fontSize = 11.sp, letterSpacing = 0.1.em, fontWeight = FontWeight.Medium)
    val Wordmark = TextStyle(fontFamily = CtrlFonts.Mono, fontSize = 13.sp, letterSpacing = 0.14.em, fontWeight = FontWeight.SemiBold)
    val Hero = TextStyle(fontFamily = CtrlFonts.Grotesk, fontSize = 56.sp, letterSpacing = (-0.04).em, fontWeight = FontWeight.Bold, lineHeight = 54.sp)
    val Headline = TextStyle(fontFamily = CtrlFonts.Grotesk, fontSize = 40.sp, letterSpacing = (-0.035).em, fontWeight = FontWeight.Bold, lineHeight = 42.sp)
    val BigNumber = TextStyle(fontFamily = CtrlFonts.Grotesk, fontSize = 64.sp, letterSpacing = (-0.05).em, fontWeight = FontWeight.Bold, lineHeight = 60.sp)
    val Key = TextStyle(fontFamily = CtrlFonts.Mono, fontSize = 26.sp, fontWeight = FontWeight.Medium)
}

object CtrlShapes {
    val Card = RoundedCornerShape(22.dp)
    val Hero = RoundedCornerShape(26.dp)
    val Control = RoundedCornerShape(14.dp)
    val Button = RoundedCornerShape(16.dp)
    val Field = RoundedCornerShape(16.dp)
    val Chip = RoundedCornerShape(10.dp)
    val Badge = RoundedCornerShape(9.dp)
    val Dot = RoundedCornerShape(2.dp)
    val Poster = RoundedCornerShape(14.dp)
    val Dialog = RoundedCornerShape(26.dp)
}

/** Material's colour slots filled from a Signal palette, so stock Material widgets match too. */
internal fun ctrlColorScheme(c: CtrlPalette): ColorScheme {
    val slots: ColorScheme = if (c.isDark) darkColorScheme() else lightColorScheme()
    return slots.copy(
        // Material uses "primary" for text, icons and small marks (checkboxes, sliders, the selected
        // calendar day), so it gets the readable accent; fills that need the light violet use the containers.
        primary = c.accentText,
        onPrimary = if (c.isDark) c.onAccent else Color.White,
        primaryContainer = c.accent,
        onPrimaryContainer = c.onAccent,
        inversePrimary = c.accent,
        secondary = c.text,
        onSecondary = c.background,
        // Selected filter chips and other "selected" fills use the accent too.
        secondaryContainer = c.accent,
        onSecondaryContainer = c.onAccent,
        tertiary = c.accentText,
        onTertiary = if (c.isDark) c.onAccent else Color.White,
        tertiaryContainer = c.raised,
        onTertiaryContainer = c.text,
        background = c.background,
        onBackground = c.text,
        // Material draws plain list rows, tab rows and app bars in "surface", so that stays the page colour;
        // cards, sheets, dialogs and menus use the surfaceContainer slots below.
        surface = c.background,
        onSurface = c.text,
        surfaceVariant = c.raised,
        onSurfaceVariant = c.muted,
        surfaceTint = Color.Transparent,
        surfaceBright = c.raised,
        surfaceDim = c.background,
        surfaceContainerLowest = c.background,
        surfaceContainerLow = c.surface,
        surfaceContainer = c.surface,
        surfaceContainerHigh = c.raised,
        surfaceContainerHighest = c.outline,
        inverseSurface = c.text,
        inverseOnSurface = c.background,
        outline = c.controlOutline,
        outlineVariant = c.divider,
        error = c.error,
        onError = c.onError,
        errorContainer = c.error,
        onErrorContainer = c.onError,
        scrim = Color.Black,
    )
}

internal val CtrlMaterialShapes = Shapes(
    extraSmall = CtrlShapes.Badge,
    small = CtrlShapes.Chip,
    medium = CtrlShapes.Poster,
    large = CtrlShapes.Card,
    extraLarge = CtrlShapes.Dialog,
)

private fun TextStyle.sg(size: TextUnit, weight: FontWeight, spacing: TextUnit = 0.sp, line: TextUnit = TextUnit.Unspecified) = copy(
    fontFamily = CtrlFonts.Grotesk, fontSize = size, fontWeight = weight, letterSpacing = spacing,
    lineHeight = if (line == TextUnit.Unspecified) lineHeight else line,
)

internal val CtrlTypography: Typography = Typography().let { t ->
    Typography(
        displayLarge = t.displayLarge.sg(56.sp, FontWeight.Bold, (-0.04).em, 54.sp),
        displayMedium = t.displayMedium.sg(46.sp, FontWeight.Bold, (-0.04).em, 46.sp),
        displaySmall = t.displaySmall.sg(36.sp, FontWeight.Bold, (-0.035).em, 38.sp),
        headlineLarge = t.headlineLarge.sg(40.sp, FontWeight.Bold, (-0.035).em, 42.sp),
        headlineMedium = t.headlineMedium.sg(30.sp, FontWeight.Bold, (-0.03).em, 34.sp),
        headlineSmall = t.headlineSmall.sg(24.sp, FontWeight.Bold, (-0.02).em, 28.sp),
        titleLarge = t.titleLarge.sg(20.sp, FontWeight.SemiBold, (-0.01).em),
        titleMedium = t.titleMedium.sg(18.sp, FontWeight.SemiBold),
        titleSmall = t.titleSmall.sg(15.sp, FontWeight.SemiBold),
        bodyLarge = t.bodyLarge.sg(16.sp, FontWeight.Normal),
        bodyMedium = t.bodyMedium.sg(14.sp, FontWeight.Normal),
        bodySmall = t.bodySmall.sg(12.sp, FontWeight.Normal),
        labelLarge = t.labelLarge.sg(14.sp, FontWeight.SemiBold),
        labelMedium = t.labelMedium.sg(12.sp, FontWeight.Medium),
        labelSmall = t.labelSmall.sg(11.sp, FontWeight.Medium),
    )
}
