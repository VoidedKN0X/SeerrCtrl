package com.vknxworks.seerrctrl.data

import android.content.Context
import android.os.SystemClock
import android.util.Base64
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.security.MessageDigest
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** When the app lock asks again after you leave the app. */
enum class LockAgain(val label: String, val millis: Long) {
    IMMEDIATELY("As soon as you leave the app", 0L),
    MINUTE_1("After 1 minute away", 60_000L),
    MINUTES_5("After 5 minutes away", 5 * 60_000L),
    MINUTES_15("After 15 minutes away", 15 * 60_000L),
    HOUR_1("After 1 hour away", 60 * 60_000L),
    RESTART("Only when the app restarts", -1L),
}

/**
 * App lock preferences. The PIN, fingerprint key and the key that encrypts the sign-in live in [LockVault]
 * (the shared Ctrl series lock). Older versions kept a PBKDF2 hash of the PIN here; it's still checked once,
 * on the first unlock after updating, and then replaced by the vault.
 */
class LockSettings(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("app_lock", Context.MODE_PRIVATE)
    val vault = LockVault(appContext, "seerrctrl")

    private var _legacy by mutableStateOf(prefs.contains(KEY_PIN_HASH))
    /** A PIN from before the shared lock, not yet moved to the vault. */
    val legacyPin: Boolean get() = _legacy && !vault.enabled

    /** The app lock is on. */
    val pinSet: Boolean get() = vault.enabled || legacyPin

    val biometricEnabled: Boolean get() = vault.biometricEnabled

    private var _again by mutableStateOf(readLockAgain())
    var lockAgain: LockAgain
        get() = _again
        set(v) { _again = v; prefs.edit().putString(KEY_LOCK_AGAIN, v.name).apply() }

    /** Hide the app in the recent-apps screen and block screenshots/screen recording. */
    private var _hide by mutableStateOf(prefs.getBoolean(KEY_HIDE, true))
    var hideContent: Boolean
        get() = _hide
        set(v) { _hide = v; prefs.edit().putBoolean(KEY_HIDE, v).apply() }

    /** Lets the lock screen check the PIN as soon as enough digits are entered. */
    val pinLength: Int get() = if (vault.enabled) vault.pinLength else prefs.getInt(KEY_PIN_LENGTH, 0)

    /** Shortest PIN the lock screen accepts: older PINs may still have 4 or 5 digits until they're replaced. */
    val minEntry: Int get() = if (legacyPin) LEGACY_MIN else LockVault.PIN_MIN

    fun lockoutRemaining(): Long = if (legacyPin) legacyLockoutRemaining() else vault.lockoutRemaining()

    enum class Unlock { Ok, Wrong, LockedOut, NeedsNewPin }

    /** Checks [pin] (slow on purpose: call off the main thread). A correct older PIN is moved to the vault. */
    fun unlockWithPin(pin: String): Unlock {
        if (!legacyPin) {
            return when (vault.unlockWithPin(pin)) {
                LockVault.PinResult.Ok -> Unlock.Ok
                LockVault.PinResult.Wrong -> Unlock.Wrong
                LockVault.PinResult.LockedOut -> Unlock.LockedOut
            }
        }
        if (legacyLockoutRemaining() > 0) return Unlock.LockedOut
        // Counted before it's checked, so killing the app mid-check doesn't give a free try.
        val fails = prefs.getInt(KEY_FAILED, 0) + 1
        prefs.edit().putInt(KEY_FAILED, fails).commit()
        if (!legacyCheck(pin)) {
            if (fails >= MAX_TRIES) {
                legacyStartLockout((30_000L shl ((fails - MAX_TRIES) / MAX_TRIES).coerceAtMost(5)).coerceAtMost(900_000L))
            }
            return Unlock.Wrong
        }
        prefs.edit().putInt(KEY_FAILED, 0).commit()
        if (pin.length < LockVault.PIN_MIN || isObvious(pin)) return Unlock.NeedsNewPin
        setUp(pin)
        return Unlock.Ok
    }

    /** Turns the lock on with [pin] (or replaces an older PIN). The vault is open afterwards. */
    fun setUp(pin: String) {
        vault.setUp(pin)
        clearLegacy()
    }

    /** Turns the lock off and forgets every key. */
    fun disable() {
        vault.disable()
        clearLegacy()
    }

    private fun clearLegacy() {
        prefs.edit()
            .remove(KEY_PIN_LENGTH).remove(KEY_PIN_HASH).remove(KEY_PIN_SALT)
            .remove(KEY_BIOMETRIC).remove(KEY_FAILED).remove(KEY_LOCKOUT_UNTIL)
            .remove(KEY_LOCKOUT_REMAINING).remove(KEY_LOCKOUT_BASE).remove(KEY_LOCKOUT_BOOT)
            .commit()
        _legacy = false
        // The old fingerprint key can't open the vault; fingerprint unlock is turned on again from Settings.
        runCatching {
            java.security.KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.deleteEntry("seerrctrl_biometric_unlock")
        }
    }

    private fun readLockAgain(): LockAgain {
        prefs.getString(KEY_LOCK_AGAIN, null)?.let { n -> LockAgain.entries.firstOrNull { it.name == n }?.let { return it } }
        if (!prefs.contains(KEY_LOCK_AFTER)) return LockAgain.IMMEDIATELY
        // Older versions: seconds. Never pick a choice that waits longer than before.
        val v = when (prefs.getInt(KEY_LOCK_AFTER, 0)) {
            in Int.MIN_VALUE..59 -> LockAgain.IMMEDIATELY
            in 60..299 -> LockAgain.MINUTE_1
            in 300..899 -> LockAgain.MINUTES_5
            else -> LockAgain.MINUTES_15
        }
        prefs.edit().putString(KEY_LOCK_AGAIN, v.name).remove(KEY_LOCK_AFTER).apply()
        return v
    }

    // ---------------------------------------------------------------- older PIN (migration only)

    private fun legacyCheck(pin: String): Boolean {
        val salt = prefs.getString(KEY_PIN_SALT, null)?.let(::unb64) ?: return false
        val expected = prefs.getString(KEY_PIN_HASH, null)?.let(::unb64) ?: return false
        val spec = PBEKeySpec(pin.toCharArray(), salt, 120_000, 256)
        val actual = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        return MessageDigest.isEqual(expected, actual)
    }

    private fun legacyStartLockout(durationMs: Long) {
        prefs.edit()
            .putLong(KEY_LOCKOUT_REMAINING, durationMs)
            .putLong(KEY_LOCKOUT_BASE, SystemClock.elapsedRealtime())
            .putInt(KEY_LOCKOUT_BOOT, bootCount())
            .remove(KEY_LOCKOUT_UNTIL)
            .commit()
    }

    private fun legacyLockoutRemaining(): Long {
        if (prefs.contains(KEY_LOCKOUT_UNTIL)) {
            val left = (prefs.getLong(KEY_LOCKOUT_UNTIL, 0L) - System.currentTimeMillis()).coerceIn(0L, 900_000L)
            prefs.edit().remove(KEY_LOCKOUT_UNTIL).apply()
            if (left > 0L) legacyStartLockout(left)
        }
        val remaining = prefs.getLong(KEY_LOCKOUT_REMAINING, 0L)
        if (remaining <= 0L) return 0L
        val now = SystemClock.elapsedRealtime()
        val base = prefs.getLong(KEY_LOCKOUT_BASE, now)
        val rebooted = prefs.getInt(KEY_LOCKOUT_BOOT, -1) != bootCount() || now < base
        val left = if (rebooted) remaining else (remaining - (now - base)).coerceAtLeast(0L)
        val edit = prefs.edit()
        if (left <= 0L) edit.remove(KEY_LOCKOUT_REMAINING).remove(KEY_LOCKOUT_BASE).remove(KEY_LOCKOUT_BOOT)
        else edit.putLong(KEY_LOCKOUT_REMAINING, left).putLong(KEY_LOCKOUT_BASE, now).putInt(KEY_LOCKOUT_BOOT, bootCount())
        edit.apply()
        return left
    }

    private fun bootCount(): Int =
        runCatching { android.provider.Settings.Global.getInt(appContext.contentResolver, android.provider.Settings.Global.BOOT_COUNT) }.getOrDefault(-1)

    private fun unb64(s: String): ByteArray = Base64.decode(s, Base64.NO_WRAP)

    companion object {
        const val LEGACY_MIN = 4

        /** One repeated digit, or a straight run up or down (123456, 987654). */
        fun isObvious(pin: String): Boolean =
            pin.toSet().size == 1 || "01234567890123".contains(pin) || "98765432109876".contains(pin)

        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_PIN_SALT = "pin_salt"
        private const val KEY_PIN_LENGTH = "pin_length"
        private const val KEY_BIOMETRIC = "biometric"
        private const val KEY_LOCK_AFTER = "lock_after"
        private const val KEY_LOCK_AGAIN = "lock_again"
        private const val KEY_HIDE = "hide_content"
        private const val KEY_FAILED = "pin_failed"
        private const val KEY_LOCKOUT_UNTIL = "pin_lockout_until"
        private const val KEY_LOCKOUT_REMAINING = "pin_lockout_remaining"
        private const val KEY_LOCKOUT_BASE = "pin_lockout_base_elapsed"
        private const val KEY_LOCKOUT_BOOT = "pin_lockout_boot"
        private const val MAX_TRIES = 5
    }
}
