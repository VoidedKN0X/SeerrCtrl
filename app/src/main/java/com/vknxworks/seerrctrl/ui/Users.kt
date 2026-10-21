package com.vknxworks.seerrctrl.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.vknxworks.seerrctrl.App
import com.vknxworks.seerrctrl.data.MediaServer
import com.vknxworks.seerrctrl.data.Perm
import com.vknxworks.seerrctrl.data.UserTypes
import com.vknxworks.seerrctrl.data.bool
import com.vknxworks.seerrctrl.data.int
import com.vknxworks.seerrctrl.data.jsonOf
import com.vknxworks.seerrctrl.data.long
import com.vknxworks.seerrctrl.data.obj
import com.vknxworks.seerrctrl.data.objects
import com.vknxworks.seerrctrl.data.objs
import com.vknxworks.seerrctrl.data.prettyDate
import com.vknxworks.seerrctrl.data.str
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import com.vknxworks.seerrctrl.R

// ---------------- profile ----------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(id: Int) {
    val nav = LocalNav.current
    val closeScreen = rememberCloseScreen()
    val s = App.session
    val act = rememberActions()
    val self = id == s.userId
    val load = rememberLoad(id) {
        val u = App.api.obj("/user/$id")
        val q = try { App.api.obj("/user/$id/quota") } catch (_: Exception) { null }
        u to q
    }
    var confirmDelete by remember { mutableStateOf(false) }
    val canManage = !self && s.can(Perm.MANAGE_USERS)

    Scaffold(
        topBar = {
            BackBar(if (self) stringResource(R.string.your_profile) else load.value?.first?.str("displayName") ?: stringResource(R.string.user), { nav.back() }) {
                IconButton({ nav.go("watchlist/$id") }) { Icon(Icons.Outlined.Bookmark, stringResource(R.string.watchlist)) }
                if (self || s.can(Perm.MANAGE_USERS)) IconButton({ nav.go("user/$id/settings") }) { Icon(Icons.Outlined.Settings, stringResource(R.string.settings)) }
                if (canManage && id != 1) IconButton({ confirmDelete = true }) { Icon(Icons.Outlined.Delete, stringResource(R.string.delete_user)) }
            }
        },
    ) { pad ->
        Loaded(load, Modifier.padding(pad)) { (u, q) ->
            Column(Modifier.padding(pad).fillMaxSize()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Avatar(u.str("avatar"), u.str("displayName") ?: "?", 64.dp)
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(u.str("displayName") ?: "", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            listOfNotNull(UserTypes.name(u.int("userType")), roleOf(u)).joinToString(App.str(R.string.list_separator)),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        u.str("email")?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                        prettyDate(u.str("createdAt"))?.let { Text(stringResource(R.string.joined, it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
                Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard(stringResource(R.string.requests), "${u.int("requestCount")}", Modifier.weight(1f))
                    q.obj("movie")?.takeIf { it.int("limit") > 0 }?.let { m ->
                        StatCard(stringResource(R.string.movie_quota), "${m.int("used")}/${m.int("limit")}", Modifier.weight(1f), stringResource(R.string.per_days, m.int("days")))
                    }
                    q.obj("tv")?.takeIf { it.int("limit") > 0 }?.let { t ->
                        StatCard(stringResource(R.string.season_quota), "${t.int("used")}/${t.int("limit")}", Modifier.weight(1f), stringResource(R.string.per_days, t.int("days")))
                    }
                }
                if (self || s.can(Perm.MANAGE_REQUESTS, Perm.REQUEST_VIEW, any = true)) {
                    Box(Modifier.weight(1f)) { RequestsTab(userId = id) }
                }
            }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_this_user)) },
            text = { Text(stringResource(R.string.their_requests_are_removed_too_this)) },
            confirmButton = {
                TextButton({
                    confirmDelete = false
                    act.run(App.str(R.string.user_deleted), onOk = { closeScreen() }) { App.api.delete("/user/$id") }
                }) { Text(stringResource(R.string.lock_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton({ confirmDelete = false }) { Text(stringResource(R.string.lock_cancel)) } },
        )
    }
}

fun roleOf(u: JSONObject?): String? = when {
    u.int("id") == 1 -> App.str(R.string.owner)
    Perm.has(u.long("permissions"), Perm.ADMIN) -> App.str(R.string.admin)
    else -> null
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier, sub: String? = null) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            sub?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

// ---------------- watchlist & blocklist ----------------

@Composable
fun WatchlistScreen(userId: Int) {
    val nav = LocalNav.current
    val self = userId == App.session.userId
    Scaffold(topBar = { BackBar(if (self) stringResource(R.string.your_watchlist) else stringResource(R.string.watchlist), { nav.back() }) }) { pad ->
        PagedTileGrid(if (self) "/discover/watchlist" else "/user/$userId/watchlist", emptyMap(), Modifier.padding(pad))
    }
}

@Composable
fun BlocklistScreen() {
    val nav = LocalNav.current
    val act = rememberActions()
    var search by remember { mutableStateOf("") }
    var reload by remember { mutableIntStateOf(0) }
    val load = rememberLoad(search, reload) {
        delay(if (search.isEmpty()) 0 else 350)
        App.api.obj("/blocklist", mapOf("take" to 100, "search" to search.ifBlank { null })).objs("results")
    }
    Scaffold(topBar = { BackBar(stringResource(R.string.blocklist), { nav.back() }) }) { pad ->
        Column(Modifier.padding(pad)) {
            OutlinedTextField(
                search, { search = it }, placeholder = { Text(stringResource(R.string.search_blocklist)) }, leadingIcon = { Icon(Icons.Outlined.Search, null) },
                singleLine = true, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )
            Loaded(load) { list ->
                LazyColumn(contentPadding = ListBottom) {
                    items(list, key = { it.int("id") }) { b ->
                        ListItem(
                            headlineContent = { Text(b.str("title") ?: stringResource(R.string.tmdb_2, b.int("tmdbId"))) },
                            supportingContent = {
                                Text(listOfNotNull(
                                    if (b.str("mediaType") == "tv") App.str(R.string.series_2) else App.str(R.string.movie_2),
                                    b.obj("user").str("displayName")?.let { App.str(R.string.by_2, it) },
                                    prettyDate(b.str("createdAt")),
                                    b.str("blocklistedTags")?.let { App.str(R.string.tags_2, it) },
                                ).joinToString(App.str(R.string.list_separator)))
                            },
                            trailingContent = if (App.session.can(Perm.MANAGE_BLOCKLIST)) ({
                                IconButton({
                                    act.run(App.str(R.string.removed_from_blocklist), onOk = { reload++ }) {
                                        App.api.delete("/blocklist/${b.int("tmdbId")}", query = mapOf("mediaType" to b.str("mediaType")))
                                    }
                                }) { Icon(Icons.Outlined.Close, stringResource(R.string.remove)) }
                            }) else null,
                            modifier = Modifier.clickable { nav.openMedia(b.str("mediaType"), b.int("tmdbId")) },
                        )
                    }
                    if (list.isEmpty()) item { EmptyBox(Icons.Outlined.Shield, stringResource(R.string.nothing_is_blocklisted)) }
                }
            }
        }
    }
}

// ---------------- users ----------------

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun UsersScreen() {
    val nav = LocalNav.current
    val act = rememberActions()
    var query by remember { mutableStateOf("") }
    var sort by remember { mutableStateOf("displayname") }
    var reload by remember { mutableIntStateOf(0) }
    val users = remember(query, sort, reload) { mutableStateListOf<JSONObject>() }
    var total by remember(query, sort, reload) { mutableIntStateOf(-1) }
    var loading by remember { mutableStateOf(false) }
    val selected = remember { mutableStateListOf<Int>() }
    var menu by remember { mutableStateOf(false) }
    var sortMenu by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf<String?>(null) } // create | jellyfin | plex | perms
    val list = rememberLazyListState()

    suspend fun more() {
        if (loading || (total >= 0 && users.size >= total)) return
        loading = true
        try {
            if (query.isNotBlank()) delay(300)
            val j = App.api.obj("/user", mapOf("take" to 25, "skip" to users.size, "sort" to sort, "q" to query.ifBlank { null }))
            users.addAll(j.objs("results").filter { u -> users.none { it.int("id") == u.int("id") } })
            total = j.obj("pageInfo").int("results", users.size)
        } catch (e: Exception) { act.say(e.message ?: App.str(R.string.couldn_t_load_users)) }
        loading = false
    }
    LaunchedEffect(query, sort, reload) { more() }
    val nearEnd by remember { derivedStateOf { (list.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) >= list.layoutInfo.totalItemsCount - 5 } }
    LaunchedEffect(nearEnd) { if (nearEnd && users.isNotEmpty()) more() }

    Scaffold(
        topBar = {
            if (selected.isNotEmpty()) {
                BackBar(pluralStringResource(R.plurals.users_selected, selected.size, selected.size), { selected.clear() }) {
                    TextButton({ dialog = "perms" }) { Text(stringResource(R.string.edit_permissions)) }
                }
            } else {
                BackBar(stringResource(R.string.users), { nav.back() }, subtitle = if (total >= 0) pluralStringResource(R.plurals.users_count, total, total) else null) {
                    Box {
                        TextButton({ sortMenu = true }) { Text(stringResource(R.string.sort)) }
                        DropdownMenu(sortMenu, { sortMenu = false }) {
                            listOf("displayname" to App.str(R.string.name), "created" to App.str(R.string.joined_2), "requests" to App.str(R.string.requests), "usertype" to App.str(R.string.type), "role" to App.str(R.string.role)).forEach { (k, l) ->
                                DropdownMenuItem({ Text(l) }, { sort = k; sortMenu = false })
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            Box {
                FloatingActionButton({ menu = true }) { Icon(Icons.Outlined.PersonAdd, stringResource(R.string.add_users)) }
                DropdownMenu(menu, { menu = false }) {
                    DropdownMenuItem({ Text(stringResource(R.string.create_local_user)) }, { menu = false; dialog = "create" }, leadingIcon = { Icon(Icons.Outlined.PersonAdd, null) })
                    val ms = App.session.mediaServer
                    if (ms == MediaServer.Jellyfin || ms == MediaServer.Emby) {
                        DropdownMenuItem({ Text(stringResource(R.string.import_users, ms.label)) }, { menu = false; dialog = "jellyfin" }, leadingIcon = { Icon(Icons.Outlined.CloudDownload, null) })
                    }
                    if (ms == MediaServer.Plex) {
                        DropdownMenuItem({ Text(stringResource(R.string.import_plex_users)) }, { menu = false; dialog = "plex" }, leadingIcon = { Icon(Icons.Outlined.CloudDownload, null) })
                    }
                }
            }
        },
    ) { pad ->
        Column(Modifier.padding(pad)) {
            OutlinedTextField(
                query, { query = it }, placeholder = { Text(stringResource(R.string.search_users)) }, leadingIcon = { Icon(Icons.Outlined.Search, null) },
                singleLine = true, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )
            LazyColumn(state = list, contentPadding = ListBottom) {
                items(users, key = { it.int("id") }) { u ->
                    val id = u.int("id")
                    val sel = id in selected
                    ListItem(
                        leadingContent = {
                            if (selected.isNotEmpty()) Checkbox(sel, { if (sel) selected.remove(id) else selected.add(id) })
                            else Avatar(u.str("avatar"), u.str("displayName") ?: "?")
                        },
                        headlineContent = { Text(u.str("displayName") ?: u.str("email") ?: stringResource(R.string.user_2, id)) },
                        supportingContent = {
                            Text(listOfNotNull(UserTypes.name(u.int("userType")), roleOf(u), pluralStringResource(R.plurals.requests_count, u.int("requestCount"), u.int("requestCount"))).joinToString(App.str(R.string.list_separator)))
                        },
                        colors = ListItemDefaults.colors(containerColor = if (sel) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent),
                        modifier = Modifier.combinedClickable(
                            onClick = {
                                if (selected.isNotEmpty()) { if (sel) selected.remove(id) else selected.add(id) }
                                else nav.go("user/$id")
                            },
                            onLongClick = { if (id != 1 && id != App.session.userId) selected.add(id) },
                        ),
                    )
                }
                if (loading) item { Box(Modifier.fillMaxWidth().padding(24.dp), Alignment.Center) { CircularProgressIndicator() } }
                if (!loading && users.isEmpty()) item { EmptyBox(Icons.Outlined.Group, stringResource(R.string.no_users_found)) }
            }
        }
    }

    when (dialog) {
        "create" -> CreateUserDialog({ dialog = null }) { dialog = null; reload++ }
        "jellyfin", "plex" -> ImportUsersDialog(dialog == "plex", { dialog = null }) { dialog = null; reload++ }
        "perms" -> {
            var perms by remember { mutableLongStateOf(0L) }
            AlertDialog(
                onDismissRequest = { dialog = null },
                title = { Text(pluralStringResource(R.plurals.permissions_for_users, selected.size, selected.size)) },
                text = { Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState())) { PermissionEditor(perms) { perms = it } } },
                confirmButton = {
                    TextButton({
                        val ids = selected.toList()
                        dialog = null
                        act.run(App.str(R.string.permissions_updated), onOk = { selected.clear(); reload++ }) {
                            App.api.put("/user", JSONObject().put("ids", JSONArray(ids)).put("permissions", perms))
                        }
                    }) { Text(stringResource(R.string.save)) }
                },
                dismissButton = { TextButton({ dialog = null }) { Text(stringResource(R.string.lock_cancel)) } },
            )
        }
    }
}

@Composable
private fun CreateUserDialog(onDismiss: () -> Unit, onDone: () -> Unit) {
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val emailOn = App.session.public.bool("emailEnabled")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.create_local_user)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.display_name)) }, singleLine = true)
                OutlinedTextField(email, { email = it }, label = { Text(stringResource(R.string.email)) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
                OutlinedTextField(
                    password, { password = it }, label = { Text(if (emailOn) stringResource(R.string.password_blank_email_them_one) else stringResource(R.string.password)) },
                    singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = PasswordKeyboard,
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton({
                scope.launch {
                    try {
                        App.api.post("/user", jsonOf("email" to email.trim(), "username" to name.trim().ifEmpty { null }, "password" to password.ifEmpty { null }))
                        onDone()
                    } catch (e: Exception) { error = e.message }
                }
            }, enabled = email.isNotBlank() && (emailOn || password.length >= 8)) { Text(stringResource(R.string.create)) }
        },
        dismissButton = { TextButton(onDismiss) { Text(stringResource(R.string.lock_cancel)) } },
    )
}

