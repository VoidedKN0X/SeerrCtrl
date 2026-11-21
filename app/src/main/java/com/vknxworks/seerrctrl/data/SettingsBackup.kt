package com.vknxworks.seerrctrl.data

import org.json.JSONObject

/**
 * SeerrCtrl's own settings as a file, to carry them to another phone or keep them through a reinstall.
 *
 * What's in it: poster size, the browsing switches, which notifications are wanted and how often the
 * background check runs, and the two "hide" switches. What is never in it: the server address, the
 * sign-in, the app PIN or anything else of the app lock, a trusted certificate, and the ntfy push address.
 * Restoring can't make the app less protected or more talkative: it never touches the app lock, never
 * turns notifications or push on or off (that changes how the sign-in is encrypted), never turns a "hide"
 * switch off and never turns on profile pictures from other sites.
 */
class SettingsBackup(
    val createdAt: String,
    val posterSize: Int?,
    val hideAvailable: Boolean?,
    val useCustomTabs: Boolean?,
    val notifyMine: Boolean?,
    val notifyPending: Boolean?,
    val notifyIssues: Boolean?,
    val notifyEveryMinutes: Int?,
    val hideScreenshots: Boolean?,
    val hideRecents: Boolean?,
)

/** The file isn't a SeerrCtrl settings file, or ([newer]) it was made by a newer version. */
class BackupFormatException(val newer: Boolean) : Exception()

object SettingsBackups {
    const val APP = "SeerrCtrl"
    const val FORMAT = 1
    const val MAX_BYTES = 64 * 1024
    val INTERVALS = listOf(15, 30, 60)

    fun export(s: Settings, lock: LockSettings, appVersion: String, createdAt: String): String = JSONObject().apply {
        put("app", APP)
        put("format", FORMAT)
        put("appVersion", appVersion)
        put("createdAt", createdAt)
        put("posterSize", s.posterSize)
        put("hideAvailable", s.hideAvailable)
        put("useCustomTabs", s.useCustomTabs)
        put("notifyMine", s.notifyMine)
        put("notifyPending", s.notifyPending)
        put("notifyIssues", s.notifyIssues)
        put("notifyEveryMinutes", s.notifyEveryMinutes)
        put("hideScreenshots", lock.hideScreenshots)
        put("hideRecents", lock.hideRecents)
    }.toString(2)

    fun parse(text: String): SettingsBackup {
        val j = try { JSONObject(text) } catch (_: Exception) { throw BackupFormatException(newer = false) }
        val format = j.int("format")
        if (j.str("app") != APP || format < 1) throw BackupFormatException(newer = false)
        if (format > FORMAT) throw BackupFormatException(newer = true)
        fun flag(key: String): Boolean? = if (j.has(key) && !j.isNull(key)) j.opt(key) as? Boolean else null
        return SettingsBackup(
            createdAt = j.str("createdAt").orEmpty(),
            posterSize = j.intOrNull("posterSize")?.takeIf { it in 0..2 },
            hideAvailable = flag("hideAvailable"),
            useCustomTabs = flag("useCustomTabs"),
            notifyMine = flag("notifyMine"),
            notifyPending = flag("notifyPending"),
            notifyIssues = flag("notifyIssues"),
            notifyEveryMinutes = j.intOrNull("notifyEveryMinutes")?.takeIf { it in INTERVALS },
            hideScreenshots = flag("hideScreenshots"),
            hideRecents = flag("hideRecents"),
        )
    }

    /** Applies [b] with the limits described on [SettingsBackup]. */
    fun restore(b: SettingsBackup, s: Settings, lock: LockSettings) {
        b.posterSize?.let { s.posterSize = it }
        b.hideAvailable?.let { s.hideAvailable = it }
        b.useCustomTabs?.let { s.useCustomTabs = it }
        b.notifyMine?.let { s.notifyMine = it }
        b.notifyPending?.let { s.notifyPending = it }
        b.notifyIssues?.let { s.notifyIssues = it }
        b.notifyEveryMinutes?.let { s.notifyEveryMinutes = it }
        if (b.hideScreenshots == true) lock.hideScreenshots = true
        if (b.hideRecents == true) lock.hideRecents = true
    }
}
