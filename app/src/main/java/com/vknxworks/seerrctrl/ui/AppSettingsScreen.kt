package com.vknxworks.seerrctrl.ui

import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Refresh
import com.vknxworks.seerrctrl.data.Perm
import com.vknxworks.seerrctrl.notify.Notifier
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Password
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.vknxworks.seerrctrl.App
import com.vknxworks.seerrctrl.BuildConfig
import com.vknxworks.seerrctrl.R
import com.vknxworks.seerrctrl.data.ThemeMode
import com.vknxworks.seerrctrl.data.Tls
import com.vknxworks.seerrctrl.data.str
import com.vknxworks.seerrctrl.data.LockAgain
import com.vknxworks.seerrctrl.data.LockSettings
import com.vknxworks.seerrctrl.ui.lock.PinRequest
import com.vknxworks.seerrctrl.ui.lock.PinRequests
import com.vknxworks.seerrctrl.ui.lock.lockActivity
import com.vknxworks.seerrctrl.ui.lock.PIN_MIN
import com.vknxworks.seerrctrl.ui.lock.biometricAvailable
import com.vknxworks.seerrctrl.ui.lock.expectReturn
import com.vknxworks.seerrctrl.ui.lock.showBiometricPrompt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val SPONSOR_URL = "https://github.com/sponsors/VoidedKN0X"
private const val REPO_URL = "https://github.com/VoidedKN0X/SeerrCtrl"
private const val ISSUES_URL = "https://github.com/VoidedKN0X/SeerrCtrl/issues"
private const val LICENCE_URL = "https://www.gnu.org/licenses/gpl-3.0.html"

