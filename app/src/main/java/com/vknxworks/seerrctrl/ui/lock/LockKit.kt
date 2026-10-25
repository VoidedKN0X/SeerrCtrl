package com.vknxworks.seerrctrl.ui.lock

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import com.vknxworks.seerrctrl.ui.components.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.vknxworks.seerrctrl.data.lock.LockVault
import com.vknxworks.seerrctrl.R
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.em
import com.vknxworks.seerrctrl.ui.components.Button
import com.vknxworks.seerrctrl.ui.theme.CtrlColors
import com.vknxworks.seerrctrl.ui.theme.CtrlType
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

// The look and flow of the shared Ctrl series app lock (as in MemoCtrl): the lock screen, choosing,
// changing and confirming the app PIN. Each app keeps its own colours through its theme.

/** What a PIN screen on top of the app is for. */
enum class PinRequest { ENABLE, CHANGE, DISABLE }

/** The PIN screen shown on top of the app (from Settings), or null. Cleared when the app locks. */
object PinRequests {
    var request by mutableStateOf<PinRequest?>(null)
}

/** PINs that are too easy to guess: one digit repeated, or a straight run up or down. */
fun isObviousPin(pin: String): Boolean =
    pin.toSet().size == 1 || "0123456789012".contains(pin) || "9876543210987".contains(pin)

internal tailrec fun Context.lockActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.lockActivity()
    else -> null
}

/** The wrong-PIN shake and buzz. */
internal fun CoroutineScope.shakePin(shake: Animatable<Float, AnimationVector1D>, haptics: HapticFeedback) {
    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    launch { for (x in listOf(18f, -14f, 10f, -6f, 0f)) shake.animateTo(x, tween(45)) }
}

/** "Too many tries" with the seconds left. */
@Composable
fun lockoutText(ms: Long) = stringResource(R.string.lock_too_many, ((ms + 999) / 1000).toInt())

/** 72 dp accent rounded square with the lock icon. */
@Composable
fun LockBadge() {
    Box(
        Modifier.size(72.dp).clip(RoundedCornerShape(24.dp)).background(CtrlColors.Accent),
        contentAlignment = Alignment.Center,
    ) { Icon(Icons.Outlined.Lock, null, Modifier.size(30.dp), tint = CtrlColors.OnAccent) }
}

/**
 * "Forgot PIN?" under the lock screen's PIN pad. Fingerprint unlock is the outlined key at the bottom left
 * of the pad itself ([PinPadScreen]'s onFingerprint).
 */
@Composable
fun LockScreenButtons(onForgot: () -> Unit) {
    TextButton(onClick = onForgot) { Text(stringResource(R.string.lock_forgot), color = CtrlColors.Muted) }
}

/** Asks before "Forgot PIN?" resets the app. [body] says what's removed. */
@Composable
fun ForgotPinDialog(body: String, confirm: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.lock_forgot_title)) },
        text = { Text(body) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirm, color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.lock_cancel)) } },
    )
}

/**
 * Choosing a PIN (twice), changing it (current, then new twice) or confirming it to turn the lock off.
 * [onDone] runs in a coroutine; a failure shows "Couldn't save the PIN".
 */
@Composable
fun PinSetupScreen(
    vault: LockVault,
    title: String,
    intro: String,
    verifyCurrent: Boolean,
    cancellable: Boolean,
    onDone: suspend (String) -> Unit,
    onCancel: () -> Unit,
    verifyOnly: Boolean = false,
) {
    val context = LocalContext.current
    val activity = context.lockActivity()
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val shake = remember { Animatable(0f) }
    // 0 = current PIN, 1 = new PIN, 2 = repeat new PIN
    var step by remember { mutableIntStateOf(if (verifyCurrent) 0 else 1) }
    var pin by remember { mutableStateOf("") }
    var first by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var lockout by remember { mutableLongStateOf(vault.lockoutRemaining()) }

    LaunchedEffect(lockout > 0) {
        while (lockout > 0) {
            delay(500)
            lockout = vault.lockoutRemaining()
        }
    }

    BackHandler { if (cancellable) onCancel() else activity?.moveTaskToBack(true) }

    fun wrong(text: String) {
        message = text
        pin = ""
        scope.shakePin(shake, haptics)
    }

    fun finish(newPin: String) {
        busy = true
        scope.launch {
            runCatching { onDone(newPin) }.onFailure { busy = false; wrong(context.getString(R.string.lock_save_failed)) }
        }
    }

    fun next() {
        if (busy || lockout > 0) return
        when (step) {
            0 -> {
                if (pin.length < LockVault.PIN_MIN) return
                val entered = pin
                busy = true
                scope.launch {
                    // Slow on purpose (it makes guessing slow too), and counts towards the wrong-PIN wait.
                    val result = withContext(Dispatchers.Default) { vault.unlockWithPin(entered) }
                    busy = false
                    if (result == LockVault.PinResult.Ok) {
                        pin = ""
                        message = null
                        if (verifyOnly) finish(entered) else step = 1
                    } else {
                        lockout = vault.lockoutRemaining()
                        wrong(context.getString(R.string.lock_wrong_pin))
                    }
                }
            }
            1 -> {
                if (pin.length < LockVault.PIN_MIN) return
                if (isObviousPin(pin)) {
                    wrong(context.getString(R.string.lock_too_easy))
                    return
                }
                first = pin
                pin = ""
                message = null
                step = 2
            }
            else -> {
                if (pin != first) {
                    first = ""
                    step = 1
                    wrong(context.getString(R.string.lock_mismatch))
                    return
                }
                finish(pin)
            }
        }
    }

    PinPadScreen(
        title = title,
        message = when {
            lockout > 0 -> lockoutText(lockout)
            message != null -> message!!
            step == 0 -> stringResource(R.string.lock_current_pin)
            step == 1 -> intro
            else -> stringResource(R.string.lock_repeat_pin)
        },
        isError = lockout > 0 || message != null,
        pin = pin,
        shake = shake.value,
        checking = busy,
        enabled = !busy && lockout == 0L,
        onDigit = { d -> if (pin.length < LockVault.PIN_MAX) { message = null; pin += d } },
        onBackspace = { pin = pin.dropLast(1) },
        onConfirm = ::next,
        showConfirm = pin.length >= LockVault.PIN_MIN,
        confirmLabel = stringResource(if (step == 2 || (step == 0 && verifyOnly)) R.string.lock_done else R.string.lock_next),
        bottom = { if (cancellable) TextButton(onClick = onCancel) { Text(stringResource(R.string.lock_cancel)) } },
    )
}

