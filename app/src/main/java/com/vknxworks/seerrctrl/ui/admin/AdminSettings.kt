package com.vknxworks.seerrctrl.ui.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lan
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Rule
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vknxworks.seerrctrl.App
import com.vknxworks.seerrctrl.data.MediaServer
import com.vknxworks.seerrctrl.data.bool
import com.vknxworks.seerrctrl.data.humanBytes
import com.vknxworks.seerrctrl.data.int
import com.vknxworks.seerrctrl.data.intOrNull
import com.vknxworks.seerrctrl.data.jsonOf
import com.vknxworks.seerrctrl.data.long
import com.vknxworks.seerrctrl.data.obj
import com.vknxworks.seerrctrl.data.objects
import com.vknxworks.seerrctrl.data.objs
import com.vknxworks.seerrctrl.data.prettyDateTime
import com.vknxworks.seerrctrl.data.str
import com.vknxworks.seerrctrl.ui.BackBar
import com.vknxworks.seerrctrl.ui.EmptyBox
import com.vknxworks.seerrctrl.ui.FormSpec
import com.vknxworks.seerrctrl.ui.FormState
import com.vknxworks.seerrctrl.ui.ItemDivider
import com.vknxworks.seerrctrl.ui.JsonFields
import com.vknxworks.seerrctrl.ui.ListBottom
import com.vknxworks.seerrctrl.ui.Loaded
import com.vknxworks.seerrctrl.ui.LocalNav
import com.vknxworks.seerrctrl.ui.NavItem
import com.vknxworks.seerrctrl.ui.NotificationTypeList
import com.vknxworks.seerrctrl.ui.PermissionEditor
import com.vknxworks.seerrctrl.ui.Pill
import com.vknxworks.seerrctrl.ui.SettingsSection
import com.vknxworks.seerrctrl.ui.SheetHeader
import com.vknxworks.seerrctrl.ui.Status
import com.vknxworks.seerrctrl.ui.rememberActions
import com.vknxworks.seerrctrl.ui.rememberLoad
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject

/** Every server settings page from the web UI, as one list. */
@Composable
fun AdminSettingsScreen() {
    val nav = LocalNav.current
    val ms = App.session.mediaServer
    Scaffold(topBar = { BackBar("Server settings", { nav.popBackStack() }, subtitle = App.session.appTitle) }) { pad ->
        LazyColumn(Modifier.padding(pad), contentPadding = ListBottom) {
            item {
                SettingsSection("Server") {
                    NavItem(Icons.Outlined.Settings, "General", "Title, URL, image cache, discover region, display options") { nav.navigate("admin/general") }
                    ItemDivider()
                    NavItem(Icons.Outlined.Group, "Users", "Sign-in methods, default permissions and quotas") { nav.navigate("admin/users") }
                    ItemDivider()
                    NavItem(Icons.Outlined.Lan, "Network", "Proxy, CSRF, DNS cache, timeouts") { nav.navigate("admin/network") }
                    ItemDivider()
                    NavItem(Icons.Outlined.Info, "About", "Version, totals, paths") { nav.navigate("admin/about") }
                }
            }
            item {
                SettingsSection("Media") {
                    NavItem(Icons.Outlined.Movie, ms.label.takeIf { ms != MediaServer.None } ?: "Media server", "Connection, libraries and scans") { nav.navigate("admin/mediaserver") }
                    ItemDivider()
                    NavItem(Icons.Outlined.Dns, "Services", "Radarr and Sonarr servers") { nav.navigate("admin/services") }
                    ItemDivider()
                    NavItem(Icons.Outlined.Tune, "Metadata", "Metadata providers for movies, series and anime") { nav.navigate("admin/metadata") }
                    if (ms == MediaServer.Plex) {
                        ItemDivider()
                        NavItem(Icons.Outlined.Storage, "Tautulli", "Watch history from Tautulli") { nav.navigate("admin/tautulli") }
                    }
                    ItemDivider()
                    NavItem(Icons.Outlined.Rule, "Override rules", "Route requests to specific servers and profiles") { nav.navigate("admin/overrides") }
                }
            }
            item {
                SettingsSection("Behaviour") {
                    NavItem(Icons.Outlined.Notifications, "Notifications", "Email, Discord, Telegram, webhooks and more") { nav.navigate("admin/notifications") }
                    ItemDivider()
                    NavItem(Icons.Outlined.Explore, "Discover", "Choose and order the discover sliders") { nav.navigate("admin/discover") }
                    ItemDivider()
                    NavItem(Icons.Outlined.Schedule, "Jobs & cache", "Run scans now, change schedules, flush caches") { nav.navigate("admin/jobs") }
                    ItemDivider()
                    NavItem(Icons.AutoMirrored.Outlined.Article, "Logs", "Server log, filterable by level") { nav.navigate("admin/logs") }
                }
            }
        }
    }
}

@Composable
fun AdminSectionScreen(section: String) {
    when {
        section == "general" -> SettingsForm("General", "/settings/main", generalSpec, hideKeys = usersKeys + setOf("defaultPermissions", "defaultQuotas"), regen = true)
        section == "users" -> UsersSettings()
        section == "network" -> SettingsForm("Network", "/settings/network", networkSpec)
        section == "mediaserver" -> MediaServerSettings()
        section == "metadata" -> SettingsForm("Metadata", "/settings/metadatas", FormSpec(), method = "PUT")
        section == "tautulli" -> SettingsForm("Tautulli", "/settings/tautulli", FormSpec(labels = mapOf("urlBase" to "URL base", "externalUrl" to "External URL")))
        section == "services" -> ServicesScreen()
        section.startsWith("service:") -> {
            val (_, kind, id) = section.split(':').let { Triple(it[0], it.getOrElse(1) { "radarr" }, it.getOrElse(2) { "new" }) }
            ServiceEditor(kind, id.toIntOrNull())
        }
        section == "notifications" -> NotificationAgentsScreen()
        section.startsWith("notif:") -> NotificationAgentScreen(section.removePrefix("notif:"))
        section == "discover" -> DiscoverSlidersScreen()
        section == "jobs" -> JobsScreen()
        section == "logs" -> LogsScreen()
        section == "about" -> AboutScreen()
        section == "overrides" -> OverrideRulesScreen()
    }
}

