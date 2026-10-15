package com.vknxworks.seerrctrl.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class ThemeMode { System, Light, Dark }

/** App preferences, backed by SharedPreferences and observable from Compose. */
class Settings(context: Context) {
    private val sp = context.getSharedPreferences("seerr_settings", Context.MODE_PRIVATE)

    private fun bool(key: String, def: Boolean) = mutableStateOf(sp.getBoolean(key, def))
    private fun int(key: String, def: Int) = mutableIntStateOf(sp.getInt(key, def))

    private val _theme = mutableStateOf(ThemeMode.entries[sp.getInt("theme", 0).coerceIn(0, 2)])
    var theme: ThemeMode
        get() = _theme.value
        set(v) { _theme.value = v; sp.edit().putInt("theme", v.ordinal).apply() }

    private val _dynamic = bool("dynamic_color", true)
    var dynamicColor: Boolean
        get() = _dynamic.value
        set(v) { _dynamic.value = v; sp.edit().putBoolean("dynamic_color", v).apply() }

    private val _amoled = bool("amoled", false)
    var pureBlack: Boolean
        get() = _amoled.value
        set(v) { _amoled.value = v; sp.edit().putBoolean("amoled", v).apply() }

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
}