/**
 * The PIN pad screen every lock screen and PIN step uses, in the Ctrl / Signal look: accent lock badge,
 * "VKNXWORKS · CTRL", a big title, square PIN cells and a three-column keypad. [onFingerprint] puts the
 * outlined fingerprint key at the bottom left.
 */
@Composable
fun PinPadScreen(
    title: String,
    message: String,
    isError: Boolean,
    pin: String,
    shake: Float,
    checking: Boolean,
    enabled: Boolean,
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    onConfirm: () -> Unit,
    showConfirm: Boolean,
    confirmLabel: String = stringResource(R.string.lock_ok),
    minDots: Int = LockVault.PIN_MIN,
    onFingerprint: (() -> Unit)? = null,
    bottom: @Composable () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    // The PIN pad is never in screenshots, screen recordings or casting, whatever the app's setting says:
    // a recording would show which keys were pressed.
    KeepOutOfScreenshots()
    Surface(Modifier.fillMaxSize(), color = CtrlColors.Background) {
        Column(
            Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState())
                .padding(start = 32.dp, end = 32.dp, top = 40.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LockBadge()
            Spacer(Modifier.height(28.dp))
            Text(stringResource(R.string.lock_brand), style = CtrlType.Label.copy(letterSpacing = 0.18.em), color = CtrlColors.Muted)
            Spacer(Modifier.height(8.dp))
            Text(
                title,
                style = MaterialTheme.typography.displaySmall,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isError) CtrlColors.Error else CtrlColors.Muted,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 340.dp),
            )
            Spacer(Modifier.height(28.dp))
            val dots = maxOf(pin.length, minDots)
            Box(
                Modifier.height(20.dp).offset { IntOffset(shake.roundToInt(), 0) }
                    .semantics { contentDescription = "${pin.length} / $dots" },
                contentAlignment = Alignment.Center,
            ) {
                if (checking) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = CtrlColors.Accent)
                else Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    repeat(dots) { i ->
                        val cell = Modifier.size(14.dp).clip(RoundedCornerShape(5.dp))
                        if (i < pin.length) Box(cell.background(CtrlColors.Accent))
                        else Box(cell.border(2.dp, CtrlColors.PinEmpty, RoundedCornerShape(5.dp)))
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
            Column(
                Modifier.widthIn(max = 360.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                listOf("123", "456", "789").forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { d ->
                            DigitKey(d.toString(), enabled, Modifier.weight(1f)) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onDigit(d)
                            }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (onFingerprint != null) {
                        PadKey(
                            Modifier.weight(1f).border(1.dp, CtrlColors.Accent, KeyShape),
                            enabled = true, onClick = onFingerprint,
                            label = stringResource(R.string.lock_use_fingerprint),
                        ) { Icon(Icons.Outlined.Fingerprint, null, Modifier.size(26.dp), tint = CtrlColors.Accent) }
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                    DigitKey("0", enabled, Modifier.weight(1f)) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onDigit('0')
                    }
                    PadKey(
                        Modifier.weight(1f), enabled = enabled && pin.isNotEmpty(), onClick = onBackspace,
                        label = stringResource(R.string.lock_delete),
                    ) { Icon(Icons.AutoMirrored.Outlined.Backspace, null, Modifier.size(24.dp), tint = CtrlColors.Muted) }
                }
                if (showConfirm) {
                    Button(
                        onClick = onConfirm, enabled = enabled,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                    ) { Text(confirmLabel, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold) }
                }
            }
            Spacer(Modifier.height(12.dp))
            bottom()
        }
    }
}

private val KeyShape = RoundedCornerShape(22.dp)

@Composable
private fun DigitKey(label: String, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    PadKey(
        modifier.background(CtrlColors.Surface, KeyShape).border(1.dp, CtrlColors.Outline, KeyShape),
        enabled = enabled, onClick = onClick, label = null,
    ) { Text(label, style = CtrlType.Key, color = CtrlColors.Text) }
}

@Composable
private fun PadKey(modifier: Modifier, enabled: Boolean, onClick: () -> Unit, label: String?, content: @Composable () -> Unit) {
    Box(
        modifier
            .height(68.dp)
            .clip(KeyShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .then(if (label != null) Modifier.semantics { contentDescription = label } else Modifier)
            .alpha(if (enabled) 1f else 0.4f),
        contentAlignment = Alignment.Center,
    ) { content() }
}
