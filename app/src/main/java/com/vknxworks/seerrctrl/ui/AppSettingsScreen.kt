package com.vknxworks.seerrctrl.ui

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
                    ToggleItem(
                        Icons.Outlined.VisibilityOff, "Hide from recents & screenshots",
                        "Blank preview in the app switcher; blocks screenshots and screen recording", lock.hideContent,
                    ) { lock.hideContent = it }
                    ItemDivider()
                    NavItem(Icons.Outlined.Password, "Change PIN") { pinDialog = PinMode.Change }
                }
            }

            SettingsSection("Server") {
                NavItem(
                    Icons.Outlined.Dns, App.session.appTitle,
                    App.api.baseUrl + (server.value?.str("version")?.let { " · v$it" } ?: ""),
                ) {}
                ItemDivider()
                NavItem(Icons.AutoMirrored.Outlined.Logout, "Log out", "Sign out and forget this server") { confirmLogout = true }
            }

            SettingsSection("About") {
                NavItem(Icons.Outlined.Info, "SeerrCtrl $appVersion", "VKNXWorks · unofficial app for Seerr, Jellyseerr and Overseerr") {}
            }
        }
    }

    pinDialog?.let { mode -> PinSetupDialog(mode) { pinDialog = null } }
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