/** In the same order as every Ctrl app. */
private val THEMES = listOf(ThemeMode.System, ThemeMode.Dark, ThemeMode.Light, ThemeMode.Black)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSettingsScreen(onLogout: () -> Unit) {
    val nav = LocalNav.current
    val s = App.settings
    val ctx = LocalContext.current
    val lock = App.lock
    var confirmLogout by remember { mutableStateOf(false) }
    val bioAvailable = remember { biometricAvailable(ctx) }
    val server = rememberLoad("status") { App.api.obj("/status") }
    val scope = rememberCoroutineScope()
    val snack = LocalSnackbar.current
    var repin by remember { mutableStateOf<String?>(null) }
    var repinError by remember { mutableStateOf<String?>(null) }

    Scaffold(topBar = { BackBar("App settings", { nav.back() }) }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {
            SettingsSection(stringResource(R.string.group_appearance)) {
                ChoiceItem(
                    Icons.Outlined.DarkMode,
                    stringResource(R.string.theme),
                    THEMES.map { stringResource(it.label) },
                    THEMES.indexOf(s.theme).coerceAtLeast(0),
                ) { s.theme = THEMES[it] }
                ToggleItem(Icons.Outlined.Palette, stringResource(R.string.dynamic_colour), stringResource(R.string.dynamic_colour_subtitle), s.dynamicColor) { s.dynamicColor = it }
                ChoiceItem(Icons.Outlined.GridView, "Poster size", listOf("Small", "Medium", "Large"), s.posterSize) { s.posterSize = it }
            }

            NotificationSection()

            SettingsSection(stringResource(R.string.group_privacy)) {
                val lockOn = lock.pinSet
                ToggleItem(
                    Icons.Outlined.Lock, stringResource(R.string.lock_app),
                    stringResource(if (s.notifyEnabled) R.string.lock_app_subtitle_notifications else R.string.lock_app_subtitle),
                    lockOn,
                ) { on -> PinRequests.request = if (on) PinRequest.ENABLE else PinRequest.DISABLE }
                if (lockOn) {
                    NavItem(
                        Icons.Outlined.Password, stringResource(R.string.lock_change_pin), stringResource(R.string.lock_change_pin_subtitle, PIN_MIN),
                        trailing = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    ) { PinRequests.request = PinRequest.CHANGE }
                    ToggleItem(
                        Icons.Outlined.Fingerprint, stringResource(R.string.lock_fingerprint),
                        stringResource(if (bioAvailable || lock.biometricEnabled) R.string.lock_fingerprint_settings_subtitle else R.string.lock_fingerprint_unavailable),
                        lock.biometricEnabled, enabled = bioAvailable || lock.biometricEnabled,
                    ) { on ->
                        val act = ctx.lockActivity() as? FragmentActivity
                        if (on && act != null) {
                            val cipher = lock.vault.biometricEnrolCipher()
                            if (cipher == null) scope.launch { snack.showSnackbar(ctx.getString(R.string.lock_fingerprint_setup_failed)) }
                            else showBiometricPrompt(act, ctx.getString(R.string.lock_fingerprint_enable_title), cipher, subtitle = ctx.getString(R.string.lock_fingerprint_enable_subtitle)) { c ->
                                if (!lock.vault.finishBiometricEnrol(c)) scope.launch { snack.showSnackbar(ctx.getString(R.string.lock_fingerprint_setup_failed)) }
                            }
                        } else if (!on) {
                            lock.vault.disableBiometric()
                        }
                    }
                    ChoiceItem(
                        Icons.Outlined.Timer, stringResource(R.string.lock_again), LockAgain.entries.map { stringResource(it.label) },
                        lock.lockAgain.ordinal,
                    ) { lock.lockAgain = LockAgain.entries[it] }
                }
                ToggleItem(
                    Icons.Outlined.VisibilityOff, stringResource(R.string.hide_screenshots),
                    stringResource(R.string.hide_screenshots_subtitle),
                    lock.hideScreenshots,
                ) { lock.hideScreenshots = it }
                ToggleItem(
                    Icons.Outlined.Layers, stringResource(R.string.hide_recents),
                    stringResource(R.string.hide_recents_subtitle),
                    lock.hideRecents,
                ) { lock.hideRecents = it }
                ToggleItem(
                    Icons.Outlined.AccountCircle, "Profile pictures from Gravatar and Plex",
                    "Gravatar and Plex see your IP address and whose profiles you look at. Off shows initials instead",
                    s.externalAvatars,
                ) { s.externalAvatars = it }
            }
            Text(
                stringResource(R.string.privacy_footer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp),
            )

            SettingsSection("Browsing") {
                ToggleItem(Icons.Outlined.VisibilityOff, "Hide available titles", "Leave out what's already on your media server from discover rows", s.hideAvailable) {
                    s.hideAvailable = it
                }
                ToggleItem(Icons.AutoMirrored.Outlined.OpenInNew, "Open links in the app", "Trailers and external pages open in an in-app browser tab", s.useCustomTabs) {
                    s.useCustomTabs = it
                }
            }

            SettingsSection("Server") {
                NavItem(
                    Icons.Outlined.Dns, App.session.appTitle,
                    App.api.baseUrl + (server.value?.str("version")?.let { " · v$it" } ?: "") +
                        (if (App.api.isHttp) "\nNot encrypted (HTTP)" else ""),
                ) {}
                ItemDivider()
                if (App.api.pinnedFingerprint.isNotEmpty() || (App.prefs.allowInsecureTls && !App.api.isHttp)) {
                    NavItem(
                        Icons.Outlined.Lock, "Trusted certificate",
                        if (App.api.pinnedFingerprint.isEmpty()) "None yet. Tap to check and trust the server's certificate"
                        else "SHA-256 ${Tls.pretty(App.api.pinnedFingerprint).take(23)}…  Tap to check the server again",
                    ) {
                        scope.launch {
                            repin = try { Tls.sha256(Tls.fetchLeaf(App.api.baseUrl)) } catch (e: Exception) {
                                repinError = e.message ?: e.javaClass.simpleName; null
                            }
                        }
                    }
                    ItemDivider()
                }
                NavItem(Icons.AutoMirrored.Outlined.Logout, "Log out", "Sign out and forget this server") { confirmLogout = true }
            }

            SettingsSection(stringResource(R.string.group_about)) {
                AboutRow(Icons.Filled.Favorite, stringResource(R.string.support_title), stringResource(R.string.support_subtitle)) {
                    openUrl(ctx, SPONSOR_URL)
                }
                ItemDivider()
                AboutRow(Icons.Filled.BugReport, stringResource(R.string.bugs_title), stringResource(R.string.bugs_subtitle)) {
                    openUrl(ctx, ISSUES_URL)
                }
                ItemDivider()
                AboutRow(Icons.Filled.Code, stringResource(R.string.source_title), stringResource(R.string.source_subtitle)) {
                    openUrl(ctx, REPO_URL)
                }
                ItemDivider()
                AboutRow(Icons.Filled.Gavel, stringResource(R.string.licence_title), stringResource(R.string.licence_subtitle)) {
                    openUrl(ctx, LICENCE_URL)
                }
            }
            Text(
                stringResource(R.string.about_footer, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                textAlign = TextAlign.Center,
            )
        }
    }

    repin?.let { fp ->
        val same = App.api.pinnedFingerprint.isEmpty() || fp.equals(App.api.pinnedFingerprint, ignoreCase = true)
        CertDialog(
            App.api.baseUrl.substringAfter("://").substringBefore('/'), fp, changed = !same,
            onTrust = { App.api.pinSavedServer(fp); repin = null },
            onCancel = { repin = null },
        )
    }
    repinError?.let { msg ->
        AlertDialog(
            onDismissRequest = { repinError = null },
            title = { Text("Couldn't check the certificate") },
            text = { Text(msg) },
            confirmButton = { TextButton({ repinError = null }) { Text("OK") } },
        )
    }
    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            icon = { Icon(Icons.AutoMirrored.Outlined.Logout, null) },
            title = { Text("Log out?") },
            text = { Text("You'll be signed out of this server on this phone.") },
            confirmButton = { TextButton({ confirmLogout = false; onLogout() }) { Text("Log out") } },
            dismissButton = { TextButton({ confirmLogout = false }) { Text("Cancel") } },
        )
    }
}

