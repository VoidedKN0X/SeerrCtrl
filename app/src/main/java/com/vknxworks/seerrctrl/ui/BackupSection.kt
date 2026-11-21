package com.vknxworks.seerrctrl.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.padding
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
import com.vknxworks.seerrctrl.BuildConfig
import com.vknxworks.seerrctrl.R
import com.vknxworks.seerrctrl.data.BackupFormatException
import com.vknxworks.seerrctrl.data.SettingsBackup
import com.vknxworks.seerrctrl.data.SettingsBackups
import com.vknxworks.seerrctrl.notify.Notifier
import com.vknxworks.seerrctrl.ui.components.AlertDialog
import com.vknxworks.seerrctrl.ui.lock.expectReturn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.time.LocalDate
import java.time.OffsetDateTime

/**
 * Back up and restore SeerrCtrl's own settings: a small JSON file in a place the user picks. No server,
 * sign-in, PIN or certificate is in it, and restoring shows what will be replaced and asks first.
 */
@Composable
fun BackupSection() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val snack = LocalSnackbar.current
    var pending by remember { mutableStateOf<SettingsBackup?>(null) }
    fun say(id: Int) { scope.launch { snack.showSnackbar(ctx.getString(id)) } }

    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val ok = runCatching {
                val text = SettingsBackups.export(App.settings, App.lock, BuildConfig.VERSION_NAME, OffsetDateTime.now().toString())
                withContext(Dispatchers.IO) {
                    ctx.contentResolver.openOutputStream(uri, "wt")?.use { it.write(text.toByteArray(Charsets.UTF_8)) } ?: throw IOException()
                }
            }.isSuccess
            say(if (ok) R.string.backup_saved else R.string.backup_save_failed)
        }
    }

    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val result = runCatching {
                val text = withContext(Dispatchers.IO) {
                    ctx.contentResolver.openInputStream(uri)?.use { input ->
                        // A settings file is well under a kilobyte; anything far bigger isn't one.
                        val out = ByteArrayOutputStream()
                        val buf = ByteArray(8192)
                        while (true) {
                            val n = input.read(buf)
                            if (n < 0) break
                            out.write(buf, 0, n)
                            if (out.size() > SettingsBackups.MAX_BYTES) throw BackupFormatException(newer = false)
                        }
                        out.toString(Charsets.UTF_8.name())
                    } ?: throw IOException()
                }
                SettingsBackups.parse(text)
            }
            result.onSuccess { pending = it }.onFailure { e ->
                say(if (e is BackupFormatException && e.newer) R.string.backup_too_new else R.string.backup_not_valid)
            }
        }
    }

    SettingsSection(stringResource(R.string.backup_title)) {
        NavItem(Icons.Outlined.FileDownload, stringResource(R.string.backup_save), stringResource(R.string.backup_save_sub)) {
            expectReturn(); save.launch("seerrctrl-settings-${LocalDate.now()}.json")
        }
        NavItem(Icons.Outlined.FileUpload, stringResource(R.string.backup_restore), stringResource(R.string.backup_restore_sub)) {
            expectReturn(); open.launch(arrayOf("application/json", "text/*", "application/octet-stream"))
        }
    }
    Text(
        stringResource(R.string.backup_footer),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 20.dp),
    )

    pending?.let { b ->
        val on = stringResource(R.string.backup_on)
        val off = stringResource(R.string.backup_off)
        fun flag(v: Boolean) = if (v) on else off
        val sizes = listOf(stringResource(R.string.small), stringResource(R.string.medium), stringResource(R.string.large))
        val lines = buildList {
            b.posterSize?.let { add(stringResource(R.string.poster_size) + ": " + sizes[it]) }
            b.hideAvailable?.let { add(stringResource(R.string.hide_available_titles) + ": " + flag(it)) }
            b.useCustomTabs?.let { add(stringResource(R.string.open_links_in_the_app) + ": " + flag(it)) }
            b.notifyMine?.let { add(stringResource(R.string.backup_line_notify, stringResource(R.string.your_requests)) + ": " + flag(it)) }
            b.notifyPending?.let { add(stringResource(R.string.backup_line_notify, stringResource(R.string.requests_to_approve)) + ": " + flag(it)) }
            b.notifyIssues?.let { add(stringResource(R.string.backup_line_notify, stringResource(R.string.issues)) + ": " + flag(it)) }
            b.notifyEveryMinutes?.let { add(stringResource(R.string.backup_line_interval, it)) }
            if (b.hideScreenshots == true) add(stringResource(R.string.hide_screenshots) + ": " + on)
            if (b.hideRecents == true) add(stringResource(R.string.hide_recents) + ": " + on)
        }
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text(stringResource(R.string.backup_restore_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.backup_restore_body,
                        b.createdAt.take(10).ifBlank { "?" },
                        lines.joinToString("\n") { "• $it" }.ifEmpty { stringResource(R.string.backup_line_nothing) },
                    ),
                )
            },
            confirmButton = {
                TextButton({
                    pending = null
                    val ok = runCatching {
                        SettingsBackups.restore(b, App.settings, App.lock)
                        // The interval and the choices feed the background check.
                        Notifier.schedule(ctx)
                    }.isSuccess
                    say(if (ok) R.string.backup_restored else R.string.backup_restore_failed)
                }) { Text(stringResource(R.string.backup_restore_confirm)) }
            },
            dismissButton = { TextButton({ pending = null }) { Text(stringResource(R.string.lock_cancel)) } },
        )
    }
}