@Composable
private fun ImportUsersDialog(plex: Boolean, onDismiss: () -> Unit, onDone: () -> Unit) {
    val scope = rememberCoroutineScope()
    val load = rememberLoad(plex) { App.api.arr(if (plex) "/settings/plex/users" else "/settings/jellyfin/users").objects() }
    val picked = remember { mutableStateListOf<String>() }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.import_users, if (plex) "Plex" else App.session.mediaServer.label)) },
        text = {
            Box(Modifier.heightIn(min = 80.dp, max = 460.dp)) {
                Loaded(load) { list ->
                    LazyColumn {
                        items(list, key = { it.str("id") ?: it.str("username") ?: "" }) { u ->
                            val id = u.str("id") ?: return@items
                            Row(Modifier.fillMaxWidth().clickable { if (id in picked) picked.remove(id) else picked.add(id) }, verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(id in picked, { if (it) picked.add(id) else picked.remove(id) })
                                Text(u.str("username") ?: u.str("title") ?: u.str("email") ?: id)
                            }
                        }
                        if (list.isEmpty()) item { Text(stringResource(R.string.no_users_to_import)) }
                    }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.BottomStart)) }
            }
        },
        confirmButton = {
            TextButton({
                scope.launch {
                    try {
                        if (plex) App.api.post("/user/import-from-plex", JSONObject().put("plexIds", JSONArray(picked.toList())))
                        else App.api.post("/user/import-from-jellyfin", JSONObject().put("jellyfinUserIds", JSONArray(picked.toList())))
                        onDone()
                    } catch (e: Exception) { error = e.message }
                }
            }, enabled = picked.isNotEmpty()) { Text(stringResource(R.string.import_count, picked.size)) }
        },
        dismissButton = { TextButton(onDismiss) { Text(stringResource(R.string.lock_cancel)) } },
    )
}

