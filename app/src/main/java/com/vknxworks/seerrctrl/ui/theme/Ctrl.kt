package com.vknxworks.seerrctrl.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
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

// "Ctrl / Signal": the look shared by every Ctrl app. Dark only, one accent colour, Space Grotesk for text
// and JetBrains Mono for numbers, labels and the wordmark.

/** Every colour of the Signal look. Change [Accent] to recolour the whole app. */
object CtrlColors {
    val Accent = Color(0xFFB48CFF)
    val OnAccent = Color(0xFF08080A)

    val Background = Color(0xFF08080A)
    val Surface = Color(0xFF141417)
    val Raised = Color(0xFF1C1C21)
    val Outline = Color(0xFF232328)
    val ControlOutline = Color(0xFF2A2A2F)
    val Divider = Color(0xFF1E1E23)
    val Text = Color(0xFFF4F4F2)
    val Muted = Color(0xFFA3A3AB)
    val Inactive = Color(0xFF8C8C94)
    val Error = Color(0xFFFF6B6B)

    val SwitchOffTrack = Color(0xFF2A2A30)
    val ProgressTrack = Color(0xFF26262C)
    val PinEmpty = Color(0xFF3A3A41)
    val BarFill = Color(0xEB141417) // Surface at ~92 %
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

internal val CtrlColorScheme = darkColorScheme(
    primary = CtrlColors.Accent,
    onPrimary = CtrlColors.OnAccent,
    primaryContainer = CtrlColors.Accent,
    onPrimaryContainer = CtrlColors.OnAccent,
    inversePrimary = CtrlColors.Accent,
    secondary = CtrlColors.Text,
    onSecondary = CtrlColors.OnAccent,
    // Selected filter chips and other "selected" fills use the accent too.
    secondaryContainer = CtrlColors.Accent,
    onSecondaryContainer = CtrlColors.OnAccent,
    tertiary = CtrlColors.Text,
    onTertiary = CtrlColors.OnAccent,
    tertiaryContainer = CtrlColors.Raised,
    onTertiaryContainer = CtrlColors.Text,
    background = CtrlColors.Background,
    onBackground = CtrlColors.Text,
    // Material draws plain list rows, tab rows and app bars in "surface", so that stays the page colour;
    // cards, sheets, dialogs and menus use the surfaceContainer slots below (#141417).
    surface = CtrlColors.Background,
    onSurface = CtrlColors.Text,
    surfaceVariant = CtrlColors.Raised,
    onSurfaceVariant = CtrlColors.Muted,
    surfaceTint = Color.Transparent,
    surfaceBright = CtrlColors.Raised,
    surfaceDim = CtrlColors.Background,
    surfaceContainerLowest = CtrlColors.Background,
    surfaceContainerLow = CtrlColors.Surface,
    surfaceContainer = CtrlColors.Surface,
    surfaceContainerHigh = CtrlColors.Raised,
    surfaceContainerHighest = CtrlColors.Outline,
    inverseSurface = CtrlColors.Text,
    inverseOnSurface = CtrlColors.Background,
    outline = CtrlColors.ControlOutline,
    outlineVariant = CtrlColors.Divider,
    error = CtrlColors.Error,
    onError = CtrlColors.OnAccent,
    errorContainer = CtrlColors.Error,
    onErrorContainer = CtrlColors.OnAccent,
    scrim = Color.Black,
)

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
