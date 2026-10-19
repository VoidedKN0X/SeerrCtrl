package com.vknxworks.seerrctrl

import android.content.Context
import com.vknxworks.seerrctrl.data.LockSettings
import com.vknxworks.seerrctrl.data.Prefs
import com.vknxworks.seerrctrl.data.SeerrApi
import com.vknxworks.seerrctrl.data.Session
import com.vknxworks.seerrctrl.data.Settings
import com.vknxworks.seerrctrl.data.Telemetry

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
    lateinit var telemetry: Telemetry
        private set

    fun init(ctx: Context) {
        if (::prefs.isInitialized) return
        prefs = Prefs(ctx.applicationContext)
        api = SeerrApi(prefs)
        session = Session(api)
        settings = Settings(ctx.applicationContext)
        lock = LockSettings(ctx.applicationContext)
        telemetry = Telemetry(ctx.applicationContext, settings)
    }
}
