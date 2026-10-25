package com.vknxworks.seerrctrl.ui

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PersistableBundle
import android.provider.Settings
import android.text.format.DateUtils
import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.BatteryAlert
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.vknxworks.seerrctrl.App
import com.vknxworks.seerrctrl.R
import com.vknxworks.seerrctrl.data.Perm
import com.vknxworks.seerrctrl.data.bool
import com.vknxworks.seerrctrl.data.obj
import com.vknxworks.seerrctrl.data.str
import com.vknxworks.seerrctrl.notify.PushEvent
import com.vknxworks.seerrctrl.notify.UnifiedPush
import com.vknxworks.seerrctrl.ui.components.AlertDialog
import com.vknxworks.seerrctrl.ui.components.Button
import com.vknxworks.seerrctrl.ui.components.FilledTonalButton
import com.vknxworks.seerrctrl.ui.lock.expectReturn
import com.vknxworks.seerrctrl.ui.settings.Group
import com.vknxworks.seerrctrl.ui.settings.GroupChoice
import com.vknxworks.seerrctrl.ui.settings.GroupRow
import com.vknxworks.seerrctrl.ui.theme.CtrlColors
import com.vknxworks.seerrctrl.ui.theme.CtrlType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** Push choices: background checks only, or instant with ntfy (the only push app the Ctrl series uses). */
private const val PUSH_OFF = 0
private const val PUSH_NTFY = 1

/** Copies without showing it in clipboard previews: anyone with the push address can wake SeerrCtrl. */
private fun copySensitive(context: Context, text: String) {
    val clip = ClipData.newPlainText(context.getString(R.string.app_name), text).apply {
        description.extras = PersistableBundle().apply { putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true) }
    }
    // Some phones refuse clipboard access now and then; that must not crash the app.
    if (runCatching { context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(clip) }.isFailure) return
    Toast.makeText(context, context.getString(R.string.push_copied), Toast.LENGTH_SHORT).show()
}

/** Turns push on (registering with ntfy) or off (dropping the registration, so the old address can't wake the app). */
fun setPush(context: Context, on: Boolean) {
    val s = App.settings
    if (on) {
        s.pushEnabled = true
        UnifiedPush.register(context, context.getString(R.string.app_name))
    } else {
        UnifiedPush.unregister(context)
        s.pushEnabled = false
        s.pushEndpoint = null
    }
}

/** Posts a test to your own push address, like Seerr's webhook will. Returns whether ntfy's server took it. */
private suspend fun sendTest(endpoint: String): Boolean = withContext(Dispatchers.IO) {
    runCatching {
        val client = OkHttpClient.Builder().callTimeout(15, TimeUnit.SECONDS).build()
        val body = JSONObject().put("app", "SeerrCtrl").put("notification_type", PushEvent.TEST).toString()
        client.newCall(Request.Builder().url(endpoint).post(body.toRequestBody("application/json".toMediaType())).build())
            .execute().use { it.isSuccessful }
    }.getOrDefault(false)
}

