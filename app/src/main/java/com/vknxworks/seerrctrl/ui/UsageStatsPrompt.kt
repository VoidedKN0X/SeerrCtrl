package com.vknxworks.seerrctrl.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.vknxworks.seerrctrl.App

/** What's shared, in the words used by the first-run question and the App settings footer. */
const val USAGE_STATS_DESCRIPTION =
    "Which screens you open, whether you request movies or series, whether requests are approved or declined, " +
        "that you searched, the app version, Android version, phone model and language. Never your server address, " +
        "username, or which titles you request or search for. Sent to VKNXWorks' own server, not to any analytics company."

/**
 * Asked once, after signing in. Nothing is sent until the user says yes; they can change it in App settings.
 * Settings are read straight from the phone's storage, so the question never flashes for people who answered it.
 */
@Composable
fun UsageStatsPrompt() {
    val t = App.telemetry
    if (!t.available || App.settings.usageStatsAsked) return
    AlertDialog(
        onDismissRequest = { },
        icon = { Icon(Icons.Outlined.Insights, null) },
        title = { Text("Help improve SeerrCtrl?") },
        text = {
            Text(
                "Share anonymous usage stats so I can see which features people use and which Android " +
                    "versions to support.\n\n$USAGE_STATS_DESCRIPTION\n\nYou can change this any time in App settings.",
            )
        },
        confirmButton = { TextButton({ t.setOptIn(true) }) { Text("Share stats") } },
        dismissButton = { TextButton({ t.setOptIn(false) }) { Text("No thanks") } },
    )
}
