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
import com.vknxworks.seerrctrl.data.LockVault
import com.vknxworks.seerrctrl.ui.pressScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

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
    onError: (String?) -> Unit = {},
    onSuccess: (Cipher) -> Unit,
) {
    val prompt = BiometricPrompt(
        activity,
        ContextCompat.getMainExecutor(activity),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                val c = result.cryptoObject?.cipher
                if (c != null) onSuccess(c) else onError("Fingerprint didn't unlock the key")
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
            .setNegativeButtonText("Use PIN")
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
    var pinError by mutableStateOf<String?>(null)
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

    fun onBackground() {
        val trip = LockTrips.expectReturnAt
        LockTrips.expectReturnAt = 0L
        // Only right after the app opened something itself; leaving later still counts.
        if (trip > 0 && SystemClock.elapsedRealtime() - trip < 5_000) { backgroundedAt = 0L; return }
        backgroundedAt = SystemClock.elapsedRealtime()
        // "As soon as you leave" locks before the app is hidden, so the key is gone while it's in the background.
        if (settings.pinSet && settings.lockAgain == LockAgain.IMMEDIATELY) lockNow()
    }

    fun onForeground() {
        val since = backgroundedAt
        backgroundedAt = 0L
        LockTrips.expectReturnAt = 0L
        val after = settings.lockAgain.millis
        if (!settings.pinSet || locked || since == 0L || after < 0) return
        if (SystemClock.elapsedRealtime() - since >= after) lockNow()
    }

    fun lockNow() {
        if (settings.pinSet && !locked) {
            locked = true
            biometricShown = false
            pinError = null
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
                    pinError = "Wrong PIN"
                    onWrong()
                }
            }
        }
    }

    /** Saves the new PIN chosen after an older short PIN. */
    fun chooseNewPin(pin: String) {
        viewModelScope.launch {
            checking = true
            withContext(Dispatchers.Default) { App.enableLock(pin) }
            checking = false
            mustChoosePin = false
            unlocked()
        }
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

@Composable
fun LockScreen(vm: AppLockViewModel, onForgot: () -> Unit) {
    if (vm.mustChoosePin) { NewPinScreen(vm); return }
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val cs = MaterialTheme.colorScheme
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val shake = remember { Animatable(0f) }
    var pin by remember { mutableStateOf("") }
    var bioMessage by remember { mutableStateOf<String?>(null) }
    var confirmForgot by remember { mutableStateOf(false) }
    val vault = vm.settings.vault
    val pinDue = vault.biometricEnabled && vault.pinRequired()
    val canBio = vault.biometricEnabled && !pinDue && activity != null && biometricAvailable(context)

    val askBiometric: () -> Unit = {
        val cipher = vault.biometricUnlockCipher()
        if (cipher == null) {
            // Invalidated by a newly added fingerprint (fingerprint unlock is now off), or the PIN is due.
            if (!vault.biometricEnabled) bioMessage = "A fingerprint was added or removed. Use your PIN, then turn fingerprint unlock on again in settings."
        } else if (activity != null) {
            runCatching {
                showBiometricPrompt(activity, "Unlock SeerrCtrl", cipher, onError = { bioMessage = it }) { c ->
                    if (!vm.unlockWithBiometric(c)) bioMessage = "Couldn't unlock with your fingerprint. Use your PIN."
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
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        scope.launch {
            for (x in listOf(18f, -14f, 10f, -6f, 0f)) shake.animateTo(x, androidx.compose.animation.core.tween(45))
        }
    }

    fun press(d: Char) {
        if (vm.lockoutRemaining > 0 || vm.checking || pin.length >= PIN_MAX) return
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        bioMessage = null
        pin += d
        if (pin.length == vm.settings.pinLength) { val p = pin; pin = ""; vm.submitPin(p, wrong) }
    }
    val minEntry = vm.settings.minEntry

    Surface(Modifier.fillMaxSize(), color = cs.background) {
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                Modifier.size(72.dp).clip(CircleShape).background(cs.primaryContainer),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Outlined.Lock, null, tint = cs.onPrimaryContainer, modifier = Modifier.size(34.dp)) }
            Spacer(Modifier.height(18.dp))
            Text("SeerrCtrl is locked", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            val err = vm.pinError != null || vm.lockoutRemaining > 0 || bioMessage != null
            Text(
                when {
                    vm.lockoutRemaining > 0 -> "Too many tries. Try again in ${(vm.lockoutRemaining + 999) / 1000} s"
                    vm.checking -> "Checking…"
                    vm.pinError != null -> vm.pinError!!
                    bioMessage != null -> bioMessage!!
                    pinDue -> "Enter your PIN. It's needed after a restart and every 72 hours."
                    else -> "Enter your PIN"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (err) cs.error else cs.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            Box(Modifier.offset { IntOffset(shake.value.roundToInt(), 0) }) { PinDots(pin.length) }
            Spacer(Modifier.height(32.dp))
            PinPad(
                onDigit = ::press,
                onBackspace = { if (pin.isNotEmpty()) pin = pin.dropLast(1) },
                onConfirm = { if (pin.length >= minEntry) { val p = pin; pin = ""; vm.submitPin(p, wrong) } },
                confirmEnabled = pin.length >= minEntry && !vm.checking && vm.lockoutRemaining == 0L,
            )
            Spacer(Modifier.height(20.dp))
            if (canBio) {
                FilledTonalButton(onClick = askBiometric) {
                    Icon(Icons.Outlined.Fingerprint, null)
                    Spacer(Modifier.size(8.dp))
                    Text("Use fingerprint")
                }
            }
            TextButton(onClick = { confirmForgot = true }) { Text("Forgot PIN? Log out") }
        }
    }
    if (confirmForgot) {
        AlertDialog(
            onDismissRequest = { confirmForgot = false },
            title = { Text("Forgot your PIN?") },
            text = {
                Text(
                    "The PIN can't be recovered, because it encrypts your sign-in. SeerrCtrl will log out, delete what it " +
                        "keeps on this phone and turn the app lock off. Your Seerr server and requests aren't touched."
                )
            },
            confirmButton = { TextButton({ confirmForgot = false; onForgot() }) { Text("Log out", color = cs.error) } },
            dismissButton = { TextButton({ confirmForgot = false }) { Text("Cancel") } },
        )
    }
}

/** After an older short (or very obvious) PIN: choose a new one of at least [PIN_MIN] digits, twice. */
@Composable
private fun NewPinScreen(vm: AppLockViewModel) {
    val activity = LocalContext.current as? FragmentActivity
    val cs = MaterialTheme.colorScheme
    val haptics = LocalHapticFeedback.current
    var pin by remember { mutableStateOf("") }
    var first by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    BackHandler { activity?.moveTaskToBack(true) }

    fun next() {
        if (vm.checking || pin.length < PIN_MIN) return
        val f = first
        when {
            f == null && LockSettings.isObvious(pin) -> { message = "Too easy to guess. Try a less obvious PIN."; pin = "" }
            f == null -> { first = pin; pin = ""; message = null }
            pin != f -> { first = null; pin = ""; message = "The PINs didn't match. Start again." }
            else -> vm.chooseNewPin(pin)
        }
        if (message != null) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    Surface(Modifier.fillMaxSize(), color = cs.background) {
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                Modifier.size(72.dp).clip(CircleShape).background(cs.primaryContainer),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Outlined.Lock, null, tint = cs.onPrimaryContainer, modifier = Modifier.size(34.dp)) }
            Spacer(Modifier.height(18.dp))
            Text("Choose a new PIN", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                message ?: if (first == null) "The app lock now needs at least $PIN_MIN digits, and encrypts your sign-in with the PIN."
                else "Enter the same PIN again",
                style = MaterialTheme.typography.bodyMedium,
                color = if (message != null) cs.error else cs.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            PinDots(pin.length)
            Spacer(Modifier.height(32.dp))
            PinPad(
                onDigit = { d ->
                    if (pin.length < PIN_MAX) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        message = null
                        pin += d
                    }
                },
                onBackspace = { if (pin.isNotEmpty()) pin = pin.dropLast(1) },
                onConfirm = ::next,
                confirmEnabled = pin.length >= PIN_MIN && !vm.checking,
            )
        }
    }
}

@Composable
fun PinDots(count: Int) {
    val cs = MaterialTheme.colorScheme
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        repeat(maxOf(count, PIN_MIN)) { i ->
            Box(
                Modifier.size(14.dp).clip(CircleShape)
                    .background(if (i < count) cs.primary else cs.surfaceContainerHighest)
            )
        }
    }
}

@Composable
fun PinPad(onDigit: (Char) -> Unit, onBackspace: () -> Unit, onConfirm: () -> Unit, confirmEnabled: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        listOf("123", "456", "789").forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) { row.forEach { d -> DigitKey(d.toString()) { onDigit(d) } } }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(22.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBackspace, modifier = Modifier.size(72.dp)) {
                Icon(Icons.AutoMirrored.Outlined.Backspace, "Delete")
            }
            DigitKey("0") { onDigit('0') }
            TextButton(onClick = onConfirm, enabled = confirmEnabled, modifier = Modifier.size(72.dp)) { Text("OK") }
        }
    }
}

@Composable
private fun DigitKey(label: String, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    FilledIconButton(
        onClick = onClick,
        interactionSource = source,
        colors = androidx.compose.material3.IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        modifier = Modifier.size(72.dp).pressScale(source, 0.9f),
    ) { Text(label, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Medium) }
}
