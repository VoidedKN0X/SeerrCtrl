package com.vknxworks.seerrctrl.notify

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import com.vknxworks.seerrctrl.App
import com.vknxworks.seerrctrl.R
import org.json.JSONObject

/**
 * Receives what ntfy hands over: the push address, registration errors and pushes from Seerr's webhook.
 *
 * A push is only a hint: SeerrCtrl reads which kind of event it was and the request or issue number, then asks
 * your Seerr server itself for the details, exactly like the background check, and shows the same notifications.
 * Nothing else in the message (titles, names, comments) is trusted or shown, so a forged push can at most make
 * the app check your server early.
 *
 * Exported because ntfy is another app, so every broadcast must carry the secret token of SeerrCtrl's
 * registration or it's ignored: the token is what keeps other apps out. From Android 14 a broadcast from a
 * sender that identifies itself as an app other than ntfy is dropped too.
 */
class PushReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Exported: a broadcast from another app with broken extras (or a failure scheduling the check) must never
        // crash SeerrCtrl. The periodic check catches up on anything missed.
        try { handle(context, intent) } catch (_: Exception) {}
    }

    private fun handle(context: Context, intent: Intent) {
        val token = intent.getStringExtra(UnifiedPush.EXTRA_TOKEN) ?: return
        val ours = UnifiedPush.token(context) ?: return
        if (!java.security.MessageDigest.isEqual(token.toByteArray(), ours.toByteArray())) return
        if (Build.VERSION.SDK_INT >= 34) {
            val sender = sentFromPackage
            if (sender != null && sender != UnifiedPush.NTFY) return
        }
        val app = context.applicationContext
        App.init(app)
        when (intent.action) {
            UnifiedPush.ACTION_NEW_ENDPOINT -> {
                val endpoint = intent.getStringExtra(UnifiedPush.EXTRA_ENDPOINT) ?: return
                UnifiedPush.ack(app, token, intent.getStringExtra(UnifiedPush.EXTRA_ID))
                UnifiedPush.onEndpoint(app)
                App.settings.pushEndpoint = endpoint
            }
            UnifiedPush.ACTION_REGISTRATION_FAILED -> {
                UnifiedPush.onLost(app, intent.getStringExtra(UnifiedPush.EXTRA_REASON) ?: "UNKNOWN")
                App.settings.pushEnabled = false
                App.settings.pushEndpoint = null
            }
            UnifiedPush.ACTION_UNREGISTERED -> {
                UnifiedPush.onLost(app, null)
                App.settings.pushEnabled = false
                App.settings.pushEndpoint = null
            }
            UnifiedPush.ACTION_TEMP_UNAVAILABLE -> UnifiedPush.onTempUnavailable()
            UnifiedPush.ACTION_MESSAGE -> {
                UnifiedPush.onMessage(app)
                UnifiedPush.ack(app, token, intent.getStringExtra(UnifiedPush.EXTRA_ID))
                if (!App.prefs.isConfigured || !App.settings.notifyEnabled || !App.settings.pushEnabled) return
                val event = PushEvent.parse(
                    intent.getByteArrayExtra(UnifiedPush.EXTRA_BYTES_MESSAGE)?.toString(Charsets.UTF_8)
                        ?: intent.getStringExtra(UnifiedPush.EXTRA_MESSAGE),
                )
                if (event.type == PushEvent.TEST) {
                    Notifier.channels(app)
                    Notifier.post(app, PushEvent.TEST_NOTIFICATION_ID, Notifier.CH_MINE, App.str(R.string.push_test_title), App.str(R.string.push_test_text), "requests")
                } else {
                    Notifier.checkNow(app, event)
                }
            }
        }
    }
}

/**
 * What a push from Seerr's webhook says, as set up by SeerrCtrl's payload template ([PushEvent.TEMPLATE]):
 * the kind of event and the request or issue it's about. Anything that doesn't parse is treated as
 * "something changed" and the whole check runs.
 */
data class PushEvent(val type: String, val requestId: Int, val issueId: Int) {
    val aboutRequest get() = type in REQUEST_TYPES
    val aboutIssue get() = type in ISSUE_TYPES

    companion object {
        const val TEST = "TEST_NOTIFICATION"
        const val TEST_NOTIFICATION_ID = 9_999
        val REQUEST_TYPES = setOf("MEDIA_PENDING", "MEDIA_APPROVED", "MEDIA_AUTO_APPROVED", "MEDIA_AVAILABLE", "MEDIA_FAILED", "MEDIA_DECLINED", "MEDIA_AUTO_REQUESTED")
        val ISSUE_TYPES = setOf("ISSUE_CREATED", "ISSUE_COMMENT", "ISSUE_RESOLVED", "ISSUE_REOPENED")

        /** Seerr's notification types SeerrCtrl asks the webhook to send (bit flags, as in Seerr's settings). */
        const val WEBHOOK_TYPES = 2L or 4L or 8L or 16L or 64L or 128L or 256L or 512L or 1024L or 2048L or 4096L

        /**
         * The JSON payload for Seerr's webhook. Only the kind of event and numbers: no titles, names or comments
         * pass through your ntfy server. SeerrCtrl looks those up on your Seerr server itself.
         */
        val TEMPLATE = """
            {
              "app": "SeerrCtrl",
              "notification_type": "{{notification_type}}",
              "media_type": "{{media_type}}",
              "media_tmdbid": "{{media_tmdbid}}",
              "request_id": "{{request_id}}",
              "issue_id": "{{issue_id}}"
            }
        """.trimIndent()

        fun parse(text: String?): PushEvent {
            val o = runCatching { JSONObject(text.orEmpty()) }.getOrNull() ?: return PushEvent("", 0, 0)
            fun num(k: String) = o.optString(k).trim().toIntOrNull()?.takeIf { it > 0 } ?: 0
            return PushEvent(o.optString("notification_type").trim().uppercase(), num("request_id"), num("issue_id"))
        }
    }
}

/**
 * ntfy binds to this for a few seconds before it delivers a push. That lifts SeerrCtrl to foreground importance,
 * so the check can start straight away with network access even while the phone is in deep sleep.
 */
class RaiseToForegroundService : Service() {
    private val binder = Binder()
    override fun onBind(intent: Intent?): IBinder = binder
}
