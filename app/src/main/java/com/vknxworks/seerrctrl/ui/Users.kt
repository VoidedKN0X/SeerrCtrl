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

// ---------------- profile ----------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(id: Int) {
    val nav = LocalNav.current
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
            BackBar(if (self) "Your profile" else load.value?.first?.str("displayName") ?: "User", { nav.popBackStack() }) {
                IconButton({ nav.navigate("watchlist/$id") }) { Icon(Icons.Outlined.Bookmark, "Watchlist") }
                if (self || s.can(Perm.MANAGE_USERS)) IconButton({ nav.navigate("user/$id/settings") }) { Icon(Icons.Outlined.Settings, "Settings") }
                if (canManage && id != 1) IconButton({ confirmDelete = true }) { Icon(Icons.Outlined.Delete, "Delete user") }
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
                            listOfNotNull(UserTypes.name(u.int("userType")), roleOf(u)).joinToString(" · "),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        u.str("email")?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                        prettyDate(u.str("createdAt"))?.let { Text("Joined $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
                Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("Requests", "${u.int("requestCount")}", Modifier.weight(1f))
                    q.obj("movie")?.takeIf { it.int("limit") > 0 }?.let { m ->
                        StatCard("Movie quota", "${m.int("used")}/${m.int("limit")}", Modifier.weight(1f), "per ${m.int("days")} days")
                    }
                    q.obj("tv")?.takeIf { it.int("limit") > 0 }?.let { t ->
                        StatCard("Season quota", "${t.int("used")}/${t.int("limit")}", Modifier.weight(1f), "per ${t.int("days")} days")
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
            title = { Text("Delete this user?") },
            text = { Text("Their requests are removed too. This can't be undone.") },
            confirmButton = {
                TextButton({
                    confirmDelete = false
                    act.run("User deleted", onOk = { nav.popBackStack() }) { App.api.delete("/user/$id") }
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton({ confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

fun roleOf(u: JSONObject?): String? = when {
    u.int("id") == 1 -> "Owner"
    Perm.has(u.long("permissions"), Perm.ADMIN) -> "Admin"
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
    Scaffold(topBar = { BackBar(if (self) "Your watchlist" else "Watchlist", { nav.popBackStack() }) }) { pad ->
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
    Scaffold(topBar = { BackBar("Blocklist", { nav.popBackStack() }) }) { pad ->
        Column(Modifier.padding(pad)) {
            OutlinedTextField(
                search, { search = it }, placeholder = { Text("Search blocklist") }, leadingIcon = { Icon(Icons.Outlined.Search, null) },
                singleLine = true, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )
            Loaded(load) { list ->
                LazyColumn(contentPadding = ListBottom) {
                    items(list, key = { it.int("id") }) { b ->
                        ListItem(
                            headlineContent = { Text(b.str("title") ?: "TMDB ${b.int("tmdbId")}") },
                            supportingContent = {
                                Text(listOfNotNull(
                                    if (b.str("mediaType") == "tv") "Series" else "Movie",
                                    b.obj("user").str("displayName")?.let { "by $it" },
                                    prettyDate(b.str("createdAt")),
                                    b.str("blocklistedTags")?.let { "tags $it" },
                                ).joinToString(" · "))
                            },
                            trailingContent = if (App.session.can(Perm.MANAGE_BLOCKLIST)) ({
                                IconButton({
                                    act.run("Removed from blocklist", onOk = { reload++ }) {
                                        App.api.delete("/blocklist/${b.int("tmdbId")}", query = mapOf("mediaType" to b.str("mediaType")))
                                    }
                                }) { Icon(Icons.Outlined.Close, "Remove") }
                            }) else null,
                            modifier = Modifier.clickable { nav.openMedia(b.str("mediaType"), b.int("tmdbId")) },
                        )
                    }
                    if (list.isEmpty()) item { EmptyBox(Icons.Outlined.Shield, "Nothing is blocklisted") }
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
        } catch (e: Exception) { act.say(e.message ?: "Couldn't load users") }
        loading = false
    }
    LaunchedEffect(query, sort, reload) { more() }
    val nearEnd by remember { derivedStateOf { (list.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) >= list.layoutInfo.totalItemsCount - 5 } }
    LaunchedEffect(nearEnd) { if (nearEnd && users.isNotEmpty()) more() }

    Scaffold(
        topBar = {
            if (selected.isNotEmpty()) {
                BackBar("${selected.size} selected", { selected.clear() }) {
                    TextButton({ dialog = "perms" }) { Text("Edit permissions") }
                }
            } else {
                BackBar("Users", { nav.popBackStack() }, subtitle = if (total >= 0) "$total users" else null) {
                    Box {
                        TextButton({ sortMenu = true }) { Text("Sort") }
                        DropdownMenu(sortMenu, { sortMenu = false }) {
                            listOf("displayname" to "Name", "created" to "Joined", "requests" to "Requests", "usertype" to "Type", "role" to "Role").forEach { (k, l) ->
                                DropdownMenuItem({ Text(l) }, { sort = k; sortMenu = false })
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            Box {
                FloatingActionButton({ menu = true }) { Icon(Icons.Outlined.PersonAdd, "Add users") }
                DropdownMenu(menu, { menu = false }) {
                    DropdownMenuItem({ Text("Create local user") }, { menu = false; dialog = "create" }, leadingIcon = { Icon(Icons.Outlined.PersonAdd, null) })
                    val ms = App.session.mediaServer
                    if (ms == MediaServer.Jellyfin || ms == MediaServer.Emby) {
                        DropdownMenuItem({ Text("Import ${ms.label} users") }, { menu = false; dialog = "jellyfin" }, leadingIcon = { Icon(Icons.Outlined.CloudDownload, null) })
                    }
                    if (ms == MediaServer.Plex) {
                        DropdownMenuItem({ Text("Import Plex users") }, { menu = false; dialog = "plex" }, leadingIcon = { Icon(Icons.Outlined.CloudDownload, null) })
                    }
                }
            }
        },
    ) { pad ->
        Column(Modifier.padding(pad)) {
            OutlinedTextField(
                query, { query = it }, placeholder = { Text("Search users") }, leadingIcon = { Icon(Icons.Outlined.Search, null) },
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
                        headlineContent = { Text(u.str("displayName") ?: u.str("email") ?: "User $id") },
                        supportingContent = {
                            Text(listOfNotNull(UserTypes.name(u.int("userType")), roleOf(u), "${u.int("requestCount")} requests").joinToString(" · "))
                        },
                        colors = ListItemDefaults.colors(containerColor = if (sel) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent),
                        modifier = Modifier.combinedClickable(
                            onClick = {
                                if (selected.isNotEmpty()) { if (sel) selected.remove(id) else selected.add(id) }
                                else nav.navigate("user/$id")
                            },
                            onLongClick = { if (id != 1 && id != App.session.userId) selected.add(id) },
                        ),
                    )
                }
                if (loading) item { Box(Modifier.fillMaxWidth().padding(24.dp), Alignment.Center) { CircularProgressIndicator() } }
                if (!loading && users.isEmpty()) item { EmptyBox(Icons.Outlined.Group, "No users found") }
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
                title = { Text("Permissions for ${selected.size} users") },
                text = { Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState())) { PermissionEditor(perms) { perms = it } } },
                confirmButton = {
                    TextButton({
                        val ids = selected.toList()
                        dialog = null
                        act.run("Permissions updated", onOk = { selected.clear(); reload++ }) {
                            App.api.put("/user", JSONObject().put("ids", JSONArray(ids)).put("permissions", perms))
                        }
                    }) { Text("Save") }
                },
                dismissButton = { TextButton({ dialog = null }) { Text("Cancel") } },
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
        title = { Text("Create local user") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Display name") }, singleLine = true)
                OutlinedTextField(email, { email = it }, label = { Text("Email") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
                OutlinedTextField(
                    password, { password = it }, label = { Text(if (emailOn) "Password (blank = email them one)" else "Password") },
                    singleLine = true, visualTransformation = PasswordVisualTransformation(),
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
            }, enabled = email.isNotBlank() && (emailOn || password.length >= 8)) { Text("Create") }
        },
        dismissButton = { TextButton(onDismiss) { Text("Cancel") } },
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
        title = { Text("Import ${if (plex) "Plex" else App.session.mediaServer.label} users") },
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
                        if (list.isEmpty()) item { Text("No users to import") }
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
            }, enabled = picked.isNotEmpty()) { Text("Import ${picked.size}") }
        },
        dismissButton = { TextButton(onDismiss) { Text("Cancel") } },
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
    val nav = LocalNav.current
    val s = App.session
    val self = id == s.userId
    val tabs = buildList {
        add("General"); add("Password"); add("Notifications")
        if (!self && s.can(Perm.MANAGE_USERS) && id != 1 || self) add("Permissions")
    }
    var tab by remember { mutableIntStateOf(0) }
    Scaffold(topBar = { BackBar(if (self) "Account settings" else "User settings", { nav.popBackStack() }) }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            PrimaryTabRow(tab) { tabs.forEachIndexed { i, t -> Tab(i == tab, { tab = i }, text = { Text(t, maxLines = 1) }) } }
            when (tabs[tab]) {
                "General" -> GeneralSettings(id)
                "Password" -> PasswordSettings(id, self)
                "Notifications" -> NotificationSettings(id)
                "Permissions" -> PermissionSettings(id, readOnly = self)
            }
        }
    }
}

@Composable
private fun SaveBar(state: FormState, busy: Boolean, onSave: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.End) {
        androidx.compose.material3.Button(onSave, enabled = state.dirty && !busy) {
            if (busy) CircularProgressIndicator(Modifier.width(18.dp), strokeWidth = 2.dp) else Text("Save changes")
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
        val regionChoices = listOf("" to "Default (server setting)") + regions.map { (it.str("iso_3166_1") ?: "") to (it.str("english_name") ?: "") }.sortedBy { it.second }
        val langChoices = listOf("" to "Default (server setting)") + langs.map { (it.str("iso_639_1") ?: "") to (it.str("english_name") ?: "") }.sortedBy { it.second }
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
                        "username" to "Display name", "discordId" to "Discord user ID",
                        "discoverRegion" to "Discover region", "streamingRegion" to "Streaming region",
                        "originalLanguage" to "Discover language",
                        "movieQuotaLimit" to "Movie request limit", "movieQuotaDays" to "Movie limit period (days)",
                        "tvQuotaLimit" to "Season request limit", "tvQuotaDays" to "Season limit period (days)",
                        "watchlistSyncMovies" to "Auto-request movies on Plex watchlist", "watchlistSyncTv" to "Auto-request series on Plex watchlist",
                    ),
                    help = mapOf(
                        "movieQuotaLimit" to "Leave empty to use the global limit (${main.int("globalMovieQuotaLimit")} per ${main.int("globalMovieQuotaDays")} days)",
                        "tvQuotaLimit" to "Leave empty to use the global limit (${main.int("globalTvQuotaLimit")} per ${main.int("globalTvQuotaDays")} days)",
                        "discoverRegion" to "Filters discover content by regional availability",
                        "streamingRegion" to "Shows streaming sites by regional availability",
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
                act.run("Saved", onOk = {
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
                if (has) "Change the password used for signing in with email." else "This account has no Seerr password yet. Set one to allow email sign-in.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (has && self) OutlinedTextField(current, { current = it }, label = { Text("Current password") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(new1, { new1 = it }, label = { Text("New password (8+ characters)") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(new2, { new2 = it }, label = { Text("Repeat new password") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(),
                isError = new2.isNotEmpty() && new1 != new2)
            androidx.compose.material3.Button(
                enabled = new1.length >= 8 && new1 == new2 && !act.busy,
                onClick = {
                    act.run("Password saved", onOk = { current = ""; new1 = ""; new2 = "" }) {
                        App.api.post("/user/$id/settings/password", jsonOf("currentPassword" to current.ifEmpty { null }, "newPassword" to new1, "confirmPassword" to new2))
                    }
                },
            ) { Text("Save password") }
        }
    }
}

val NotificationTypeList = listOf(
    2L to "Request pending approval", 128L to "Request automatically approved", 4L to "Request approved",
    64L to "Request declined", 8L to "Request available", 16L to "Request processing failed",
    4096L to "Request automatically submitted", 256L to "Issue reported", 512L to "Issue comment",
    1024L to "Issue resolved", 2048L to "Issue reopened",
)

@Composable
private fun NotificationSettings(id: Int) {
    val act = rememberActions()
    val load = rememberLoad(id, "notif") { App.api.obj("/user/$id/settings/notifications") }
    Loaded(load) { n ->
        val state = remember(n) { FormState(n) }
        val agents = buildList {
            if (n.bool("emailEnabled")) add("email" to "Email")
            if (n.bool("discordEnabled")) add("discord" to "Discord")
            add("pushbullet" to "Pushbullet"); add("pushover" to "Pushover")
            if (n.bool("telegramEnabled")) add("telegram" to "Telegram")
            if (n.bool("webPushEnabled")) add("webpush" to "Web push")
        }
        var agent by remember { mutableIntStateOf(0) }
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Text(
                "Agents your server has turned on, and which events reach you through each.",
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp),
            )
            JsonFields(
                state,
                FormSpec(
                    hide = setOf("emailEnabled", "discordEnabled", "telegramEnabled", "webPushEnabled", "notificationTypes", "telegramBotUsername", "discordEnabledTypes", "discordIds"),
                    labels = mapOf("pgpKey" to "PGP public key (email)", "telegramChatId" to "Telegram chat ID", "telegramMessageThreadId" to "Telegram thread ID",
                        "telegramSendSilently" to "Telegram: send silently", "pushoverSound" to "Pushover sound"),
                    multiline = setOf("pgpKey"),
                    types = mapOf("pgpKey" to "string", "pushbulletAccessToken" to "string", "pushoverApplicationToken" to "string", "pushoverUserKey" to "string",
                        "pushoverSound" to "string", "telegramChatId" to "string", "telegramMessageThreadId" to "string", "telegramSendSilently" to "bool"),
                ),
            )
            n.str("telegramBotUsername")?.let {
                Text("Start a chat with @$it on Telegram, then send /my_id to get your chat ID.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 16.dp))
            }
            if (agents.isNotEmpty()) {
                SheetHeader("Notify me about")
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
                act.run("Saved", onOk = { state.saved() }) { App.api.post("/user/$id/settings/notifications", state.obj) }
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
            if (readOnly) Text("Only another admin can change your permissions.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(8.dp))
            PermissionEditor(perms, enabled = !readOnly) { perms = it }
            if (!readOnly) {
                Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.End) {
                    androidx.compose.material3.Button(
                        enabled = perms != initial && !act.busy,
                        onClick = { act.run("Permissions saved", onOk = { load.reload(silent = true) }) { App.api.post("/user/$id/settings/permissions", jsonOf("permissions" to perms)) } },
                    ) { Text("Save permissions") }
                }
            }
            Spacer(Modifier.padding(40.dp))
        }
    }
}

