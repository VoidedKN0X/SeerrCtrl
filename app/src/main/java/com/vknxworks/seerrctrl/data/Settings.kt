package com.vknxworks.seerrctrl.data

import android.content.Context
import androidx.annotation.StringRes
import com.vknxworks.seerrctrl.R
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** The theme choices every Ctrl app offers. Stored by position, so new ones go at the end. */
enum class ThemeMode(@StringRes val label: Int) {
    System(R.string.theme_system), Light(R.string.theme_light), Dark(R.string.theme_dark), Black(R.string.theme_black),
}

/** App preferences, backed by SharedPreferences and observable from Compose. */
class Settings(context: Context) {
    private val sp = context.getSharedPreferences("seerr_settings", Context.MODE_PRIVATE)

    init {
        // Versions before 1.4.10 stored the choice for the old opt-in stats under keys starting with
        // "usage_". Nothing uses them any more, so they're deleted from the phone.
        val old = sp.all.keys.filter { it.startsWith("usage_") }
        if (old.isNotEmpty()) sp.edit().apply { old.forEach { remove(it) } }.apply()
        // Before 1.5.2, pure black was a separate switch: Dark with it on is now the Pure black theme.
        if (sp.contains("amoled")) {
            val edit = sp.edit().remove("amoled")
            if (sp.getBoolean("amoled", false) && sp.getInt("theme", 0) == ThemeMode.Dark.ordinal) edit.putInt("theme", ThemeMode.Black.ordinal)
            edit.apply()
        }
    }

    private fun bool(key: String, def: Boolean) = mutableStateOf(sp.getBoolean(key, def))
    private fun int(key: String, def: Int) = mutableIntStateOf(sp.getInt(key, def))

    private val _theme = mutableStateOf(ThemeMode.entries[sp.getInt("theme", 0).coerceIn(0, ThemeMode.entries.size - 1)])
    var theme: ThemeMode
        get() = _theme.value
        set(v) { _theme.value = v; sp.edit().putInt("theme", v.ordinal).apply() }

    private val _dynamic = bool("dynamic_color", true)
    var dynamicColor: Boolean
        get() = _dynamic.value
        set(v) { _dynamic.value = v; sp.edit().putBoolean("dynamic_color", v).apply() }

    /** True black backgrounds (the "Pure black" theme). */
    val pureBlack: Boolean get() = theme == ThemeMode.Black

    /** Poster size in grids: 0 = small, 1 = medium, 2 = large. */
    private val _poster = int("poster_size", 1)
    var posterSize: Int
        get() = _poster.intValue
        set(v) { _poster.intValue = v; sp.edit().putInt("poster_size", v).apply() }

    /** Hide titles that are already available from discover rows (on top of the server's own setting). */
    private val _hideAvail = bool("hide_available", false)
    var hideAvailable: Boolean
        get() = _hideAvail.value
        set(v) { _hideAvail.value = v; sp.edit().putBoolean("hide_available", v).apply() }

    /** Open trailers and links in an in-app browser tab instead of the default app. */
    private val _tabs = bool("custom_tabs", true)
    var useCustomTabs: Boolean
        get() = _tabs.value
        set(v) { _tabs.value = v; sp.edit().putBoolean("custom_tabs", v).apply() }

    /**
     * Load profile pictures from Gravatar and Plex. Off by default: those sites would see the phone's IP address
     * and whose profiles are being looked at. Pictures stored on the Seerr server always load.
     */
    private val _extAvatars = bool("external_avatars", false)
    var externalAvatars: Boolean
        get() = _extAvatars.value
        set(v) { _extAvatars.value = v; sp.edit().putBoolean("external_avatars", v).apply() }

    // Notifications
    private val _notify = bool("notify_enabled", true)
    var notifyEnabled: Boolean
        get() = _notify.value
        set(v) { _notify.value = v; sp.edit().putBoolean("notify_enabled", v).apply() }

    /** Your own requests: approved, declined, available, failed. */
    private val _nMine = bool("notify_mine", true)
    var notifyMine: Boolean
        get() = _nMine.value
        set(v) { _nMine.value = v; sp.edit().putBoolean("notify_mine", v).apply() }

    /** New requests waiting for approval (managers). */
    private val _nPending = bool("notify_pending", true)
    var notifyPending: Boolean
        get() = _nPending.value
        set(v) { _nPending.value = v; sp.edit().putBoolean("notify_pending", v).apply() }

    /** New issues (managers), and replies/resolution on issues you reported. */
    private val _nIssues = bool("notify_issues", true)
    var notifyIssues: Boolean
        get() = _nIssues.value
        set(v) { _nIssues.value = v; sp.edit().putBoolean("notify_issues", v).apply() }

    /** Minutes between background checks (Android's minimum is 15). */
    private val _nEvery = int("notify_every", 15)
    var notifyEveryMinutes: Int
        get() = _nEvery.intValue
        set(v) { _nEvery.intValue = v; sp.edit().putInt("notify_every", v).apply() }

    /** Instant notifications through ntfy (UnifiedPush): Seerr's webhook wakes the app. Background checks keep running. */
    private val _push = bool("push_enabled", false)
    var pushEnabled: Boolean
        get() = _push.value
        set(v) { _push.value = v; sp.edit().putBoolean("push_enabled", v).apply() }

    /** The push address on your ntfy server that Seerr's webhook posts to; given by ntfy. */
    private val _pushEndpoint = mutableStateOf(sp.getString("push_endpoint", null))
    var pushEndpoint: String?
        get() = _pushEndpoint.value
        set(v) { _pushEndpoint.value = v; sp.edit().apply { if (v != null) putString("push_endpoint", v) else remove("push_endpoint") }.apply() }

    var askedNotificationPermission: Boolean
        get() = sp.getBoolean("asked_notif_perm", false)
        set(v) { sp.edit().putBoolean("asked_notif_perm", v).apply() }
}
