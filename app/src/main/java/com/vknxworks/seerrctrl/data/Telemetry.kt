package com.vknxworks.seerrctrl.data

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import com.vknxworks.seerrctrl.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Opt-in, anonymous usage stats sent to the project's own self-hosted Aptabase server.
 *
 * Nothing is sent unless the user turned "Share anonymous usage stats" on, and builds without an
 * Aptabase key (anyone building from source) can't send anything at all.
 *
 * What an event contains: its name, a few fixed-value properties chosen in code (a screen's name,
 * "movie" or "tv", "approved" or "declined"), the app version, the Android version, the phone model, the
 * system language and a random session ID that changes every launch or after an hour of inactivity.
 * Never sent: server addresses, usernames, titles, TMDB or request IDs, search text, or any persistent
 * device or install ID. (Like any web request, the stats server sees the phone's IP address; Aptabase uses
 * it for a country lookup and doesn't store it.)
 */
class Telemetry(private val context: Context, private val settings: Settings) {
    /** Whether this build can send stats at all (it has a key and an HTTPS server address). */
    val available: Boolean = BuildConfig.APTABASE_KEY.startsWith("A-SH-") && BuildConfig.APTABASE_HOST.startsWith("https://")

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // A separate client from the Seerr one: no session cookie, no pinned certificates, no redirects.
    private val http: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .followRedirects(false)
            .followSslRedirects(false)
            .build()
    }

    private val systemProps: JSONObject by lazy {
        JSONObject()
            .put("isDebug", BuildConfig.DEBUG)
            .put("osName", "Android")
            .put("osVersion", Build.VERSION.RELEASE ?: "")
            .put("locale", Locale.getDefault().language)
            .put("appVersion", BuildConfig.VERSION_NAME)
            .put("appBuildNumber", BuildConfig.VERSION_CODE.toString())
            .put("sdkVersion", "seerrctrl@${BuildConfig.VERSION_NAME}")
            .put("deviceModel", Build.MODEL ?: "")
    }

    private val queue = Channel<JSONObject>(capacity = 64, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    @Volatile private var sessionId = newSessionId()
    @Volatile private var lastEventAt = 0L

    init {
        if (available) {
            // One event at a time, in order. Failures are dropped: stats are never stored or retried.
            scope.launch { for (event in queue) send(event) }
        }
    }

    private val enabled: Boolean
        get() = available && settings.usageStats

    /** Called when the app's screen is opened (not for background checks or notification buttons). */
    fun appOpened() = track("app_opened", mapOf("form_factor" to formFactor(), "theme" to settings.theme.name.lowercase()))

    /**
     * Saves the user's answer (Settings switch or the first-run question). Switching on starts a fresh
     * session, so stats from before and after aren't linked.
     */
    fun setOptIn(on: Boolean) {
        val was = settings.usageStats
        settings.usageStatsAsked = true
        settings.usageStats = on
        if (on && !was) {
            resetSession()
            appOpened()
        }
    }

    /** Records an event if the user opted in. [props] values must be fixed strings or numbers from code. */
    fun track(name: String, props: Map<String, Any> = emptyMap()) {
        if (!enabled) return
        val now = System.currentTimeMillis()
        if (now - lastEventAt > SESSION_TIMEOUT_MS) sessionId = newSessionId()
        lastEventAt = now
        val event = JSONObject()
            .put("timestamp", timestamp(now))
            .put("sessionId", sessionId)
            .put("eventName", name)
            .put("systemProps", systemProps)
            .put("props", JSONObject(props))
        queue.trySend(event)
    }

    private fun resetSession() {
        sessionId = newSessionId()
        lastEventAt = 0L
    }

    private fun send(event: JSONObject) {
        // The user may have opted out while this was queued.
        if (!enabled) return
        val request = Request.Builder()
            .url("${BuildConfig.APTABASE_HOST.trimEnd('/')}/api/v0/event")
            .header("App-Key", BuildConfig.APTABASE_KEY)
            .header("User-Agent", "SeerrCtrl/${BuildConfig.VERSION_NAME} (Android)")
            .post(event.toString().toRequestBody(JSON))
            .build()
        runCatching { http.newCall(request).execute().close() }
    }

    /** "phone" or "tablet", from the screen's smallest width. */
    private fun formFactor(): String {
        val sw = context.resources.configuration.smallestScreenWidthDp
        return if (sw != Configuration.SMALLEST_SCREEN_WIDTH_DP_UNDEFINED && sw >= 600) "tablet" else "phone"
    }

    private fun newSessionId(): String = UUID.randomUUID().toString()

    private fun timestamp(ms: Long): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date(ms))

    companion object {
        private val JSON = "application/json".toMediaType()
        private val SESSION_TIMEOUT_MS = TimeUnit.HOURS.toMillis(1)

        /**
         * "user/{id}/settings" -> "user_settings". Only the route pattern's fixed parts: arguments (IDs,
         * browse keys, admin sections) are dropped.
         */
        fun screenName(route: String): String =
            route.substringBefore('?').split('/').filter { it.isNotEmpty() && !it.startsWith("{") }.joinToString("_")
                .let { if (route.startsWith("admin/{")) "admin_section" else it }
    }
}
