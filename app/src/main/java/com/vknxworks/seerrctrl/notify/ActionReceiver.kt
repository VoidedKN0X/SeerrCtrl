package com.vknxworks.seerrctrl.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.vknxworks.seerrctrl.App
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Approve / Decline buttons on "new request" notifications. */
class ActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val requestId = intent.getIntExtra(EXTRA_REQUEST, 0)
        val nid = intent.getIntExtra(EXTRA_NOTIFICATION, 0)
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Request"
        val approve = intent.action == APPROVE
        if (requestId == 0) return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            App.init(context.applicationContext)
            try {
                App.api.post("/request/$requestId/${if (approve) "approve" else "decline"}")
                Notifier.replace(context, nid, Notifier.CH_PENDING, title, if (approve) "Approved" else "Declined")
                Notifier.bump()
            } catch (_: Exception) {
                Notifier.replace(context, nid, Notifier.CH_PENDING, title, "Couldn't ${if (approve) "approve" else "decline"} — open the app")
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
    }
}
