package com.vknxworks.seerrctrl.ui.lock

import android.content.Context
import android.os.SystemClock
import javax.crypto.Cipher
import androidx.activity.compose.BackHandler
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vknxworks.seerrctrl.App
import com.vknxworks.seerrctrl.data.LockAgain
import com.vknxworks.seerrctrl.data.LockSettings
import com.vknxworks.seerrctrl.data.lock.LockVault
import com.vknxworks.seerrctrl.ui.pressScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import androidx.compose.ui.res.stringResource
import com.vknxworks.seerrctrl.R

const val PIN_MIN = LockVault.PIN_MIN
const val PIN_MAX = LockVault.PIN_MAX

/** Only class-3 ("strong") biometrics — weak face unlock can be fooled by a photo. */
fun biometricAvailable(context: Context): Boolean =
    BiometricManager.from(context).canAuthenticate(BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS

/**
 * The app itself is about to open another app (browser, trailer, calendar, settings). That trip doesn't count
 * as leaving, so coming straight back doesn't ask for the PIN again.
 */
object LockTrips {
    @Volatile var expectReturnAt = 0L
}

fun expectReturn() { LockTrips.expectReturnAt = SystemClock.elapsedRealtime() }

/**
 * Shows the fingerprint prompt for [cipher], a Keystore key from [LockVault] that only works right after a strong
 * match. [onSuccess] gets the cipher the match unlocked; a hooked "success" without it can't open anything.
 */
fun showBiometricPrompt(
    activity: FragmentActivity,
    title: String,
    cipher: Cipher,
    subtitle: String? = null,
    onError: (String?) -> Unit = {},
    onSuccess: (Cipher) -> Unit,
) {
    val prompt = BiometricPrompt(
        activity,
        ContextCompat.getMainExecutor(activity),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                val c = result.cryptoObject?.cipher
                if (c != null) onSuccess(c) else onError(activity.getString(R.string.lock_fingerprint_failed))
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                val cancelled = errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                    errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON || errorCode == BiometricPrompt.ERROR_CANCELED
                onError(if (cancelled) null else errString.toString())
            }
        },
    )
    prompt.authenticate(
        BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .apply { if (subtitle != null) setSubtitle(subtitle) }
            .setNegativeButtonText(activity.getString(R.string.lock_use_pin))
            .setAllowedAuthenticators(BIOMETRIC_STRONG)
            .build(),
        BiometricPrompt.CryptoObject(cipher),
    )
}

/** Whether the app is locked. Survives rotation; a fresh process always starts locked. */
class AppLockViewModel : ViewModel() {
    val settings = App.lock

    var locked by mutableStateOf(settings.pinSet)
        private set
    /** A string resource: why the last try didn't unlock. */
    var pinError by mutableStateOf<Int?>(null)
        private set
    var checking by mutableStateOf(false)
        private set
    var lockoutRemaining by mutableLongStateOf(0L)
        private set

    /** An older 4–5 digit (or too obvious) PIN was right: a new one has to be chosen before going on. */
    var mustChoosePin by mutableStateOf(false)
        private set

    /** Unlocked at least once in this process, so the app's screens may stay open underneath the lock. */
    var everUnlocked by mutableStateOf(!settings.pinSet)
        private set

    /** Set once the lock screen has shown the biometric prompt, so rotation doesn't show it twice. */
    var biometricShown = false

    private var backgroundedAt = 0L
    private var ticker: Job? = null

    init { refreshLockout() }

    /** The current trip away was started by the app itself (a link, trailer, settings). */
    private var exemptTrip = false

    fun onBackground() {
        val trip = LockTrips.expectReturnAt
        LockTrips.expectReturnAt = 0L
        backgroundedAt = SystemClock.elapsedRealtime()
        // Only right after the app opened something itself; leaving later still counts.
        exemptTrip = trip > 0 && backgroundedAt - trip < 5_000
        if (exemptTrip) return
        // "As soon as you leave" locks before the app is hidden, so the key is gone while it's in the background.
        if (settings.pinSet && settings.lockAgain == LockAgain.IMMEDIATELY) lockNow()
    }