// ---------------- generic settings form ----------------

private val usersKeys = setOf("localLogin", "mediaServerLogin", "newPlexLogin")

private val generalSpec = FormSpec(
    labels = mapOf(
        "applicationTitle" to "Application title", "applicationUrl" to "Application URL", "cacheImages" to "Cache images locally",
        "hideAvailable" to "Hide available media", "hideBlocklisted" to "Hide blocklisted items", "hideRequested" to "Hide requested media",
        "discoverRegion" to "Discover region (ISO code)", "streamingRegion" to "Streaming region (ISO code)", "originalLanguage" to "Discover language (ISO codes)",
        "partialRequestsEnabled" to "Allow partial series requests", "enableSpecialEpisodes" to "Allow special episode requests",
        "youtubeUrl" to "YouTube URL", "versionCheck" to "Check for updates", "blocklistedTags" to "Blocklisted tags",
        "blocklistedTagsLimit" to "Blocklisted tags limit", "blocklistRegion" to "Blocklist region", "blocklistLanguage" to "Blocklist language",
    ),
    help = mapOf(
        "apiKey" to "Read-only here. Use Regenerate to make a new one.",
        "cacheImages" to "Proxy and cache TMDB images through Seerr",
        "applicationUrl" to "Used in notifications and links",
    ),
    readOnly = setOf("apiKey", "mediaServerType"),
    hide = setOf("mediaServerType", "locale"),
    order = listOf("applicationTitle", "applicationUrl", "apiKey"),
)

private val networkSpec = FormSpec(
    labels = mapOf(
        "csrfProtection" to "CSRF protection", "forceIpv4First" to "Force IPv4 resolution first", "trustProxy" to "Trust proxy (behind a reverse proxy)",
        "apiRequestTimeout" to "API request timeout (ms)", "bypassFilter" to "Bypass proxy for addresses", "bypassLocalAddresses" to "Bypass proxy for local addresses",
        "forceMinTtl" to "Minimum TTL (s)", "forceMaxTtl" to "Maximum TTL (s, -1 = off)",
    ),
    help = mapOf("csrfProtection" to "Only turn on if you know what it does; it can lock you out of the web UI over plain HTTP."),
)

@Composable
private fun SettingsForm(
    title: String,
    path: String,
    spec: FormSpec,
    hideKeys: Set<String> = emptySet(),
    method: String = "POST",
    regen: Boolean = false,
    extra: @Composable (FormState) -> Unit = {},
) {
    val nav = LocalNav.current
    val act = rememberActions()
    val load = rememberLoad(path) { App.api.obj(path) }
    Scaffold(topBar = { BackBar(title, { nav.popBackStack() }) }) { pad ->
        Loaded(load, Modifier.padding(pad)) { data ->
            val state = remember(data) { FormState(data) }
            Column(Modifier.padding(pad).verticalScroll(rememberScrollState())) {
                Spacer(Modifier.height(8.dp))
                JsonFields(state, spec.copy(hide = spec.hide + hideKeys))
                extra(state)
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                    if (regen) {
                        OutlinedButton({
                            act.run("New API key generated", onOk = { load.reload(silent = true) }) { App.api.post("/settings/main/regenerate") }
                        }) { Text("Regenerate API key") }
                    }
                    Button(
                        enabled = state.dirty && !act.busy,
                        onClick = {
                            act.run("Saved", onOk = { state.saved(); try { App.session.refresh() } catch (_: Exception) {} }) {
                                if (method == "PUT") App.api.put(path, state.obj) else App.api.post(path, state.obj)
                            }
                        },
                    ) { Text("Save") }
                }
                Spacer(Modifier.height(48.dp))
            }
        }
    }
}

// ---------------- users ----------------

@Composable
private fun UsersSettings() {
    val nav = LocalNav.current
    val act = rememberActions()
    val load = rememberLoad("main-users") { App.api.obj("/settings/main") }
    Scaffold(topBar = { BackBar("User settings", { nav.popBackStack() }) }) { pad ->
        Loaded(load, Modifier.padding(pad)) { main ->
            val subset = remember(main) {
                val q = main.obj("defaultQuotas") ?: JSONObject()
                for (k in listOf("movie", "tv")) {
                    val o = q.optJSONObject(k) ?: JSONObject().also { q.put(k, it) }
                    if (!o.has("quotaLimit")) o.put("quotaLimit", JSONObject.NULL)
                    if (!o.has("quotaDays")) o.put("quotaDays", JSONObject.NULL)
                }
                JSONObject().apply {
                    usersKeys.forEach { if (main.has(it)) put(it, main.opt(it)) }
                    put("defaultQuotas", q)
                }
            }
            val state = remember(subset) { FormState(subset) }
            var perms by remember(main) { mutableStateOf(main.long("defaultPermissions")) }
            Column(Modifier.padding(pad).verticalScroll(rememberScrollState())) {
                JsonFields(
                    state,
                    FormSpec(
                        labels = mapOf(
                            "localLogin" to "Allow email & password sign-in", "mediaServerLogin" to "Allow media server sign-in",
                            "newPlexLogin" to "Allow new Plex users to sign in", "defaultQuotas" to "Default request limits",
                            "movie.quotaLimit" to "Movie requests", "movie.quotaDays" to "…per days", "quotaLimit" to "Request limit", "quotaDays" to "Period (days)",
                        ),
                        types = mapOf("quotaLimit" to "int", "quotaDays" to "int"),
                        hide = if (App.session.mediaServer != MediaServer.Plex) setOf("newPlexLogin") else emptySet(),
                    ),
                )
                SheetHeader("Default permissions for new users")
                Box(Modifier.padding(horizontal = 8.dp)) { PermissionEditor(perms) { perms = it } }
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.End) {
                    Button(
                        enabled = (state.dirty || perms != main.long("defaultPermissions")) && !act.busy,
                        onClick = {
                            act.run("Saved", onOk = { state.saved(); load.reload(silent = true) }) {
                                val body = JSONObject(main.toString())
                                state.obj.keys().forEach { body.put(it, state.obj.opt(it)) }
                                body.put("defaultPermissions", perms)
                                App.api.post("/settings/main", body)
                            }
                        },
                    ) { Text("Save") }
                }
                Spacer(Modifier.height(48.dp))
            }
        }
    }
}