/** Checkbox tree of Seerr permissions. Children of a checked permission are implied. */
@Composable
fun PermissionEditor(value: Long, enabled: Boolean = true, onChange: (Long) -> Unit) {
    val admin = value and Perm.ADMIN != 0L
    Column {
        Perm.tree.forEach { node ->
            val on = value and node.bit != 0L
            PermRow(node.name, node.desc, on || (admin && node.bit != Perm.ADMIN), enabled && (!admin || node.bit == Perm.ADMIN), 0) {
                onChange(if (it) value or node.bit else value and node.bit.inv())
            }
            node.children.forEach { c ->
                val implied = admin || on
                PermRow(c.name, c.desc, implied || value and c.bit != 0L, enabled && !implied, 1) {
                    onChange(if (it) value or c.bit else value and c.bit.inv())
                }
            }
        }
    }
}

@Composable
private fun PermRow(name: String, desc: String, checked: Boolean, enabled: Boolean, depth: Int, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(enabled) { onChange(!checked) }.padding(start = (depth * 28).dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked, onChange, enabled = enabled)
        Column(Modifier.weight(1f)) {
            Text(name, fontWeight = if (depth == 0) FontWeight.SemiBold else FontWeight.Normal)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ---------------- user settings ----------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserSettingsScreen(id: Int) {
    // Notification tokens (Pushbullet, Pushover) and passwords are shown here: never in screenshots or recents.
    com.vknxworks.seerrctrl.SecureWindow()
    val nav = LocalNav.current
    val s = App.session
    val self = id == s.userId
    val tabs = buildList {
        add(App.str(R.string.general)); add(App.str(R.string.password)); add(App.str(R.string.notifications))
        if (!self && s.can(Perm.MANAGE_USERS) && id != 1 || self) add(App.str(R.string.permissions))
    }
    var tab by remember { mutableIntStateOf(0) }
    Scaffold(topBar = { BackBar(if (self) stringResource(R.string.you_account_settings) else stringResource(R.string.user_settings), { nav.back() }) }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            PrimaryTabRow(tab) { tabs.forEachIndexed { i, t -> Tab(i == tab, { tab = i }, text = { Text(t, maxLines = 1) }) } }
            when (tabs[tab]) {
                App.str(R.string.general) -> GeneralSettings(id)
                App.str(R.string.password) -> PasswordSettings(id, self)
                App.str(R.string.notifications) -> NotificationSettings(id)
                App.str(R.string.permissions) -> PermissionSettings(id, readOnly = self)
            }
        }
    }
}

@Composable
private fun SaveBar(state: FormState, busy: Boolean, onSave: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.End) {
        androidx.compose.material3.Button(onSave, enabled = state.dirty && !busy) {
            if (busy) CircularProgressIndicator(Modifier.width(18.dp), strokeWidth = 2.dp) else Text(stringResource(R.string.save_changes))
        }
    }
}

