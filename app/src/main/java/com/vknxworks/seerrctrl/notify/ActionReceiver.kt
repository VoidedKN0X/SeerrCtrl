package com.vknxworks.seerrctrl.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.vknxworks.seerrctrl.App
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import com.vknxworks.seerrctrl.R

/** Approve / Decline buttons on "new request" notifications. */
class ActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val requestId = intent.getIntExtra(EXTRA_REQUEST, 0)
        val nid = intent.getIntExtra(EXTRA_NOTIFICATION, 0)
        val title = intent.getStringExtra(EXTRA_TITLE) ?: App.str(R.string.request)
        val approve = intent.action == APPROVE
        if (requestId == 0) return
        App.init(context.applicationContext)
        // Signed out since the notification was posted: never act with whatever account is signed in later.
        // The token is only valid for the sign-in that posted the notification (it's replaced on sign-out).
        if (!App.prefs.isConfigured || !Notifier.checkActionToken(context, intent.getStringExtra(EXTRA_TOKEN))) {
            androidx.core.app.NotificationManagerCompat.from(context).cancel(nid)
            return
        }
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                App.api.post("/request/$requestId/${if (approve) "approve" else "decline"}")
                Notifier.replace(context, nid, Notifier.CH_PENDING, title, if (approve) App.str(R.string.approved) else App.str(R.string.declined))
                Notifier.bump()
            } catch (_: Exception) {
                Notifier.replace(context, nid, Notifier.CH_PENDING, title, App.str(if (approve) R.string.notify_approve_failed else R.string.notify_decline_failed))
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val APPROVE = "com.vknxworks.seerrctrl.APPROVE"
        const val DECLINE = "com.vknxworks.seerrctrl.DECLINE"
        const val EXTRA_REQUEST = "request"
        const val EXTRA_NOTIFICATION = "nid"
        const val EXTRA_TITLE = "title"
        const val EXTRA_TOKEN = "action_token"
    }
}
