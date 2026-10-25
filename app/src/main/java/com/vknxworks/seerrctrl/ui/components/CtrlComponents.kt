package com.vknxworks.seerrctrl.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vknxworks.seerrctrl.ui.pressScale
import com.vknxworks.seerrctrl.ui.theme.CtrlColors
import com.vknxworks.seerrctrl.ui.theme.CtrlShapes
import com.vknxworks.seerrctrl.ui.theme.CtrlType

// The shared building blocks of the Ctrl / Signal look.

/** "SEERR/CTRL": the app's name in mono capitals with the slash in the accent colour. */
@Composable
fun CtrlWordmark(name: String, modifier: Modifier = Modifier) {
    Text(
        buildAnnotatedString {
            append(name.uppercase())
            withStyle(SpanStyle(color = CtrlColors.Accent)) { append("/") }
            append("CTRL")
        },
        style = CtrlType.Wordmark, color = CtrlColors.Text, modifier = modifier,
    )
}

/** A mono, upper-case section label ("PRIVACY", "NODE · ONLINE"). */
@Composable
fun CtrlLabel(text: String, modifier: Modifier = Modifier, color: Color = CtrlColors.Muted, strong: Boolean = false) {
    Text(
        text.uppercase(), style = if (strong) CtrlType.LabelStrong else CtrlType.Label, color = color,
        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = modifier,
    )
}

/**
 * Top bar of the main tabs: the wordmark on the left and [trailing] (the profile picture) on the right.
 * No filled app bar.
 */
@Composable
fun CtrlTopBar(name: String, modifier: Modifier = Modifier, trailing: @Composable RowScope.() -> Unit = {}) {
    Row(
        modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 4.dp).height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CtrlWordmark(name, Modifier.weight(1f))
        trailing()
    }
}

/** A 44 dp square button (back, close, small actions) in the Signal style: surface fill, thin border. */
@Composable
fun CtrlSquareButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    background: Color = CtrlColors.Surface,
    tint: Color = CtrlColors.Text,
    enabled: Boolean = true,
) {
    val src = remember { MutableInteractionSource() }
    Box(
        modifier
            .size(44.dp)
            .pressScale(src, 0.92f)
            .clip(CtrlShapes.Control)
            .background(background)
            .border(1.dp, CtrlColors.ControlOutline, CtrlShapes.Control)
            .clickable(src, indication = androidx.compose.material3.ripple(), enabled = enabled, role = Role.Button, onClick = onClick)
            .alpha(if (enabled) 1f else 0.4f),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, contentDescription, Modifier.size(20.dp), tint = tint) }
}

/** Secondary card: surface fill, 1 dp border, 22 dp corners. */
@Composable
fun CtrlCard(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .clip(CtrlShapes.Card)
            .background(CtrlColors.Surface)
            .border(1.dp, CtrlColors.Outline, CtrlShapes.Card)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(padding),
        content = content,
    )
}

/**
 * The one most important thing on a screen: accent fill, dark content, a mono label row on top and a giant
 * number below.
 */
@Composable
fun CtrlHeroCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    trailingLabel: String? = null,
    unit: String? = null,
    onClick: (() -> Unit)? = null,
    footer: (@Composable ColumnScope.() -> Unit)? = null,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(CtrlShapes.Hero)
            .background(CtrlColors.Accent)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CtrlLabel(label, Modifier.weight(1f), color = CtrlColors.OnAccent, strong = true)
            if (trailingLabel != null) CtrlLabel(trailingLabel, color = CtrlColors.OnAccent, strong = true)
        }
        Text(
            buildAnnotatedString {
                append(value)
                if (unit != null) withStyle(SpanStyle(fontSize = 28.sp)) { append(unit) }
            },
            style = CtrlType.BigNumber, color = CtrlColors.OnAccent, maxLines = 1,
        )
        if (footer != null) CompositionLocalProvider(LocalContentColor provides CtrlColors.OnAccent) { footer() }
    }
}

/** 8 dp square status dot. */
@Composable
fun CtrlDot(color: Color, modifier: Modifier = Modifier, size: Dp = 8.dp) {
    Box(modifier.size(size).clip(CtrlShapes.Dot).background(color))
}