@Composable
private fun GeneralSettings(id: Int) {
    val s = App.session
    val act = rememberActions()
    val load = rememberLoad(id, "general") {
        Triple(
            App.api.obj("/user/$id/settings/main"),
            try { App.api.arr("/regions").objects() } catch (_: Exception) { emptyList() },
            try { App.api.arr("/languages").objects() } catch (_: Exception) { emptyList() },
        )
    }
    Loaded(load) { (main, regions, langs) ->
        val state = remember(main) { FormState(main) }
        val regionChoices = listOf("" to App.str(R.string.default_server_setting)) + regions.map { (it.str("iso_3166_1") ?: "") to (it.str("english_name") ?: "") }.sortedBy { it.second }
        val langChoices = listOf("" to App.str(R.string.default_server_setting)) + langs.map { (it.str("iso_639_1") ?: "") to (it.str("english_name") ?: "") }.sortedBy { it.second }
        val canQuota = s.can(Perm.MANAGE_USERS) && id != s.userId
        val hide = buildSet {
            addAll(listOf("globalMovieQuotaDays", "globalMovieQuotaLimit", "globalTvQuotaDays", "globalTvQuotaLimit", "locale"))
            if (!canQuota) addAll(listOf("movieQuotaLimit", "movieQuotaDays", "tvQuotaLimit", "tvQuotaDays"))
            if (s.mediaServer != MediaServer.Plex) addAll(listOf("watchlistSyncMovies", "watchlistSyncTv"))
        }
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Spacer(Modifier.padding(4.dp))
            JsonFields(
                state,
                FormSpec(
                    labels = mapOf(
                        "username" to App.str(R.string.display_name), "discordId" to App.str(R.string.discord_user_id),
                        "discoverRegion" to App.str(R.string.discover_region), "streamingRegion" to App.str(R.string.streaming_region),
                        "originalLanguage" to App.str(R.string.discover_language),
                        "movieQuotaLimit" to App.str(R.string.movie_request_limit), "movieQuotaDays" to App.str(R.string.movie_limit_period_days),
                        "tvQuotaLimit" to App.str(R.string.season_request_limit), "tvQuotaDays" to App.str(R.string.season_limit_period_days),
                        "watchlistSyncMovies" to App.str(R.string.auto_request_movies_on_plex), "watchlistSyncTv" to App.str(R.string.auto_request_series_on_plex),
                    ),
                    help = mapOf(
                        "movieQuotaLimit" to App.str(R.string.leave_empty_to_use_the_global_limit, main.int("globalMovieQuotaLimit"), main.int("globalMovieQuotaDays")),
                        "tvQuotaLimit" to App.str(R.string.leave_empty_to_use_the_global_limit, main.int("globalTvQuotaLimit"), main.int("globalTvQuotaDays")),
                        "discoverRegion" to App.str(R.string.filters_discover_content_by_regional),
                        "streamingRegion" to App.str(R.string.shows_streaming_sites_by_regional),
                    ),
                    hide = hide,
                    types = mapOf(
                        "movieQuotaLimit" to "int", "movieQuotaDays" to "int", "tvQuotaLimit" to "int", "tvQuotaDays" to "int",
                        "watchlistSyncMovies" to "bool", "watchlistSyncTv" to "bool", "username" to "string", "email" to "string", "discordId" to "string",
                    ),
                    choices = mapOf("discoverRegion" to regionChoices, "streamingRegion" to regionChoices, "originalLanguage" to langChoices),
                    order = listOf("username", "email", "discordId", "discoverRegion", "streamingRegion", "originalLanguage"),
                ),
            )
            SaveBar(state, act.busy) {
                act.run(App.str(R.string.saved), onOk = {
                    state.saved()
                    if (id == s.userId) try { s.refresh() } catch (_: Exception) {}
                }) { App.api.post("/user/$id/settings/main", state.obj) }
            }
            Spacer(Modifier.padding(40.dp))
        }
    }
}

