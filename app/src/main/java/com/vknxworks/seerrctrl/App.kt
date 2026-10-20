package com.vknxworks.seerrctrl

import android.content.Context
import com.vknxworks.seerrctrl.data.LockSettings
import com.vknxworks.seerrctrl.data.Prefs
import com.vknxworks.seerrctrl.data.SeerrApi
import com.vknxworks.seerrctrl.data.Session
import com.vknxworks.seerrctrl.data.Settings

/** Tiny service locator. */
object App {
    lateinit var prefs: Prefs
        private set
    lateinit var api: SeerrApi
        private set
    lateinit var session: Session
        private set
    lateinit var settings: Settings
        private set
    lateinit var lock: LockSettings
        private set

    fun init(ctx: Context) {
        if (::prefs.isInitialized) return
        prefs = Prefs(ctx.applicationContext)
        settings = Settings(ctx.applicationContext)
        lock = LockSettings(ctx.applicationContext)
        // Background checks need the session while the app is locked, so it's only sealed with notifications off.
        prefs.vault = lock.vault
        prefs.sealWanted = { lock.vault.enabled && !settings.notifyEnabled }
        api = SeerrApi(prefs)
        session = Session(api)
    }

    /** The app locked: wipe the lock's key from memory, and the session cookie if it's sealed. */
    fun onLocked() {
        lock.vault.close()
        api.lockDown()
    }

    /** The app unlocked (vault open): read the session again. */
    fun onUnlocked() {
        val sealed = prefs.cookieSealed
        prefs.resealCookie()
        if (sealed) api.reload()
    }

    /** Turns the app lock on with [pin] (or replaces an older PIN) and seals the sign-in. */
    fun enableLock(pin: String) {
        lock.setUp(pin)
        prefs.resealCookie()
    }

    /** Turns the app lock off (after the PIN was confirmed): the sign-in goes back to Keystore-only encryption. */
    fun disableLock() {
        val cookie = prefs.sessionCookie
        lock.disable()
        if (cookie.isNotEmpty()) prefs.sessionCookie = cookie
    }
}
