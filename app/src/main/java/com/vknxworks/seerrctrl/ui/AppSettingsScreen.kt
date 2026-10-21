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
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Password
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.vknxworks.seerrctrl.App
import com.vknxworks.seerrctrl.R
import com.vknxworks.seerrctrl.data.ThemeMode
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

/** In the same order as every Ctrl app. */
private val THEMES = listOf(ThemeMode.System, ThemeMode.Dark, ThemeMode.Light, ThemeMode.Black)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSettingsScreen() {
    val nav = LocalNav.current
    val s = App.settings
    val ctx = LocalContext.current
    val lock = App.lock
    val bioAvailable = remember { biometricAvailable(ctx) }
    val scope = rememberCoroutineScope()
    val snack = LocalSnackbar.current

    Scaffold(topBar = { BackBar(stringResource(R.string.settings), { nav.back() }) }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {
            SettingsSection(stringResource(R.string.group_appearance)) {
                ChoiceItem(
                    Icons.Outlined.DarkMode,
                    stringResource(R.string.theme),
                    THEMES.map { stringResource(it.label) },
                    THEMES.indexOf(s.theme).coerceAtLeast(0),
                ) { s.theme = THEMES[it] }
                ToggleItem(Icons.Outlined.Palette, stringResource(R.string.dynamic_colour), stringResource(R.string.dynamic_colour_subtitle), s.dynamicColor) { s.dynamicColor = it }
                ChoiceItem(Icons.Outlined.GridView, stringResource(R.string.poster_size), listOf(App.str(R.string.small), App.str(R.string.medium), App.str(R.string.large)), s.posterSize) { s.posterSize = it }
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
                    Icons.Outlined.AccountCircle, stringResource(R.string.profile_pictures_from_gravatar_and),
                    stringResource(R.string.gravatar_and_plex_see_your_ip),
                    s.externalAvatars,
                ) { s.externalAvatars = it }
            }
            Text(
                stringResource(R.string.privacy_footer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp),
            )

            SettingsSection(stringResource(R.string.browsing)) {
                ToggleItem(Icons.Outlined.VisibilityOff, stringResource(R.string.hide_available_titles), stringResource(R.string.leave_out_what_s_already_on_your), s.hideAvailable) {
                    s.hideAvailable = it
                }
                ToggleItem(Icons.AutoMirrored.Outlined.OpenInNew, stringResource(R.string.open_links_in_the_app), stringResource(R.string.trailers_and_external_pages_open_in), s.useCustomTabs) {
                    s.useCustomTabs = it
                }
            }
        }
    }
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

    SettingsSection(stringResource(R.string.notifications)) {
        ToggleItem(
            Icons.Outlined.Notifications, stringResource(R.string.notifications),
            if (!canPost) stringResource(R.string.android_isn_t_allowing_notifications) else stringResource(R.string.checks_your_server_in_the_background),
            s.notifyEnabled && canPost,
        ) { on ->
            if (on && !canPost) { expectReturn(); launcher.launch(android.Manifest.permission.POST_NOTIFICATIONS) }
            else { s.notifyEnabled = on; App.prefs.resealCookie(); resched() }
        }
        if (s.notifyEnabled && canPost) {
            ItemDivider()
            ToggleItem(null, stringResource(R.string.your_requests), stringResource(R.string.approved_declined_failed_now), s.notifyMine) { s.notifyMine = it; resched() }
            if (session.can(Perm.MANAGE_REQUESTS)) {
                ItemDivider()
                ToggleItem(null, stringResource(R.string.requests_to_approve), stringResource(R.string.new_requests_with_approve_and), s.notifyPending) { s.notifyPending = it; resched() }
            }
            ItemDivider()
            ToggleItem(
                null, stringResource(R.string.issues),
                if (session.can(Perm.MANAGE_ISSUES)) stringResource(R.string.new_issues_and_replies_on_issues_you) else stringResource(R.string.replies_and_fixes_on_issues_you),
                s.notifyIssues,
            ) { s.notifyIssues = it; resched() }
            ItemDivider()
            val every = listOf(15, 30, 60)
            ChoiceItem(Icons.Outlined.Timer, stringResource(R.string.check_every), listOf(App.str(R.string.15_min), App.str(R.string.30_min), App.str(R.string.1_hour)), every.indexOf(s.notifyEveryMinutes).coerceAtLeast(0)) {
                s.notifyEveryMinutes = every[it]; resched()
            }
            ItemDivider()
            NavItem(
                Icons.Outlined.BatteryChargingFull, stringResource(R.string.background_activity),
                if (unrestricted) stringResource(R.string.unrestricted_so_checks_run_on_time)
                else stringResource(R.string.restricted_coloros_and_other_phones),
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
            NavItem(Icons.Outlined.Refresh, stringResource(R.string.check_now), stringResource(R.string.run_a_check_straight_away)) { Notifier.checkNow(ctx) }
        }
    }
}