@Composable
private fun PasswordSettings(id: Int, self: Boolean) {
    val act = rememberActions()
    val load = rememberLoad(id, "pw") { App.api.obj("/user/$id/settings/password").bool("hasPassword") }
    var current by remember { mutableStateOf("") }
    var new1 by remember { mutableStateOf("") }
    var new2 by remember { mutableStateOf("") }
    Loaded(load) { has ->
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                if (has) stringResource(R.string.change_the_password_used_for_signing) else stringResource(R.string.this_account_has_no_seerr_password),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (has && self) OutlinedTextField(current, { current = it }, label = { Text(stringResource(R.string.current_password)) }, singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = PasswordKeyboard, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(new1, { new1 = it }, label = { Text(stringResource(R.string.new_password_8_characters)) }, singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = PasswordKeyboard, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(new2, { new2 = it }, label = { Text(stringResource(R.string.repeat_new_password)) }, singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = PasswordKeyboard, modifier = Modifier.fillMaxWidth(),
                isError = new2.isNotEmpty() && new1 != new2)
            androidx.compose.material3.Button(
                enabled = new1.length >= 8 && new1 == new2 && !act.busy,
                onClick = {
                    act.run(App.str(R.string.password_saved), onOk = { current = ""; new1 = ""; new2 = "" }) {
                        App.api.post("/user/$id/settings/password", jsonOf("currentPassword" to current.ifEmpty { null }, "newPassword" to new1, "confirmPassword" to new2))
                    }
                },
            ) { Text(stringResource(R.string.save_password)) }
        }
    }
}

/** Password keyboard: no suggestions or learning of what's typed. */
private val PasswordKeyboard = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false)

