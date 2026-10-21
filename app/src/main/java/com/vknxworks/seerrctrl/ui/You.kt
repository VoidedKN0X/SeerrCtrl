package com.vknxworks.seerrctrl.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.ManageAccounts
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.vknxworks.seerrctrl.App
import com.vknxworks.seerrctrl.BuildConfig
import com.vknxworks.seerrctrl.R
import com.vknxworks.seerrctrl.data.Tls
import com.vknxworks.seerrctrl.data.UserTypes
import com.vknxworks.seerrctrl.data.int
import com.vknxworks.seerrctrl.data.str
import com.vknxworks.seerrctrl.ui.settings.Group
import com.vknxworks.seerrctrl.ui.settings.GroupRow
import kotlinx.coroutines.launch

private const val SPONSOR_URL = "https://github.com/sponsors/VoidedKN0X"
private const val REPO_URL = "https://github.com/VoidedKN0X/SeerrCtrl"
private const val ISSUES_URL = "https://github.com/VoidedKN0X/SeerrCtrl/issues"
private const val LICENCE_URL = "https://www.gnu.org/licenses/gpl-3.0.html"

/**
 * Your own picture, as in every Ctrl app: your Seerr avatar when there is one (pictures from Gravatar and Plex
 * only when that's turned on), otherwise your initial on a tinted circle.
 */
@Composable
fun ProfileAvatar(size: Dp, initialStyle: androidx.compose.ui.text.TextStyle) {
    val s = App.session
    val name = s.displayName
    val url = s.avatar(s.me.str("avatar"))
    Box(
        Modifier.size(size).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center,
    ) {
        val initial = name.trim().take(1).uppercase()
        if (initial.isEmpty()) {
            Icon(Icons.Filled.AccountCircle, null, tint = MaterialTheme.colorScheme.primary)
        } else {
            Text(initial, style = initialStyle, color = MaterialTheme.colorScheme.primary)
        }
        if (url != null) AsyncImage(url, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    }
}

/** The profile picture at the top right of every tab; tapping it opens the You page (there's no settings tab). */
@Composable
fun ProfileButton() {
    val nav = LocalNav.current
    val label = stringResource(R.string.you_title)
    IconButton(onClick = { nav.go("you") }, modifier = Modifier.semantics { contentDescription = label }) {
        ProfileAvatar(34.dp, MaterialTheme.typography.titleMedium)
    }
}

/** Big left-aligned title for the main tabs, with the profile picture at the end. */
@Composable
fun TabTitle(title: String, modifier: Modifier = Modifier, trailing: @Composable RowScope.() -> Unit = {}) {
    Row(
        modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.weight(1f))
        trailing()
        ProfileButton()
    }
}

/**
 * Opened from the profile picture: your account and server, Settings, log out and the About section, in the
 * same order as the other Ctrl apps.
 */
@Composable
fun YouScreen(onLogout: () -> Unit) {
    val nav = LocalNav.current
    val ctx = LocalContext.current
    val s = App.session
    val me = s.me
    val server = rememberLoad("status") { App.api.obj("/status") }
    val scope = rememberCoroutineScope()
    var confirmLogout by remember { mutableStateOf(false) }
    var repin by remember { mutableStateOf<String?>(null) }
    var repinError by remember { mutableStateOf<String?>(null) }

    Scaffold(topBar = { BackBar(stringResource(R.string.you_title), { nav.back() }) }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {
            Column(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ProfileAvatar(96.dp, MaterialTheme.typography.displaySmall)
                Spacer(Modifier.height(14.dp))
                Text(s.displayName, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                Text(
                    listOfNotNull(UserTypes.name(me.int("userType")), me.str("email")?.takeIf { it != s.displayName }).joinToString(App.str(R.string.list_separator_wide)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            Group {
                GroupRow(
                    stringResource(R.string.you_profile), stringResource(R.string.you_profile_subtitle),
                    Icons.Outlined.Person, showChevron = true, onClick = { nav.go("user/${s.userId}") },
                )
                GroupRow(
                    stringResource(R.string.you_account_settings), stringResource(R.string.you_account_settings_subtitle),
                    Icons.Outlined.ManageAccounts, showChevron = true, onClick = { nav.go("user/${s.userId}/settings") },
                )
            }

            Group(stringResource(R.string.group_server)) {
                GroupRow(
                    s.appTitle,
                    (server.value?.str("version")?.let { stringResource(R.string.server_with_version, App.api.baseUrl, it) } ?: App.api.baseUrl).let { line ->
                        if (App.api.isHttp) stringResource(R.string.two_lines, line, stringResource(R.string.not_encrypted)) else line
                    },
                    Icons.Outlined.Dns,
                )
                if (App.api.pinnedFingerprint.isNotEmpty() || (App.prefs.allowInsecureTls && !App.api.isHttp)) {
                    GroupRow(
                        stringResource(R.string.trusted_certificate),
                        if (App.api.pinnedFingerprint.isEmpty()) stringResource(R.string.trusted_certificate_none)
                        else stringResource(R.string.trusted_certificate_pinned, Tls.pretty(App.api.pinnedFingerprint).take(23)),
                        Icons.Outlined.Lock,
                        onClick = {
                            scope.launch {
                                repin = try { Tls.sha256(Tls.fetchLeaf(App.api.baseUrl)) } catch (e: Exception) {
                                    repinError = e.message ?: e.javaClass.simpleName; null
                                }
                            }
                        },
                    )
                }
            }

            Group {
                GroupRow(
                    stringResource(R.string.settings), stringResource(R.string.settings_subtitle),
                    Icons.Filled.Settings, showChevron = true, onClick = { nav.go("appsettings") },
                )
                GroupRow(
                    stringResource(R.string.log_out),
                    icon = Icons.AutoMirrored.Outlined.Logout,
                    iconTint = MaterialTheme.colorScheme.error,
                    titleColor = MaterialTheme.colorScheme.error,
                    onClick = { confirmLogout = true },
                )
            }

            Group(stringResource(R.string.group_about)) {
                GroupRow(stringResource(R.string.support_title), stringResource(R.string.support_subtitle), Icons.Filled.Favorite, showChevron = true, onClick = { openUrl(ctx, SPONSOR_URL) })
                GroupRow(stringResource(R.string.bugs_title), stringResource(R.string.bugs_subtitle), Icons.Filled.BugReport, showChevron = true, onClick = { openUrl(ctx, ISSUES_URL) })
                GroupRow(stringResource(R.string.source_title), stringResource(R.string.source_subtitle), Icons.Filled.Code, showChevron = true, onClick = { openUrl(ctx, REPO_URL) })
                GroupRow(stringResource(R.string.licence_title), stringResource(R.string.licence_subtitle), Icons.Filled.Gavel, showChevron = true, onClick = { openUrl(ctx, LICENCE_URL) })
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
            title = { Text(stringResource(R.string.trusted_certificate_failed)) },
            text = { Text(msg) },
            confirmButton = { TextButton({ repinError = null }) { Text(stringResource(R.string.lock_ok)) } },
        )
    }
    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            icon = { Icon(Icons.AutoMirrored.Outlined.Logout, null) },
            title = { Text(stringResource(R.string.log_out_title)) },
            text = { Text(stringResource(R.string.log_out_body)) },
            confirmButton = { TextButton({ confirmLogout = false; onLogout() }) { Text(stringResource(R.string.log_out)) } },
            dismissButton = { TextButton({ confirmLogout = false }) { Text(stringResource(R.string.lock_cancel)) } },
        )
    }
}
