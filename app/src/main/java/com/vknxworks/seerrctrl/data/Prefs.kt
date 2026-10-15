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

    /** Seerr's session cookie (connect.sid), encrypted at rest. */
    var sessionCookie: String
        get() = decrypt(sp.getString("session", null))
        set(v) = sp.edit().putString("session", encrypt(v)).apply()

    var allowInsecureTls: Boolean
        get() = sp.getBoolean("insecure_tls", false)
        set(v) = sp.edit().putBoolean("insecure_tls", v).apply()

    /** Stable id this install uses towards plex.tv for the Plex sign-in. */
    val plexClientId: String
        get() = sp.getString("plex_client_id", null) ?: java.util.UUID.randomUUID().toString().also {
            sp.edit().putString("plex_client_id", it).apply()
        }

    /** True once a sign-in has worked; cleared on log out. */
    val isConfigured: Boolean get() = baseUrl.isNotBlank() && sessionCookie.isNotBlank()

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