/** The push rows in Settings › Notifications: distributor, status, setup and test. [tick] changes when the app comes back. */
@Composable
fun PushRows(tick: Int) {
    val ctx = LocalContext.current
    val nav = LocalNav.current
    val scope = rememberCoroutineScope()
    val s = App.settings
    val ntfyInstalled = remember(tick, s.pushEnabled) { UnifiedPush.isNtfyInstalled(ctx) }
    val ntfyUnrestricted = remember(tick) { UnifiedPush.isNtfyUnrestricted(ctx) }
    val lastError = remember(tick, s.pushEnabled) { UnifiedPush.lastError(ctx) }
    val endpoint = s.pushEndpoint

    GroupChoice(
        stringResource(R.string.push_distributor),
        listOf(PUSH_OFF to stringResource(R.string.push_distributor_off), PUSH_NTFY to stringResource(R.string.push_distributor_ntfy)),
        if (s.pushEnabled) PUSH_NTFY else PUSH_OFF,
        Icons.Outlined.Bolt,
        footer = stringResource(R.string.push_distributor_footer),
    ) { v ->
        when {
            v == PUSH_OFF -> setPush(ctx, false)
            !ntfyInstalled -> Toast.makeText(ctx, ctx.getString(R.string.push_ntfy_missing), Toast.LENGTH_LONG).show()
            else -> setPush(ctx, true)
        }
    }
    GroupRow(
        stringResource(R.string.push_status),
        when {
            !s.pushEnabled && lastError != null -> stringResource(R.string.push_status_failed, lastError)
            !s.pushEnabled -> stringResource(R.string.push_status_off)
            !ntfyInstalled -> stringResource(R.string.push_status_no_ntfy)
            UnifiedPush.unavailable -> stringResource(R.string.push_status_unavailable)
            endpoint == null -> stringResource(R.string.push_status_waiting)
            UnifiedPush.lastPush > 0 -> stringResource(R.string.push_status_on_last, DateUtils.getRelativeTimeSpanString(UnifiedPush.lastPush).toString())
            else -> stringResource(R.string.push_status_on)
        },
        null,
    )
    if (!ntfyInstalled) {
        GroupRow(
            stringResource(R.string.push_ntfy_missing),
            stringResource(R.string.push_ntfy_missing_body),
            Icons.Outlined.ErrorOutline,
            iconTint = MaterialTheme.colorScheme.error,
            trailing = {
                TextButton(onClick = { openUrl(ctx, UnifiedPush.NTFY_DOWNLOAD) }) { Text(stringResource(R.string.push_ntfy_install)) }
            },
        )
    } else if (s.pushEnabled && !ntfyUnrestricted) {
        GroupRow(
            stringResource(R.string.push_ntfy_battery),
            stringResource(R.string.push_ntfy_battery_body),
            Icons.Outlined.BatteryAlert,
            iconTint = MaterialTheme.colorScheme.error,
            showChevron = true,
            onClick = {
                expectReturn()
                runCatching {
                    ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${UnifiedPush.NTFY}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            },
        )
    }
    if (s.pushEnabled && endpoint != null) {
        GroupRow(
            stringResource(R.string.push_setup),
            stringResource(R.string.push_setup_body),
            Icons.Outlined.Dns,
            showChevron = true,
            onClick = { nav.go("pushsetup") },
        )
        GroupRow(stringResource(R.string.push_test), stringResource(R.string.push_test_body), Icons.Outlined.Send, onClick = {
            scope.launch {
                val ok = sendTest(endpoint)
                Toast.makeText(ctx, ctx.getString(if (ok) R.string.push_test_sent else R.string.push_test_failed), Toast.LENGTH_LONG).show()
            }
        })
    }
}

/**
 * What to set in Seerr's webhook agent for instant notifications: the push address and the JSON payload, with
 * copy buttons, and (for users who may change Seerr's settings) a button that sets it all up after confirming.
 */
@Composable
fun PushSetupScreen() {
    val nav = LocalNav.current
    val ctx = LocalContext.current
    val act = rememberActions()
    val endpoint = App.settings.pushEndpoint
    val canConfigure = App.session.can(Perm.MANAGE_SETTINGS)
    var confirm by remember { mutableStateOf<JSONObject?>(null) }
    var checking by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(topBar = { BackBar(stringResource(R.string.push_setup), { nav.back() }) }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {
            if (endpoint == null) {
                Text(
                    stringResource(R.string.push_status_waiting),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(20.dp),
                )
            } else {
            Text(
                stringResource(R.string.push_setup_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )

            if (canConfigure) {
                Group(stringResource(R.string.push_auto), footer = stringResource(R.string.push_auto_footer)) {
                    GroupRow(
                        stringResource(R.string.push_auto_button),
                        stringResource(R.string.push_auto_body),
                        Icons.Outlined.AutoFixHigh,
                        enabled = !act.busy && !checking,
                        showChevron = true,
                        onClick = {
                            // Read the current webhook settings first, so the confirmation can say what gets replaced.
                            checking = true
                            scope.launch {
                                confirm = try { App.api.obj("/settings/notifications/webhook") } catch (_: Exception) { JSONObject() }
                                checking = false
                            }
                        },
                    )
                }
            }

            Group(stringResource(R.string.push_manual), footer = stringResource(R.string.push_manual_footer)) {
                GroupRow(stringResource(R.string.push_step_agent), stringResource(R.string.push_step_agent_body), Icons.Outlined.Dns)
                GroupRow(
                    stringResource(R.string.push_webhook_url), endpoint, Icons.Outlined.Link,
                    onClick = { copySensitive(ctx, endpoint) },
                    trailing = { androidx.compose.material3.Icon(Icons.Outlined.ContentCopy, stringResource(R.string.push_copy), tint = CtrlColors.Muted) },
                )
                GroupRow(stringResource(R.string.push_step_auth), stringResource(R.string.push_step_auth_body), null)
                GroupRow(
                    stringResource(R.string.push_payload), stringResource(R.string.push_payload_body), Icons.Outlined.Code,
                    onClick = { copySensitive(ctx, PushEvent.TEMPLATE) },
                    trailing = { androidx.compose.material3.Icon(Icons.Outlined.ContentCopy, stringResource(R.string.push_copy), tint = CtrlColors.Muted) },
                )
                SelectionContainer {
                    Text(
                        PushEvent.TEMPLATE,
                        style = CtrlType.Data,
                        color = MaterialTheme.colorScheme.onSurface,
                        softWrap = false,
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(16.dp),
                    )
                }
                GroupRow(stringResource(R.string.push_step_types), stringResource(R.string.push_step_types_body), null)
            }
            Spacer(Modifier.height(8.dp))
            }
        }
    }

    val target = endpoint
    if (target != null) confirm?.let { current ->
        val oldUrl = current.obj("options").str("webhookUrl")
        val replaces = current.bool("enabled") && oldUrl != null && oldUrl != target
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text(stringResource(R.string.push_auto_confirm_title)) },
            text = {
                Text(
                    if (replaces) stringResource(R.string.push_auto_confirm_replace, oldUrl ?: "")
                    else stringResource(R.string.push_auto_confirm_body),
                )
            },
            confirmButton = {
                Button(onClick = {
                    confirm = null
                    val old = current.obj("options")
                    val body = JSONObject()
                        .put("enabled", true)
                        .put("embedPoster", false)
                        .put("types", PushEvent.WEBHOOK_TYPES)
                        .put(
                            "options",
                            JSONObject()
                                .put("webhookUrl", target)
                                .put("jsonPayload", PushEvent.TEMPLATE)
                                // An Authorization header already set for this address stays; ntfy's own addresses need none.
                                .put("authHeader", if (oldUrl == target) old.str("authHeader") ?: "" else "")
                                .put("customHeaders", JSONArray())
                                .put("supportVariables", false),
                        )
                    act.run(App.str(R.string.push_auto_done)) { App.api.post("/settings/notifications/webhook", body) }
                }) { Text(stringResource(R.string.push_auto_confirm)) }
            },
            dismissButton = { FilledTonalButton(onClick = { confirm = null }) { Text(stringResource(R.string.lock_cancel)) } },
        )
    }
}
