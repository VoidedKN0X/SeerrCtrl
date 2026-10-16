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
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LightMode
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.vknxworks.seerrctrl.App
import com.vknxworks.seerrctrl.data.ThemeMode
import com.vknxworks.seerrctrl.data.Tls
import com.vknxworks.seerrctrl.data.str
import com.vknxworks.seerrctrl.ui.lock.PIN_MAX
import com.vknxworks.seerrctrl.ui.lock.PIN_MIN
import com.vknxworks.seerrctrl.ui.lock.biometricAvailable
import com.vknxworks.seerrctrl.ui.lock.showBiometricPrompt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSettingsScreen(onLogout: () -> Unit) {
    val nav = LocalNav.current
    val s = App.settings
    val ctx = LocalContext.current
    val lock = App.lock
    var pinDialog by remember { mutableStateOf<PinMode?>(null) }
    var confirmLogout by remember { mutableStateOf(false) }
    val bioAvailable = remember { biometricAvailable(ctx) }
    val appVersion = remember {
        try { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName } catch (_: Exception) { null } ?: "?"
    }
    val server = rememberLoad("status") { App.api.obj("/status") }
    val scope = rememberCoroutineScope()
    var repin by remember { mutableStateOf<String?>(null) }
    var repinError by remember { mutableStateOf<String?>(null) }

    Scaffold(topBar = { BackBar("App settings", { nav.popBackStack() }) }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {
            SettingsSection("Appearance") {
                ChoiceItem(
                    when (s.theme) { ThemeMode.Light -> Icons.Outlined.LightMode; ThemeMode.Dark -> Icons.Outlined.DarkMode; ThemeMode.System -> Icons.Outlined.Contrast },
                    "Theme", listOf("System", "Light", "Dark"), s.theme.ordinal,
                ) { s.theme = ThemeMode.entries[it] }
                ItemDivider()
                ToggleItem(Icons.Outlined.Palette, "Material You colours", "Match the app to your wallpaper", s.dynamicColor) { s.dynamicColor = it }
                ItemDivider()
                ToggleItem(Icons.Outlined.DarkMode, "Pure black", "True black backgrounds in dark mode", s.pureBlack) { s.pureBlack = it }
                ItemDivider()
                ChoiceItem(Icons.Outlined.GridView, "Poster size", listOf("Small", "Medium", "Large"), s.posterSize) { s.posterSize = it }
            }

            SettingsSection("Browsing") {
                ToggleItem(Icons.Outlined.VisibilityOff, "Hide available titles", "Leave out what's already on your media server from discover rows", s.hideAvailable) {
                    s.hideAvailable = it
                }
                ItemDivider()
                ToggleItem(Icons.AutoMirrored.Outlined.OpenInNew, "Open links in the app", "Trailers and external pages open in an in-app browser tab", s.useCustomTabs) {
                    s.useCustomTabs = it
                }
            }

            NotificationSection()

            SettingsSection("Security") {
                ToggleItem(Icons.Outlined.Lock, "App lock", "Ask for a PIN when opening the app", lock.pinSet) { on ->
                    pinDialog = if (on) PinMode.New else PinMode.Disable
                }
                if (lock.pinSet) {
                    ItemDivider()
                    ToggleItem(
                        Icons.Outlined.Fingerprint, "Unlock with fingerprint",
                        if (bioAvailable) "Your PIN still works as a backup" else "No secure fingerprint unlock is set up on this phone",
                        lock.biometricEnabled, enabled = bioAvailable,
                    ) { on ->
                        val act = ctx as? FragmentActivity
                        if (on && act != null) showBiometricPrompt(act, "Confirm to turn on") { lock.biometricEnabled = true }
                        else lock.biometricEnabled = false
                    }
                    ItemDivider()
                    val afters = listOf(0, 30, 60, 300)
                    ChoiceItem(
                        Icons.Outlined.Timer, "Lock again after leaving the app", listOf("Instantly", "30 s", "1 min", "5 min"),
                        afters.indexOf(lock.lockAfterSeconds).coerceAtLeast(0),
                    ) { lock.lockAfterSeconds = afters[it] }
                    ItemDivider()
                    NavItem(Icons.Outlined.Password, "Change PIN") { pinDialog = PinMode.Change }
                }
                ItemDivider()
                ToggleItem(
                    Icons.Outlined.VisibilityOff, "Hide content in screenshots & recents",
                    "Blocks screenshots and screen recording. Server settings are always hidden", lock.hideContent,
                ) { lock.hideContent = it }
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

            SettingsSection("About") {
                NavItem(Icons.Outlined.Info, "SeerrCtrl $appVersion", "VKNXWorks · unofficial app for Seerr, Jellyseerr and Overseerr") {}
            }
        }
    }

    pinDialog?.let { mode -> PinSetupDialog(mode) { pinDialog = null } }
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

enum class PinMode { New, Change, Disable }

@Composable
private fun PinSetupDialog(mode: PinMode, onDismiss: () -> Unit) {
    val lock = App.lock
    val needsCurrent = mode != PinMode.New
    val needsNew = mode != PinMode.Disable
    var current by remember { mutableStateOf("") }
    var first by remember { mutableStateOf("") }
    var second by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var working by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    fun digits(v: String) = v.filter { it.isDigit() }.take(PIN_MAX)

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.Lock, null) },
        title = { Text(when (mode) { PinMode.New -> "Set a PIN"; PinMode.Change -> "Change PIN"; PinMode.Disable -> "Turn off app lock" }) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (needsCurrent) PinField("Current PIN", current) { current = digits(it) }
                if (needsNew) {
                    PinField("New PIN ($PIN_MIN–$PIN_MAX digits)", first) { first = digits(it) }
                    PinField("Repeat new PIN", second) { second = digits(it) }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(enabled = !working, onClick = {
                scope.launch {
                    working = true
                    val currentOk = !needsCurrent || withContext(Dispatchers.Default) { lock.checkPin(current) }
                    error = when {
                        !currentOk -> "Current PIN is wrong"
                        needsNew && first.length < PIN_MIN -> "Use at least $PIN_MIN digits"
                        needsNew && first != second -> "The PINs don't match"
                        needsNew && first.toSet().size == 1 -> "Pick a PIN that isn't one repeated digit"
                        else -> null
                    }
                    if (error == null) {
                        withContext(Dispatchers.Default) { if (needsNew) lock.setPin(first) else lock.clearPin() }
                        onDismiss()
                    }
                    working = false
                }
            }) { Text(if (mode == PinMode.Disable) "Turn off" else "Save") }
        },
        dismissButton = { TextButton(onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun PinField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) }, singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = Modifier.fillMaxWidth(),
    )
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
        if (ok) { s.notifyEnabled = true; Notifier.schedule(ctx) }
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
            if (on && !canPost) launcher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            else { s.notifyEnabled = on; resched() }
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