    fun onForeground() {
        val since = backgroundedAt
        val exempt = exemptTrip
        backgroundedAt = 0L
        exemptTrip = false
        LockTrips.expectReturnAt = 0L
        val after = settings.lockAgain.millis
        if (!settings.pinSet || locked || since == 0L || after < 0) return
        // A trip the app started still locks if you stay away long: after 5 minutes, or the chosen time if longer.
        val limit = if (exempt) maxOf(after, 5 * 60_000L) else after
        if (SystemClock.elapsedRealtime() - since >= limit) lockNow()
    }

    fun lockNow() {
        if (settings.pinSet && !locked) {
            locked = true
            biometricShown = false
            pinError = null
            PinRequests.request = null
            App.onLocked()
        }
    }

    /** The vault is open (PIN or fingerprint): show the app. */
    private fun unlocked() {
        pinError = null
        App.onUnlocked()
        everUnlocked = true
        locked = false
    }

    /** "Forgot PIN?" finished: the lock is off and nothing is left to protect. */
    fun reset() {
        mustChoosePin = false
        pinError = null
        everUnlocked = true
        locked = false
    }

    fun unlockWithBiometric(cipher: Cipher): Boolean {
        if (!settings.vault.unlockWithBiometric(cipher)) return false
        unlocked()
        return true
    }

    fun submitPin(pin: String, onWrong: () -> Unit) {
        if (checking || settings.lockoutRemaining() > 0) { refreshLockout(); return }
        viewModelScope.launch {
            checking = true
            // Slow on purpose (so guessing is slow too); keep it off the main thread.
            val result = withContext(Dispatchers.Default) { settings.unlockWithPin(pin) }
            checking = false
            when (result) {
                LockSettings.Unlock.Ok -> unlocked()
                LockSettings.Unlock.NeedsNewPin -> { pinError = null; mustChoosePin = true }
                LockSettings.Unlock.Wrong, LockSettings.Unlock.LockedOut -> {
                    refreshLockout()
                    pinError = R.string.lock_wrong_pin
                    onWrong()
                }
            }
        }
    }

    /** Saves the new PIN chosen after an older short PIN. Throws when it couldn't. */
    suspend fun chooseNewPin(pin: String) {
        withContext(Dispatchers.Default) { App.enableLock(pin) }
        mustChoosePin = false
        unlocked()
    }

    /** Typing again clears "Wrong PIN". */
    fun clearError() {
        pinError = null
    }

    /** Counts down a wrong-PIN lockout (monotonic clock, so changing the phone's time doesn't end it). */
    private fun refreshLockout() {
        lockoutRemaining = settings.lockoutRemaining()
        if (lockoutRemaining <= 0 || ticker?.isActive == true) return
        ticker = viewModelScope.launch {
            while (true) {
                val left = settings.lockoutRemaining()
                lockoutRemaining = left
                if (left <= 0) break
                delay(500)
            }
        }
    }
}

