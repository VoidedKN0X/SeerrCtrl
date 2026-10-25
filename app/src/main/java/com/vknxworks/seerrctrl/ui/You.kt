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
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.ManageAccounts
import androidx.compose.material.icons.outlined.Person
import com.vknxworks.seerrctrl.ui.components.AlertDialog
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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import com.vknxworks.seerrctrl.ui.components.Button
import com.vknxworks.seerrctrl.ui.components.CtrlTopBar
import com.vknxworks.seerrctrl.ui.theme.CtrlColors
import com.vknxworks.seerrctrl.ui.theme.CtrlType

private const val SPONSOR_URL = "https://github.com/sponsors/VoidedKN0X"
private const val REPO_URL = "https://github.com/VoidedKN0X/SeerrCtrl"
private const val ISSUES_URL = "https://github.com/VoidedKN0X/SeerrCtrl/issues"
private const val LICENCE_URL = "https://www.gnu.org/licenses/gpl-3.0.html"

/** Up to two initials of a name ("Axel Smith" → "AS", "axel" → "AX"). */
fun initialsOf(name: String): String {
    val words = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return when {
        words.isEmpty() -> ""
        words.size == 1 -> words[0].take(2)
        else -> "${words[0].first()}${words[1].first()}"
    }.uppercase()
}

/**
 * Your own picture, as in every Ctrl app: your Seerr avatar when there is one (pictures from Gravatar and Plex
 * only when that's turned on), otherwise your initials on a rounded square. [accent] gives the accent fill
 * of the You page; otherwise it's the surface fill with a thin border of the top bar.
 */
@Composable
fun ProfileAvatar(size: Dp, initialStyle: androidx.compose.ui.text.TextStyle, accent: Boolean = false) {
    val s = App.session
    val name = s.displayName
    val url = s.avatar(s.me.str("avatar"))
    val shape = RoundedCornerShape(if (accent) size * 0.32f else size * 0.318f)
    Box(
        Modifier.size(size).clip(shape)
            .background(if (accent) CtrlColors.Accent else CtrlColors.Surface)
            .then(if (accent) Modifier else Modifier.border(1.dp, CtrlColors.ControlOutline, shape)),
        contentAlignment = Alignment.Center,
    ) {
        val initials = initialsOf(name)
        val fg = if (accent) CtrlColors.OnAccent else CtrlColors.Text
        if (initials.isEmpty()) {
            Icon(Icons.Outlined.Person, null, tint = fg)
        } else {
            Text(initials, style = initialStyle, color = fg)
        }
        if (url != null) AsyncImage(url, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    }
}

/**
 * The 44 dp profile picture at the top right of every tab; tapping it opens the You page (there's no
 * settings tab). A small accent square at its corner shows you're connected to your server.
 */
@Composable
fun ProfileButton() {
    val nav = LocalNav.current
    val label = stringResource(R.string.you_title)
    val src = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    Box(
        Modifier
            .size(48.dp)
            .semantics(mergeDescendants = true) { contentDescription = label }
            .pressScale(src, 0.92f)
            .clickable(src, indication = null, role = Role.Button) { nav.go("you") },
        contentAlignment = Alignment.Center,
    ) {
        ProfileAvatar(44.dp, MaterialTheme.typography.labelLarge)
        if (App.session.me != null) {
            Box(
                Modifier.align(Alignment.BottomEnd).size(14.dp).clip(RoundedCornerShape(5.dp))
                    .background(CtrlColors.Background).padding(2.dp).clip(RoundedCornerShape(3.dp)).background(CtrlColors.Accent)
            )
        }
    }
}

/**
 * Header of the main tabs in the Signal look: the SEERR/CTRL wordmark and the profile picture on top, then
 * the tab's name as a big headline.
 */
@Composable
fun TabTitle(title: String, modifier: Modifier = Modifier, trailing: @Composable RowScope.() -> Unit = {}) {
    Column(modifier.fillMaxWidth()) {
        CtrlTopBar(stringResource(R.string.wordmark_name)) {
            trailing()
            ProfileButton()
        }
        Text(
            title, style = CtrlType.Headline,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 10.dp),
        )
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
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ProfileAvatar(68.dp, MaterialTheme.typography.headlineSmall, accent = true)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(s.displayName, style = MaterialTheme.typography.headlineSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        App.api.baseUrl.substringAfter("://").trimEnd('/'),
                        style = CtrlType.Data, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        listOfNotNull(UserTypes.name(me.int("userType")), me.str("email")?.takeIf { it != s.displayName }).joinToString(App.str(R.string.list_separator_wide)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                    )
                }
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
                    Icons.Outlined.Tune, showChevron = true, onClick = { nav.go("appsettings") },
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
                GroupRow(stringResource(R.string.bugs_title), stringResource(R.string.bugs_subtitle), Icons.Outlined.BugReport, showChevron = true, onClick = { openUrl(ctx, ISSUES_URL) })
                GroupRow(stringResource(R.string.source_title), stringResource(R.string.source_subtitle), Icons.Outlined.Code, showChevron = true, onClick = { openUrl(ctx, REPO_URL) })
                GroupRow(stringResource(R.string.licence_title), stringResource(R.string.licence_subtitle), Icons.Outlined.Gavel, showChevron = true, onClick = { openUrl(ctx, LICENCE_URL) })
            }
            // Support VKNXWorks: the accent button at the bottom, as in every Ctrl app.
            Button(
                onClick = { openUrl(ctx, SPONSOR_URL) },
                modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 16.dp).height(56.dp),
            ) {
                Icon(Icons.Outlined.FavoriteBorder, null, Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.support_title), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            }
            Text(
                stringResource(R.string.about_footer, BuildConfig.VERSION_NAME),
                style = CtrlType.Data,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                textAlign = TextAlign.Center,
            )
            Text(
                stringResource(R.string.ctrl_footer),
                style = CtrlType.Label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 8.dp),
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