/** Mono status word with a square dot ("● AVAILABLE"), as at the end of list rows. */
@Composable
fun CtrlStatus(text: String, color: Color, modifier: Modifier = Modifier, dotFirst: Boolean = false) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (dotFirst) CtrlDot(color)
        Text(text.uppercase(), style = CtrlType.Status, color = color, maxLines = 1)
        if (!dotFirst) CtrlDot(color)
    }
}

/**
 * Rounded-rect switch, 52 × 32 dp: accent track with a dark knob when on, grey when off. Announced as a
 * switch to TalkBack.
 */
@Composable
fun CtrlSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val track by animateColorAsState(if (checked) CtrlColors.Accent else CtrlColors.SwitchOffTrack, label = "track")
    val knob by animateColorAsState(if (checked) CtrlColors.OnAccent else CtrlColors.Inactive, label = "knob")
    val x by animateDpAsState(if (checked) 20.dp else 0.dp, label = "knobX")
    val toggle = if (onCheckedChange != null) {
        Modifier.toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
    } else Modifier
    Box(
        modifier
            .then(toggle)
            .alpha(if (enabled) 1f else 0.4f)
            .size(52.dp, 32.dp)
            .clip(RoundedCornerShape10)
            .background(track)
            .padding(4.dp),
    ) {
        Box(Modifier.offset(x = x).size(24.dp).clip(RoundedCornerShape7).background(knob))
    }
}

private val RoundedCornerShape10 = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
private val RoundedCornerShape7 = androidx.compose.foundation.shape.RoundedCornerShape(7.dp)

/** 6 dp progress bar: dark track, light fill (or the accent for the main figure). */
@Composable
fun CtrlProgress(progress: () -> Float, modifier: Modifier = Modifier, color: Color = CtrlColors.Text) {
    val p = progress().coerceIn(0f, 1f)
    Box(modifier.height(6.dp).clip(RoundedCornerShape3).background(CtrlColors.ProgressTrack)) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(p).clip(RoundedCornerShape3).background(color))
    }
}

private val RoundedCornerShape3 = androidx.compose.foundation.shape.RoundedCornerShape(3.dp)

data class CtrlTab(val index: Int, val icon: ImageVector, val label: String, val badge: Int = 0)

/**
 * Floating bottom bar: 64 dp tall, 20 dp from the sides and 28 dp above the navigation bar. The selected tab
 * is an accent block with icon and label; the others are icons only.
 */
@Composable
fun CtrlBottomBar(
    tabs: List<CtrlTab>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    Box(
        modifier.fillMaxWidth().navigationBarsPadding().padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Row(
            Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .height(64.dp)
                .clip(CtrlShapes.Card)
                .background(CtrlColors.BarFill)
                .border(1.dp, CtrlColors.ControlOutline, CtrlShapes.Card)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEach { t ->
                val isSel = t.index == selected
                val src = remember { MutableInteractionSource() }
                val bg by animateColorAsState(if (isSel) CtrlColors.Accent else Color.Transparent, label = "tabBg")
                val fg by animateColorAsState(if (isSel) CtrlColors.OnAccent else CtrlColors.Muted, label = "tabFg")
                Row(
                    Modifier
                        .height(48.dp)
                        .then(if (isSel) Modifier else Modifier.width(48.dp))
                        .pressScale(src, 0.9f)
                        .clip(CtrlShapes.Button)
                        .background(bg)
                        .semantics(mergeDescendants = true) { this.selected = isSel; role = Role.Tab; contentDescription = t.label }
                        .clickable(src, indication = null) {
                            if (!isSel) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelect(t.index)
                        }
                        .padding(horizontal = if (isSel) 16.dp else 0.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BadgedBox(badge = {
                        if (t.badge > 0) Badge { Text(if (t.badge > 99) "99+" else "${t.badge}") }
                    }) {
                        Icon(t.icon, null, Modifier.size(if (isSel) 18.dp else 21.dp), tint = fg)
                    }
                    if (isSel) {
                        Spacer(Modifier.width(8.dp))
                        Text(t.label, color = fg, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Clip)
                    }
                }
            }
        }
    }
}

/** Bottom padding that keeps content clear of [CtrlBottomBar] (64 dp + 28 dp margin + the navigation bar). */
val CtrlBottomBarClearance: Dp = 148.dp