// ---------------- media server ----------------

@Composable
private fun MediaServerSettings() {
    val nav = LocalNav.current
    val act = rememberActions()
    val ms = App.session.mediaServer
    val key = if (ms == MediaServer.Plex) "plex" else "jellyfin"
    val load = rememberLoad("ms") { App.api.obj("/settings/$key") }
    var libs by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var scan by remember { mutableStateOf<JSONObject?>(null) }
    var poll by remember { mutableIntStateOf(0) }

    LaunchedEffect(poll) {
        try { libs = App.api.arr("/settings/$key/library").objects() } catch (_: Exception) {}
        while (true) {
            try { scan = App.api.obj("/settings/$key/sync") } catch (_: Exception) {}
            if (scan?.bool("running") != true) break
            delay(2000)
        }
    }

    Scaffold(topBar = { BackBar(ms.label, { nav.popBackStack() }) }) { pad ->
        Loaded(load, Modifier.padding(pad)) { data ->
            val state = remember(data) { FormState(data) }
            Column(Modifier.padding(pad).verticalScroll(rememberScrollState())) {
                SheetHeader("Connection")
                JsonFields(
                    state,
                    FormSpec(
                        hide = setOf("libraries", "machineId", "apiKey", "serverId"),
                        labels = mapOf("ip" to "Hostname or IP", "useSsl" to "Use SSL", "urlBase" to "URL base", "externalHostname" to "External URL",
                            "jellyfinForgotPasswordUrl" to "Forgot password URL", "webAppUrl" to "Web app URL"),
                        readOnly = if (ms == MediaServer.Plex) emptySet() else setOf("name"),
                    ),
                )
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.End) {
                    Button(enabled = state.dirty && !act.busy, onClick = {
                        act.run("Saved", onOk = { state.saved() }) { App.api.post("/settings/$key", state.obj) }
                    }) { Text("Save connection") }
                }

                SheetHeader("Libraries")
                Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton({
                        act.run("Libraries synced", onOk = { poll++ }) { App.api.post("/settings/$key/library/sync") }
                    }) { Icon(Icons.Outlined.Sync, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Sync libraries") }
                }
                libs.forEach { lib ->
                    ListItem(
                        headlineContent = { Text(lib.str("name") ?: "") },
                        supportingContent = { Text(lib.str("type")?.replaceFirstChar { it.uppercase() } ?: "") },
                        trailingContent = {
                            Switch(lib.bool("enabled"), { on ->
                                act.run(onOk = { poll++ }) { App.api.put("/settings/$key/library/${lib.str("id")}", jsonOf("enabled" to on)) }
                            })
                        },
                    )
                }

                SheetHeader("Full library scan")
                val sc = scan
                Card(Modifier.padding(16.dp).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (sc?.bool("running") == true) {
                            val total = sc.int("total")
                            val prog = sc.int("progress")
                            Text("Scanning ${sc.obj("currentLibrary").str("name") ?: ""}… $prog of $total")
                            LinearProgressIndicator({ if (total > 0) prog.toFloat() / total else 0f }, Modifier.fillMaxWidth())
                            OutlinedButton({ act.run("Scan cancelled", onOk = { poll++ }) { App.api.post("/settings/$key/sync", jsonOf("cancel" to true)) } }) {
                                Icon(Icons.Outlined.Stop, null); Spacer(Modifier.width(6.dp)); Text("Cancel scan")
                            }
                        } else {
                            Text("Checks every enabled library for new and removed items. Recently added items are also picked up by a scheduled job.", style = MaterialTheme.typography.bodySmall)
                            FilledTonalButton({ act.run("Scan started", onOk = { poll++ }) { App.api.post("/settings/$key/sync", jsonOf("start" to true)) } }) {
                                Icon(Icons.Outlined.PlayArrow, null); Spacer(Modifier.width(6.dp)); Text("Start full scan")
                            }
                        }
                    }
                }
                Spacer(Modifier.height(48.dp))
            }
        }
    }
}

// ---------------- services ----------------