/** The lock screen: PIN pad, fingerprint, and "Forgot PIN?" (the same look and flow as every Ctrl app). */
@Composable
fun LockScreen(vm: AppLockViewModel, onForgot: () -> Unit) {
    val vault = vm.settings.vault
    if (vm.mustChoosePin) {
        // An older short (or very obvious) PIN was right: choose a new one, twice, before going on.
        PinSetupScreen(
            vault = vault,
            title = stringResource(R.string.lock_choose_title),
            intro = stringResource(R.string.lock_migrate_intro, PIN_MIN),
            verifyCurrent = false,
            cancellable = false,
            onDone = { pin -> vm.chooseNewPin(pin) },
            onCancel = {},
        )
        return
    }
    val context = LocalContext.current
    // The lock can be drawn in its own window (a dialog), whose context wraps the activity.
    val activity = remember(context) { context.lockActivity() as? FragmentActivity }
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val shake = remember { Animatable(0f) }
    var pin by remember { mutableStateOf("") }
    var bioMessage by remember { mutableStateOf<String?>(null) }
    var confirmForgot by remember { mutableStateOf(false) }
    val pinDue = vault.biometricEnabled && vault.pinRequired()
    val canBio = vault.biometricEnabled && !pinDue && activity != null && biometricAvailable(context)

    val askBiometric: () -> Unit = {
        val cipher = vault.biometricUnlockCipher()
        if (cipher == null) {
            // Invalidated by a newly added fingerprint (fingerprint unlock is now off), or the PIN is due.
            if (!vault.biometricEnabled) bioMessage = context.getString(R.string.lock_fingerprint_changed)
        } else if (activity != null) {
            runCatching {
                showBiometricPrompt(activity, context.getString(R.string.lock_fingerprint_title), cipher, subtitle = context.getString(R.string.lock_fingerprint_subtitle), onError = { bioMessage = it }) { c ->
                    if (!vm.unlockWithBiometric(c)) bioMessage = context.getString(R.string.lock_fingerprint_failed)
                }
            }
        }
    }
    LaunchedEffect(Unit) {
        if (!vm.biometricShown && canBio) { vm.biometricShown = true; askBiometric() }
    }
    // Back leaves the app rather than revealing anything underneath.
    BackHandler { activity?.moveTaskToBack(true) }

    val wrong: () -> Unit = {
        pin = ""
        scope.shakePin(shake, haptics)
    }
    val minEntry = vm.settings.minEntry
    val savedLength = vm.settings.pinLength

    PinPadScreen(
        title = stringResource(R.string.lock_title),
        message = when {
            vm.lockoutRemaining > 0 -> lockoutText(vm.lockoutRemaining)
            vm.pinError != null -> stringResource(vm.pinError!!)
            bioMessage != null -> bioMessage!!
            vm.settings.legacyPin -> stringResource(R.string.lock_legacy_pin)
            pinDue -> stringResource(R.string.lock_pin_due)
            else -> stringResource(R.string.lock_enter_pin)
        },
        isError = vm.pinError != null || vm.lockoutRemaining > 0 || bioMessage != null,
        pin = pin,
        shake = shake.value,
        checking = vm.checking,
        enabled = vm.lockoutRemaining == 0L && !vm.checking,
        onDigit = { d ->
            if (pin.length < PIN_MAX) {
                bioMessage = null
                vm.clearError()
                pin += d
                if (pin.length == savedLength) vm.submitPin(pin, wrong)
            }
        },
        onBackspace = { pin = pin.dropLast(1) },
        onConfirm = { if (pin.length >= minEntry) vm.submitPin(pin, wrong) },
        showConfirm = pin.length >= minEntry && pin.length != savedLength,
        minDots = maxOf(minEntry, if (vm.settings.legacyPin) savedLength else PIN_MIN),
        bottom = { LockScreenButtons(showFingerprint = canBio, onFingerprint = askBiometric, onForgot = { confirmForgot = true }) },
    )
    if (confirmForgot) {
        ForgotPinDialog(
            body = stringResource(R.string.lock_forgot_body),
            confirm = stringResource(R.string.lock_forgot_confirm),
            onDismiss = { confirmForgot = false },
        ) {
            confirmForgot = false
            onForgot()
        }
    }
}

/** Turning the lock on or off, or changing the PIN, from App settings: a PIN screen on top of the app. */
@Composable
fun PinRequestScreen(request: PinRequest) {
    val vault = App.lock.vault
    val done = { PinRequests.request = null }
    when (request) {
        PinRequest.ENABLE -> PinSetupScreen(
            vault = vault,
            title = stringResource(R.string.lock_choose_title),
            intro = stringResource(R.string.lock_choose_intro, PIN_MIN),
            verifyCurrent = false,
            cancellable = true,
            onDone = { pin -> withContext(Dispatchers.Default) { App.enableLock(pin) }; done() },
            onCancel = done,
        )
        PinRequest.CHANGE -> PinSetupScreen(
            vault = vault,
            title = stringResource(R.string.lock_change_title),
            intro = stringResource(R.string.lock_change_intro, PIN_MIN),
            verifyCurrent = true,
            cancellable = true,
            // Same key, new PIN: fingerprint unlock keeps working.
            onDone = { pin -> withContext(Dispatchers.Default) { check(vault.changePin(pin)) }; done() },
            onCancel = done,
        )
        PinRequest.DISABLE -> PinSetupScreen(
            vault = vault,
            title = stringResource(R.string.lock_disable_title),
            intro = stringResource(R.string.lock_disable_intro),
            verifyCurrent = true,
            verifyOnly = true,
            cancellable = true,
            onDone = { withContext(Dispatchers.Default) { App.disableLock() }; done() },
            onCancel = done,
        )
    }
}

/** The lock and PIN screens are always kept out of screenshots, recordings and casting, like sign-in. */
@Composable
internal fun KeepOutOfScreenshots() = com.vknxworks.seerrctrl.SecureWindow()
