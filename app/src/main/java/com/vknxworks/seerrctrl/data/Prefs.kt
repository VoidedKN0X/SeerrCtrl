package com.vknxworks.seerrctrl.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Saved server address and session. The session cookie is encrypted with an Android Keystore key. */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("seerrctrl", Context.MODE_PRIVATE)

    var baseUrl: String
        get() = sp.getString("base_url", "") ?: ""
        set(v) = sp.edit().putString("base_url", v).apply()

    /** The app lock's vault, and whether the session cookie should be sealed with it (set by [com.vknxworks.seerrctrl.App]). */
    var vault: LockVault? = null
    var sealWanted: () -> Boolean = { false }

    /**
     * Seerr's session cookie (connect.sid), encrypted at rest with the Keystore. With the app lock on (and
     * background notifications off) it's also encrypted with the lock's key, so it reads as "" while locked.
     */
    var sessionCookie: String
        get() {
            val plain = decrypt(sp.getString("session", null))
            return if (LockVault.isSealed(plain)) vault?.open(plain) ?: "" else plain
        }
        set(v) {
            if (v.isEmpty()) { sp.edit().remove("session").apply(); return }
            // Locked and it must be sealed: keep what's saved rather than store it unprotected.
            val inner = if (sealWanted()) vault?.seal(v) ?: return else v
            sp.edit().putString("session", encrypt(inner)).apply()
        }

    /** The saved cookie is sealed by the app lock (unreadable until the app is unlocked). */
    val cookieSealed: Boolean get() = LockVault.isSealed(decrypt(sp.getString("session", null)))

    /** Seals or unseals the saved cookie to match [sealWanted]. Needs the app unlocked if it's sealed now. */
    fun resealCookie() {
        val c = sessionCookie
        if (c.isNotEmpty()) sessionCookie = c
    }

    /** "Self-signed certificate" option. Now only means "a certificate is (or should be) pinned". */
    var allowInsecureTls: Boolean
        get() = sp.getBoolean("insecure_tls", false)
        set(v) = sp.edit().putBoolean("insecure_tls", v).apply()

    /** SHA-256 (hex) of the self-signed server certificate the user confirmed; "" when none is pinned. */
    var pinnedCert: String
        get() = sp.getString("pinned_cert", "") ?: ""
        set(v) = sp.edit().putString("pinned_cert", v).apply()

    /** The user confirmed that their http:// server address is unencrypted. */
    var httpAcknowledged: Boolean
        get() = sp.getBoolean("http_ack", false)
        set(v) = sp.edit().putBoolean("http_ack", v).apply()

    /** Stable id this install uses towards plex.tv for the Plex sign-in. */
    val plexClientId: String
        get() = sp.getString("plex_client_id", null) ?: java.util.UUID.randomUUID().toString().also {
            sp.edit().putString("plex_client_id", it).apply()
        }

    /** True once a sign-in has worked; cleared on log out. */
    val isConfigured: Boolean get() = baseUrl.isNotBlank() && (cookieSealed || sessionCookie.isNotBlank())

    fun clearSession() = sp.edit().remove("session").apply()
    fun clear() = sp.edit().clear().apply()

    // --- Keystore AES-GCM ---
    private val alias = "seerrctrl_key"

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(alias, null) as? SecretKey)?.let { return it }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(
            KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
        )
        return gen.generateKey()
    }

    private fun encrypt(plain: String): String? {
        if (plain.isEmpty()) return null
        return try {
            val c = Cipher.getInstance("AES/GCM/NoPadding")
            c.init(Cipher.ENCRYPT_MODE, key())
            val out = c.iv + c.doFinal(plain.toByteArray())
            Base64.encodeToString(out, Base64.NO_WRAP)
        } catch (_: Exception) { null }
    }

    private fun decrypt(stored: String?): String {
        if (stored.isNullOrEmpty()) return ""
        return try {
            val raw = Base64.decode(stored, Base64.NO_WRAP)
            val c = Cipher.getInstance("AES/GCM/NoPadding")
            c.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, raw, 0, 12))
            String(c.doFinal(raw, 12, raw.size - 12))
        } catch (_: Exception) { "" }
    }
}