@Composable
private fun ServicesScreen() {
    val nav = LocalNav.current
    val load = rememberLoad("services") { App.api.arr("/settings/radarr").objects() to App.api.arr("/settings/sonarr").objects() }
    Scaffold(topBar = { BackBar("Services", { nav.popBackStack() }) { IconButton({ load.reload(silent = true) }) { Icon(Icons.Outlined.Sync, "Refresh") } } }) { pad ->
        Loaded(load, Modifier.padding(pad)) { (radarr, sonarr) ->
            LazyColumn(Modifier.padding(pad), contentPadding = ListBottom) {
                for ((kind, list) in listOf("radarr" to radarr, "sonarr" to sonarr)) {
                    item {
                        SettingsSection(if (kind == "radarr") "Radarr" else "Sonarr") {
                            list.forEachIndexed { i, srv ->
                                if (i > 0) ItemDivider()
                                NavItem(
                                    Icons.Outlined.Dns, srv.str("name") ?: "Server",
                                    listOfNotNull(
                                        (if (srv.bool("useSsl")) "https://" else "http://") + srv.str("hostname") + ":" + srv.int("port") + (srv.str("baseUrl") ?: ""),
                                        if (srv.bool("is4k")) "4K" else null,
                                        if (srv.bool("isDefault")) "Default" else null,
                                    ).joinToString(" · "),
                                ) { nav.navigate("admin/service:$kind:${srv.int("id")}") }
                            }
                            if (list.isNotEmpty()) ItemDivider()
                            NavItem(Icons.Outlined.Add, "Add ${if (kind == "radarr") "Radarr" else "Sonarr"} server") { nav.navigate("admin/service:$kind:new") }
                        }
                    }
                }
            }
        }
    }
}

private fun newService(kind: String) = JSONObject().apply {
    put("name", ""); put("hostname", ""); put("port", if (kind == "radarr") 7878 else 8989); put("apiKey", "")
    put("useSsl", false); put("baseUrl", ""); put("activeProfileId", JSONObject.NULL); put("activeProfileName", "")
    put("activeDirectory", ""); put("tags", JSONArray()); put("is4k", false); put("isDefault", false)
    put("externalUrl", ""); put("syncEnabled", false); put("preventSearch", false); put("tagRequests", false)
    if (kind == "radarr") put("minimumAvailability", "released")
    else {
        put("seriesType", "standard"); put("animeSeriesType", "anime"); put("activeAnimeProfileId", JSONObject.NULL)
        put("activeAnimeProfileName", ""); put("activeAnimeDirectory", ""); put("activeLanguageProfileId", JSONObject.NULL)
        put("activeAnimeLanguageProfileId", JSONObject.NULL); put("animeTags", JSONArray()); put("enableSeasonFolders", true)
    }
}