val NotificationTypeList = listOf(
    2L to App.str(R.string.request_pending_approval), 128L to App.str(R.string.request_automatically_approved), 4L to App.str(R.string.request_approved),
    64L to App.str(R.string.request_declined), 8L to App.str(R.string.request_available), 16L to App.str(R.string.request_processing_failed),
    4096L to App.str(R.string.request_automatically_submitted), 256L to App.str(R.string.issue_reported), 512L to App.str(R.string.issue_comment),
    1024L to App.str(R.string.issue_resolved), 2048L to App.str(R.string.issue_reopened),
)

@Composable
private fun NotificationSettings(id: Int) {
    val act = rememberActions()
    val load = rememberLoad(id, "notif") { App.api.obj("/user/$id/settings/notifications") }
    Loaded(load) { n ->
        val state = remember(n) { FormState(n) }
        val agents = buildList {
            if (n.bool("emailEnabled")) add("email" to App.str(R.string.email))
            if (n.bool("discordEnabled")) add("discord" to App.str(R.string.discord))
            add("pushbullet" to App.str(R.string.pushbullet)); add("pushover" to App.str(R.string.pushover))
            if (n.bool("telegramEnabled")) add("telegram" to App.str(R.string.telegram))
            if (n.bool("webPushEnabled")) add("webpush" to App.str(R.string.web_push))
        }
        var agent by remember { mutableIntStateOf(0) }
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Text(
                stringResource(R.string.agents_your_server_has_turned_on_and),
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp),
            )
            JsonFields(
                state,
                FormSpec(
                    hide = setOf("emailEnabled", "discordEnabled", "telegramEnabled", "webPushEnabled", "notificationTypes", "telegramBotUsername", "discordEnabledTypes", "discordIds"),
                    labels = mapOf("pgpKey" to App.str(R.string.pgp_public_key_email), "telegramChatId" to App.str(R.string.telegram_chat_id), "telegramMessageThreadId" to App.str(R.string.telegram_thread_id),
                        "telegramSendSilently" to App.str(R.string.telegram_send_silently), "pushoverSound" to App.str(R.string.pushover_sound)),
                    multiline = setOf("pgpKey"),
                    types = mapOf("pgpKey" to "string", "pushbulletAccessToken" to "string", "pushoverApplicationToken" to "string", "pushoverUserKey" to "string",
                        "pushoverSound" to "string", "telegramChatId" to "string", "telegramMessageThreadId" to "string", "telegramSendSilently" to "bool"),
                ),
            )
            n.str("telegramBotUsername")?.let {
                Text(stringResource(R.string.start_a_chat_with_on_telegram_then, it), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 16.dp))
            }
            if (agents.isNotEmpty()) {
                SheetHeader(stringResource(R.string.notify_me_about))
                androidx.compose.material3.ScrollableTabRow(agent, edgePadding = 16.dp) {
                    agents.forEachIndexed { i, (_, l) -> Tab(i == agent, { agent = i }, text = { Text(l) }) }
                }
                state.version
                val key = agents[agent.coerceIn(0, agents.lastIndex)].first
                val current = (state.get("notificationTypes.$key") as? Number)?.toLong() ?: 0L
                NotificationTypeList.forEach { (bit, label) ->
                    Row(
                        Modifier.fillMaxWidth().clickable { state.set("notificationTypes.$key", current xor bit) }.padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(current and bit != 0L, { state.set("notificationTypes.$key", if (it) current or bit else current and bit.inv()) })
                        Text(label)
                    }
                }
            }
            SaveBar(state, act.busy) {
                act.run(App.str(R.string.saved), onOk = { state.saved() }) { App.api.post("/user/$id/settings/notifications", state.obj) }
            }
            Spacer(Modifier.padding(40.dp))
        }
    }
}

@Composable
private fun PermissionSettings(id: Int, readOnly: Boolean) {
    val act = rememberActions()
    val load = rememberLoad(id, "perm") { App.api.obj("/user/$id/settings/permissions").long("permissions") }
    Loaded(load) { initial ->
        var perms by remember(initial) { mutableLongStateOf(initial) }
        Column(Modifier.verticalScroll(rememberScrollState()).padding(8.dp)) {
            if (readOnly) Text(stringResource(R.string.only_another_admin_can_change_your), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(8.dp))
            PermissionEditor(perms, enabled = !readOnly) { perms = it }
            if (!readOnly) {
                Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.End) {
                    androidx.compose.material3.Button(
                        enabled = perms != initial && !act.busy,
                        onClick = { act.run(App.str(R.string.permissions_saved), onOk = { load.reload(silent = true) }) { App.api.post("/user/$id/settings/permissions", jsonOf("permissions" to perms)) } },
                    ) { Text(stringResource(R.string.save_permissions)) }
                }
            }
            Spacer(Modifier.padding(40.dp))
        }
    }
}