/** A row in the About section: tappable, with a chevron, like the other Ctrl apps. */
@Composable
private fun AboutRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    NavItem(icon, title, subtitle, trailing = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) }, onClick = onClick)
}

@Composable
private fun NotificationSection() {
    val ctx = LocalContext.current
    val s = App.settings
    val session = App.session
    var canPost by remember { mutableStateOf(Notifier.canPost(ctx)) }
    val pm = remember { ctx.getSystemService(PowerManager::class.java) }
    var unrestricted by remember { mutableStateOf(pm.isIgnoringBatteryOptimizations(ctx.packageName)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        canPost = ok
        if (ok) { s.notifyEnabled = true; App.prefs.resealCookie(); Notifier.schedule(ctx) }
    }
    val batteryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        unrestricted = pm.isIgnoringBatteryOptimizations(ctx.packageName)
    }
    fun resched() = Notifier.schedule(ctx)

    SettingsSection("Notifications") {
        ToggleItem(
            Icons.Outlined.Notifications, "Notifications",
            if (!canPost) "Android isn't allowing notifications for SeerrCtrl yet" else "Checks your server in the background",
            s.notifyEnabled && canPost,
        ) { on ->
            if (on && !canPost) { expectReturn(); launcher.launch(android.Manifest.permission.POST_NOTIFICATIONS) }
            else { s.notifyEnabled = on; App.prefs.resealCookie(); resched() }
        }
        if (s.notifyEnabled && canPost) {
            ItemDivider()
            ToggleItem(null, "Your requests", "Approved, declined, failed, now available", s.notifyMine) { s.notifyMine = it; resched() }
            if (session.can(Perm.MANAGE_REQUESTS)) {
                ItemDivider()
                ToggleItem(null, "Requests to approve", "New requests, with Approve and Decline buttons", s.notifyPending) { s.notifyPending = it; resched() }
            }
            ItemDivider()
            ToggleItem(
                null, "Issues",
                if (session.can(Perm.MANAGE_ISSUES)) "New issues, and replies on issues you reported" else "Replies and fixes on issues you reported",
                s.notifyIssues,
            ) { s.notifyIssues = it; resched() }
            ItemDivider()
            val every = listOf(15, 30, 60)
            ChoiceItem(Icons.Outlined.Timer, "Check every", listOf("15 min", "30 min", "1 hour"), every.indexOf(s.notifyEveryMinutes).coerceAtLeast(0)) {
                s.notifyEveryMinutes = every[it]; resched()
            }
            ItemDivider()
            NavItem(
                Icons.Outlined.BatteryChargingFull, "Background activity",
                if (unrestricted) "Unrestricted, so checks run on time"
                else "Restricted. ColorOS and other phones may delay or skip checks; tap to allow",
            ) {
                expectReturn()
                if (!unrestricted) {
                    batteryLauncher.launch(
                        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${ctx.packageName}"))
                    )
                } else {
                    batteryLauncher.launch(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                }
            }
            ItemDivider()
            NavItem(Icons.Outlined.Refresh, "Check now", "Run a check straight away") { Notifier.checkNow(ctx) }
        }
    }
}