@Composable
private fun ServiceEditor(kind: String, id: Int?) {
    val nav = LocalNav.current
    val act = rememberActions()
    val label = if (kind == "radarr") "Radarr" else "Sonarr"
    val load = rememberLoad(kind, id) {
        if (id == null) newService(kind) else App.api.arr("/settings/$kind").objects().firstOrNull { it.int("id") == id } ?: error("Server not found")
    }
    var test by remember { mutableStateOf<JSONObject?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    Scaffold(topBar = {
        BackBar(if (id == null) "Add $label server" else "Edit $label server", { nav.popBackStack() }) {
            if (id != null) IconButton({ confirmDelete = true }) { Icon(Icons.Outlined.Delete, "Delete") }
        }
    }) { pad ->
        Loaded(load, Modifier.padding(pad)) { data ->
            val state = remember(data) { FormState(data) }
            val t = test
            val profiles = t.objs("profiles").map { it.int("id").toString() to (it.str("name") ?: "") }
            val folders = t.objs("rootFolders").map { (it.str("path") ?: "") to (it.str("path") ?: "") }
            val langProfiles = t.objs("languageProfiles").map { it.int("id").toString() to (it.str("name") ?: "") }
            val choices = buildMap<String, List<Pair<String, String>>> {
                if (profiles.isNotEmpty()) { put("activeProfileId", profiles); put("activeAnimeProfileId", profiles) }
                if (folders.isNotEmpty()) { put("activeDirectory", folders); put("activeAnimeDirectory", folders) }
                if (langProfiles.isNotEmpty()) { put("activeLanguageProfileId", langProfiles); put("activeAnimeLanguageProfileId", langProfiles) }
                put("minimumAvailability", listOf("announced" to "Announced", "inCinemas" to "In cinemas", "released" to "Released"))
                put("seriesType", listOf("standard" to "Standard", "daily" to "Daily", "anime" to "Anime"))
                put("animeSeriesType", listOf("standard" to "Standard", "daily" to "Daily", "anime" to "Anime"))
            }
            // Keep the profile names in step with the picked ids, as the web UI does.
            LaunchedEffect(state.version, t) {
                if (t == null) return@LaunchedEffect
                for ((idKey, nameKey) in listOf("activeProfileId" to "activeProfileName", "activeAnimeProfileId" to "activeAnimeProfileName")) {
                    val pid = (state.get(idKey) as? Number)?.toInt() ?: continue
                    val name = profiles.firstOrNull { it.first == pid.toString() }?.second ?: continue
                    if (state.get(nameKey) != name) state.set(nameKey, name)
                }
            }
            Column(Modifier.padding(pad).verticalScroll(rememberScrollState())) {
                JsonFields(
                    state,
                    FormSpec(
                        hide = setOf("id", "activeProfileName", "activeAnimeProfileName"),
                        labels = mapOf(
                            "is4k" to "4K server", "isDefault" to "Default server", "useSsl" to "Use SSL", "apiKey" to "API key", "baseUrl" to "URL base",
                            "activeProfileId" to "Quality profile", "activeDirectory" to "Root folder", "activeAnimeProfileId" to "Anime quality profile",
                            "activeAnimeDirectory" to "Anime root folder", "activeLanguageProfileId" to "Language profile", "activeAnimeLanguageProfileId" to "Anime language profile",
                            "tags" to "Tag IDs", "animeTags" to "Anime tag IDs", "externalUrl" to "External URL", "syncEnabled" to "Enable scan",
                            "preventSearch" to "Don't search on add", "tagRequests" to "Tag requests with the user", "enableSeasonFolders" to "Season folders",
                        ),
                        help = mapOf("activeProfileId" to if (t == null) "Test the connection to pick from the server's profiles" else ""),
                        types = mapOf("tags" to "intlist", "animeTags" to "intlist", "activeProfileId" to "int", "activeAnimeProfileId" to "int",
                            "activeLanguageProfileId" to "int", "activeAnimeLanguageProfileId" to "int"),
                        choices = choices,
                        order = listOf("name", "hostname", "port", "useSsl", "apiKey", "baseUrl", "is4k", "isDefault"),
                    ),
                )
                t?.objs("tags")?.takeIf { it.isNotEmpty() }?.let { tags ->
                    Text(
                        "Tags on this server: " + tags.joinToString(", ") { "${it.int("id")} = ${it.str("label")}" },
                        style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                    OutlinedButton(enabled = !act.busy, onClick = {
                        act.run("Connected to $label") {
                            val res = App.api.post("/settings/$kind/test", state.obj) as? JSONObject ?: JSONObject()
                            test = res
                            res.str("urlBase")?.let { if (state.get("baseUrl")?.toString().isNullOrEmpty()) state.set("baseUrl", it) }
                        }
                    }) { Text("Test") }
                    Button(enabled = !act.busy && (state.dirty || id == null), onClick = {
                        act.run("Saved", onOk = { nav.popBackStack() }) {
                            if (id == null) App.api.post("/settings/$kind", state.obj) else App.api.put("/settings/$kind/$id", state.obj)
                        }
                    }) { Text("Save") }
                }
                Spacer(Modifier.height(48.dp))
            }
        }
    }
    if (confirmDelete && id != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this $label server?") },
            confirmButton = {
                TextButton({
                    confirmDelete = false
                    act.run("Deleted", onOk = { nav.popBackStack() }) { App.api.delete("/settings/$kind/$id") }
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton({ confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

// ---------------- notifications ----------------

private val agents = listOf(
    "email" to "Email", "discord" to "Discord", "slack" to "Slack", "telegram" to "Telegram", "pushbullet" to "Pushbullet",
    "pushover" to "Pushover", "gotify" to "Gotify", "ntfy" to "ntfy", "webhook" to "Webhook", "webpush" to "Web push",
)

@Composable
private fun NotificationAgentsScreen() {
    val nav = LocalNav.current
    val load = rememberLoad("agents") {
        agents.map { (k, l) -> Triple(k, l, try { App.api.obj("/settings/notifications/$k").bool("enabled") } catch (_: Exception) { false }) }
    }
    Scaffold(topBar = { BackBar("Notifications", { nav.popBackStack() }) }) { pad ->
        Loaded(load, Modifier.padding(pad)) { list ->
            LazyColumn(Modifier.padding(pad), contentPadding = ListBottom) {
                item {
                    SettingsSection(null) {
                        list.forEachIndexed { i, (k, l, on) ->
                            if (i > 0) ItemDivider()
                            NavItem(Icons.Outlined.Notifications, l, if (on) "On" else "Off", trailing = {
                                if (on) Pill("On", Status.available)
                            }) { nav.navigate("admin/notif:$k") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationAgentScreen(agent: String) {
    val nav = LocalNav.current
    val act = rememberActions()
    val name = agents.firstOrNull { it.first == agent }?.second ?: agent
    val path = "/settings/notifications/$agent"
    val load = rememberLoad(path) { App.api.obj(path) }
    Scaffold(topBar = { BackBar(name, { nav.popBackStack() }) }) { pad ->
        Loaded(load, Modifier.padding(pad)) { data ->
            val state = remember(data) { FormState(data) }
            Column(Modifier.padding(pad).verticalScroll(rememberScrollState())) {
                JsonFields(
                    state,
                    FormSpec(
                        hide = setOf("types"),
                        labels = mapOf("enabled" to "Enable agent", "embedPoster" to "Embed poster", "options" to "Options",
                            "jsonPayload" to "JSON payload (base64)", "authHeader" to "Authorization header", "webhookUrl" to "Webhook URL",
                            "botAPI" to "Bot token", "chatId" to "Chat ID", "sendSilently" to "Send silently", "smtpHost" to "SMTP host",
                            "smtpPort" to "SMTP port", "authUser" to "SMTP user", "authPass" to "SMTP password", "emailFrom" to "Sender address",
                            "senderName" to "Sender name", "secure" to "Use SSL", "ignoreTls" to "Ignore TLS", "requireTls" to "Require TLS",
                            "allowSelfSigned" to "Allow self-signed certificates", "userEmailRequired" to "Require user email", "pgpPrivateKey" to "PGP private key",
                            "pgpPassword" to "PGP password", "accessToken" to "Access token", "channelTag" to "Channel tag", "userToken" to "User key",
                            "botUsername" to "Bot username", "botAvatarUrl" to "Bot avatar URL", "enableMentions" to "Enable mentions",
                            "messageThreadId" to "Thread ID", "supportVariables" to "Support URL variables", "priority" to "Priority",
                            "sound" to "Sound", "url" to "Server URL", "token" to "Application token", "topic" to "Topic"),
                        multiline = setOf("pgpPrivateKey", "jsonPayload"),
                    ),
                )
                SheetHeader("Send notifications for")
                state.version
                val types = (state.get("types") as? Number)?.toLong() ?: 0L
                NotificationTypeList.forEach { (bit, label) ->
                    Row(Modifier.fillMaxWidth().clickable { state.set("types", types xor bit) }.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.Checkbox(types and bit != 0L, { state.set("types", if (it) types or bit else types and bit.inv()) })
                        Text(label)
                    }
                }
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                    OutlinedButton(enabled = !act.busy, onClick = { act.run("Test notification sent") { App.api.post("$path/test", state.obj) } }) { Text("Send test") }
                    Button(enabled = state.dirty && !act.busy, onClick = { act.run("Saved", onOk = { state.saved() }) { App.api.post(path, state.obj) } }) { Text("Save") }
                }
                Spacer(Modifier.height(48.dp))
            }
        }
    }
}

// ---------------- discover sliders ----------------

private val sliderNames = mapOf(
    1 to "Recently added", 2 to "Recent requests", 3 to "Watchlist", 4 to "Trending", 5 to "Popular movies", 6 to "Movie genres",
    7 to "Upcoming movies", 8 to "Studios", 9 to "Popular series", 10 to "Series genres", 11 to "Upcoming series", 12 to "Networks",
    13 to "Movie keywords", 14 to "Movie genre", 15 to "Series keywords", 16 to "Series genre", 17 to "Search", 18 to "Studio",
    19 to "Network", 20 to "Movie streaming services", 21 to "Series streaming services",
)

@Composable
private fun DiscoverSlidersScreen() {
    val nav = LocalNav.current
    val act = rememberActions()
    val sliders = remember { mutableStateListOf<JSONObject>() }
    var dirty by remember { mutableStateOf(false) }
    var reload by remember { mutableIntStateOf(0) }
    var add by remember { mutableStateOf(false) }
    val load = rememberLoad(reload) {
        val list = App.api.arr("/settings/discover").objects().sortedBy { it.int("order") }
        sliders.clear(); sliders.addAll(list); dirty = false
        list
    }
    Scaffold(
        topBar = {
            BackBar("Discover sliders", { nav.popBackStack() }) {
                TextButton({ act.run("Sliders reset", onOk = { reload++ }) { App.api.get("/settings/discover/reset") } }) { Text("Reset") }
            }
        },
        floatingActionButton = { FloatingActionButton({ add = true }) { Icon(Icons.Outlined.Add, "Add slider") } },
    ) { pad ->
        Loaded(load, Modifier.padding(pad)) {
            LazyColumn(Modifier.padding(pad), contentPadding = PaddingValues(bottom = 120.dp)) {
                item {
                    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.End) {
                        Button(enabled = dirty && !act.busy, onClick = {
                            act.run("Saved", onOk = { reload++ }) {
                                val arr = JSONArray()
                                sliders.forEachIndexed { i, s -> arr.put(JSONObject(s.toString()).put("order", i)) }
                                App.api.post("/settings/discover", arr)
                            }
                        }) { Text("Save order") }
                    }
                }
                items(sliders.size, key = { sliders[it].int("id") }) { i ->
                    val s = sliders[i]
                    ListItem(
                        headlineContent = { Text(s.str("title") ?: sliderNames[s.int("type")] ?: "Slider") },
                        supportingContent = { Text((if (s.bool("isBuiltIn")) "Built in" else sliderNames[s.int("type")] ?: "Custom") + (s.str("data")?.let { " · $it" } ?: "")) },
                        leadingContent = {
                            Switch(s.bool("enabled"), { on -> sliders[i] = JSONObject(s.toString()).put("enabled", on); dirty = true })
                        },
                        trailingContent = {
                            Row {
                                IconButton({ if (i > 0) { val x = sliders.removeAt(i); sliders.add(i - 1, x); dirty = true } }, enabled = i > 0) { Icon(Icons.Outlined.ArrowUpward, "Up") }
                                IconButton({ if (i < sliders.lastIndex) { val x = sliders.removeAt(i); sliders.add(i + 1, x); dirty = true } }, enabled = i < sliders.lastIndex) { Icon(Icons.Outlined.ArrowDownward, "Down") }
                                if (!s.bool("isBuiltIn")) IconButton({
                                    act.run("Slider deleted", onOk = { reload++ }) { App.api.delete("/settings/discover/${s.int("id")}") }
                                }) { Icon(Icons.Outlined.Delete, "Delete") }
                            }
                        },
                    )
                }
            }
        }
    }
    if (add) {
        var type by remember { mutableStateOf("13") }
        var title by remember { mutableStateOf("") }
        var data by remember { mutableStateOf("") }
        val hints = mapOf(
            "13" to "Keyword IDs, comma separated", "15" to "Keyword IDs, comma separated", "14" to "Genre ID", "16" to "Genre ID",
            "17" to "Search text", "18" to "Studio ID", "19" to "Network ID", "20" to "Region,provider IDs (e.g. US,8)", "21" to "Region,provider IDs (e.g. US,8)",
        )
        AlertDialog(
            onDismissRequest = { add = false },
            title = { Text("Add slider") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    com.vknxworks.seerrctrl.ui.Dropdown("Type", (13..21).map { it.toString() to (sliderNames[it] ?: "") }, type) { type = it }
                    OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true)
                    OutlinedTextField(data, { data = it }, label = { Text(hints[type] ?: "Value") }, singleLine = true)
                    Text("IDs are TMDB ids; find them in the web UI's slider search or on themoviedb.org.", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton({
                    add = false
                    act.run("Slider added", onOk = { reload++ }) {
                        App.api.post("/settings/discover/add", jsonOf("type" to type.toInt(), "title" to title, "data" to data, "enabled" to true))
                    }
                }, enabled = title.isNotBlank() && data.isNotBlank()) { Text("Add") }
            },
            dismissButton = { TextButton({ add = false }) { Text("Cancel") } },
        )
    }
}

// ---------------- jobs & cache ----------------

@Composable
private fun JobsScreen() {
    val nav = LocalNav.current
    val act = rememberActions()
    var reload by remember { mutableIntStateOf(0) }
    val load = rememberLoad(reload) { App.api.arr("/settings/jobs").objects() to App.api.obj("/settings/cache") }
    var editing by remember { mutableStateOf<JSONObject?>(null) }
    LaunchedEffect(load.value) {
        if (load.value?.first?.any { it.bool("running") } == true) { delay(3000); load.reload(silent = true) }
    }
    Scaffold(topBar = { BackBar("Jobs & cache", { nav.popBackStack() }) { IconButton({ reload++ }) { Icon(Icons.Outlined.Sync, "Refresh") } } }) { pad ->
        Loaded(load, Modifier.padding(pad)) { (jobs, cache) ->
            LazyColumn(Modifier.padding(pad), contentPadding = ListBottom) {
                item { SheetHeader("Jobs") }
                items(jobs, key = { it.str("id") ?: "" }) { j ->
                    ListItem(
                        headlineContent = { Text(j.str("name") ?: j.str("id") ?: "") },
                        supportingContent = {
                            Text(
                                if (j.bool("running")) "Running…"
                                else "Next: ${prettyDateTime(j.str("nextExecutionTime")) ?: "—"} · ${j.str("cronSchedule") ?: ""}"
                            )
                        },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton({ editing = j }) { Icon(Icons.Outlined.Schedule, "Change schedule") }
                                if (j.bool("running")) {
                                    IconButton({ act.run("Cancelled", onOk = { reload++ }) { App.api.post("/settings/jobs/${j.str("id")}/cancel") } }) { Icon(Icons.Outlined.Stop, "Cancel") }
                                } else {
                                    IconButton({ act.run("Started ${j.str("name")}", onOk = { reload++ }) { App.api.post("/settings/jobs/${j.str("id")}/run") } }) { Icon(Icons.Outlined.PlayArrow, "Run now") }
                                }
                            }
                        },
                    )
                }
                item { SheetHeader("API caches") }
                items(cache.objs("apiCaches"), key = { "c" + it.str("id") }) { c ->
                    val st = c.obj("stats")
                    ListItem(
                        headlineContent = { Text(c.str("name") ?: c.str("id") ?: "") },
                        supportingContent = { Text("${st.int("keys")} keys · ${st.int("hits")} hits · ${st.int("misses")} misses · ${humanBytes(st.long("ksize") + st.long("vsize"))}") },
                        trailingContent = {
                            TextButton({ act.run("Cache flushed", onOk = { reload++ }) { App.api.post("/settings/cache/${c.str("id")}/flush") } }) { Text("Flush") }
                        },
                    )
                }
                item { SheetHeader("Image cache") }
                for (k in listOf("tmdb", "avatar")) {
                    cache.obj("imageCache").obj(k)?.let { ic ->
                        item {
                            ListItem(
                                headlineContent = { Text(if (k == "tmdb") "TMDB images" else "Avatars") },
                                supportingContent = { Text("${ic.int("imageCount")} images · ${humanBytes(ic.long("size"))}") },
                            )
                        }
                    }
                }
                cache.obj("dnsCache").obj("stats")?.let { dns ->
                    item { SheetHeader("DNS cache") }
                    item {
                        ListItem(
                            headlineContent = { Text("${dns.int("size")} entries") },
                            supportingContent = { Text("${dns.int("hits")} hits · ${dns.int("misses")} misses") },
                        )
                    }
                }
            }
        }
    }
    editing?.let { j ->
        var cron by remember(j) { mutableStateOf(j.str("cronSchedule") ?: "") }
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(j.str("name") ?: "Schedule") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(cron, { cron = it }, label = { Text("Cron schedule") }, singleLine = true)
                    Text("Six fields: second minute hour day month weekday. Example: 0 */5 * * * * runs every 5 minutes.", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton({
                    editing = null
                    act.run("Schedule updated", onOk = { reload++ }) { App.api.post("/settings/jobs/${j.str("id")}/schedule", jsonOf("schedule" to cron.trim())) }
                }) { Text("Save") }
            },
            dismissButton = { TextButton({ editing = null }) { Text("Cancel") } },
        )
    }
}

// ---------------- logs ----------------

@Composable
private fun LogsScreen() {
    val nav = LocalNav.current
    var level by remember { mutableStateOf("info") }
    var search by remember { mutableStateOf("") }
    var take by remember { mutableIntStateOf(100) }
    var reload by remember { mutableIntStateOf(0) }
    val load = rememberLoad(level, search, take, reload) {
        if (search.isNotEmpty()) delay(350)
        App.api.arr("/settings/logs", mapOf("take" to take, "skip" to 0, "filter" to level, "search" to search.ifBlank { null })).objects()
    }
    Scaffold(topBar = { BackBar("Logs", { nav.popBackStack() }) { IconButton({ reload++ }) { Icon(Icons.Outlined.Sync, "Refresh") } } }) { pad ->
        Column(Modifier.padding(pad)) {
            OutlinedTextField(
                search, { search = it }, placeholder = { Text("Search logs") }, leadingIcon = { Icon(Icons.Outlined.Search, null) },
                singleLine = true, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            )
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("debug" to "Debug", "info" to "Info", "warn" to "Warnings", "error" to "Errors").forEach { (k, l) ->
                    FilterChip(level == k, { level = k }, label = { Text(l) })
                }
            }
            Loaded(load) { logs ->
                LazyColumn(contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(logs) { l ->
                        val lv = l.str("level") ?: "info"
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                            Column(Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Pill(lv.uppercase(), when (lv) { "error" -> Status.declined; "warn" -> Status.pending; "debug" -> MaterialTheme.colorScheme.outline; else -> Status.processing })
                                    Spacer(Modifier.width(8.dp))
                                    Text(l.str("label") ?: "", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                    Text(prettyDateTime(l.str("timestamp")) ?: l.str("timestamp") ?: "", style = MaterialTheme.typography.labelSmall)
                                }
                                SelectionContainer {
                                    Text(l.str("message") ?: "", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(top = 4.dp))
                                }
                                l.obj("data")?.let { d ->
                                    if (d.length() > 0) Text(d.toString(2), style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                    if (logs.isEmpty()) item { EmptyBox(Icons.AutoMirrored.Outlined.Article, "No log lines") }
                    if (logs.size >= take) item { TextButton({ take += 200 }) { Text("Load more") } }
                }
            }
        }
    }
}

// ---------------- about ----------------

@Composable
private fun AboutScreen() {
    val nav = LocalNav.current
    val load = rememberLoad("about") { App.api.obj("/settings/about") to (try { App.api.obj("/status") } catch (_: Exception) { JSONObject() }) }
    Scaffold(topBar = { BackBar("About", { nav.popBackStack() }) }) { pad ->
        Loaded(load, Modifier.padding(pad)) { (about, status) ->
            Column(Modifier.padding(pad).verticalScroll(rememberScrollState())) {
                SettingsSection("Server") {
                    Fact("Version", about.str("version") ?: status.str("version") ?: "?")
                    status.str("commitTag")?.let { Fact("Commit", it) }
                    if (status.bool("updateAvailable")) Fact("Update", "An update is available" + (status.intOrNull("commitsBehind")?.let { " ($it commits behind)" } ?: ""))
                    if (status.bool("restartRequired")) Fact("Restart", "A restart is required to apply settings")
                    Fact("Media items", "${about.int("totalMediaItems")}")
                    Fact("Requests", "${about.int("totalRequests")}")
                    about.str("tz")?.let { Fact("Time zone", it) }
                    about.str("appDataPath")?.let { Fact("Data folder", it) }
                }
                SettingsSection("Links") {
                    val ctx = androidx.compose.ui.platform.LocalContext.current
                    NavItem(Icons.Outlined.Info, "Documentation", "docs.seerr.dev") { com.vknxworks.seerrctrl.ui.openUrl(ctx, "https://docs.seerr.dev") }
                    ItemDivider()
                    NavItem(Icons.Outlined.Info, "GitHub", "seerr-team/seerr") { com.vknxworks.seerrctrl.ui.openUrl(ctx, "https://github.com/seerr-team/seerr") }
                }
            }
        }
    }
}

@Composable
private fun Fact(k: String, v: String) {
    ListItem(headlineContent = { Text(v) }, overlineContent = { Text(k) }, colors = com.vknxworks.seerrctrl.ui.itemColors())
}

// ---------------- override rules ----------------

@Composable
private fun OverrideRulesScreen() {
    val nav = LocalNav.current
    val act = rememberActions()
    var reload by remember { mutableIntStateOf(0) }
    var editing by remember { mutableStateOf<JSONObject?>(null) }
    val load = rememberLoad(reload) { App.api.arr("/overrideRule").objects() }
    Scaffold(topBar = { BackBar("Override rules", { nav.popBackStack() }) }) { pad ->
        Loaded(load, Modifier.padding(pad)) { rules ->
            LazyColumn(Modifier.padding(pad), contentPadding = ListBottom) {
                item {
                    Text(
                        "Rules send matching requests to a specific server, profile, folder or tags. Create new rules in the web UI; you can edit and delete them here.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp),
                    )
                }
                items(rules, key = { it.int("id") }) { r ->
                    val conds = listOfNotNull(
                        r.str("users")?.let { "users $it" }, r.str("genre")?.let { "genres $it" },
                        r.str("language")?.let { "language $it" }, r.str("keywords")?.let { "keywords $it" },
                    )
                    ListItem(
                        headlineContent = { Text((r.intOrNull("radarrServiceId")?.let { "Radarr #$it" } ?: r.intOrNull("sonarrServiceId")?.let { "Sonarr #$it" } ?: "Rule") + " · rule ${r.int("id")}") },
                        supportingContent = { Text(conds.joinToString(" · ").ifEmpty { "No conditions" }) },
                        trailingContent = {
                            Row {
                                IconButton({ editing = r }) { Icon(Icons.Outlined.Tune, "Edit") }
                                IconButton({ act.run("Rule deleted", onOk = { reload++ }) { App.api.delete("/overrideRule/${r.int("id")}") } }) { Icon(Icons.Outlined.Delete, "Delete") }
                            }
                        },
                    )
                }
                if (rules.isEmpty()) item { EmptyBox(Icons.Outlined.Rule, "No override rules") }
            }
        }
    }
    editing?.let { r ->
        val state = remember(r) { FormState(r) }
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text("Edit rule ${r.int("id")}") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    JsonFields(state, FormSpec(hide = setOf("id", "createdAt", "updatedAt"),
                        types = mapOf("radarrServiceId" to "int", "sonarrServiceId" to "int", "profileId" to "int", "users" to "string",
                            "genre" to "string", "language" to "string", "keywords" to "string", "rootFolder" to "string", "tags" to "string")))
                }
            },
            confirmButton = {
                TextButton({
                    editing = null
                    act.run("Rule saved", onOk = { reload++ }) { App.api.put("/overrideRule/${r.int("id")}", state.obj) }
                }) { Text("Save") }
            },
            dismissButton = { TextButton({ editing = null }) { Text("Cancel") } },
        )
    }
}
