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
import com.vknxworks.seerrctrl.ui.back
import com.vknxworks.seerrctrl.ui.go
import com.vknxworks.seerrctrl.ui.rememberActions
import com.vknxworks.seerrctrl.ui.rememberCloseScreen
import com.vknxworks.seerrctrl.ui.rememberLoad
import kotlinx.coroutines.delay
import com.vknxworks.seerrctrl.SecureWindow
import org.json.JSONArray
import org.json.JSONObject
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.platform.LocalContext
import com.vknxworks.seerrctrl.R

/** Every server settings page from the web UI, as one list. */
@Composable
fun AdminSettingsScreen() {
    SecureWindow()
    val nav = LocalNav.current
    val ms = App.session.mediaServer
    Scaffold(topBar = { BackBar(stringResource(R.string.server_settings), { nav.back() }, subtitle = App.session.appTitle) }) { pad ->
        LazyColumn(Modifier.padding(pad), contentPadding = ListBottom) {
            item {
                SettingsSection(stringResource(R.string.group_server)) {
                    NavItem(Icons.Outlined.Settings, stringResource(R.string.general), stringResource(R.string.title_url_image_cache_discover)) { nav.go("admin/general") }
                    ItemDivider()
                    NavItem(Icons.Outlined.Group, stringResource(R.string.users), stringResource(R.string.sign_in_methods_default_permissions)) { nav.go("admin/users") }
                    ItemDivider()
                    NavItem(Icons.Outlined.Lan, stringResource(R.string.network), stringResource(R.string.proxy_csrf_dns_cache_timeouts)) { nav.go("admin/network") }
                    ItemDivider()
                    NavItem(Icons.Outlined.Info, stringResource(R.string.group_about), stringResource(R.string.version_totals_paths)) { nav.go("admin/about") }
                }
            }
            item {
                SettingsSection(stringResource(R.string.media)) {
                    NavItem(Icons.Outlined.Movie, ms.label.takeIf { ms != MediaServer.None } ?: stringResource(R.string.media_server), stringResource(R.string.connection_libraries_and_scans)) { nav.go("admin/mediaserver") }
                    ItemDivider()
                    NavItem(Icons.Outlined.Dns, stringResource(R.string.services), stringResource(R.string.radarr_and_sonarr_servers)) { nav.go("admin/services") }
                    ItemDivider()
                    NavItem(Icons.Outlined.Tune, stringResource(R.string.metadata), stringResource(R.string.metadata_providers_for_movies_series)) { nav.go("admin/metadata") }
                    if (ms == MediaServer.Plex) {
                        ItemDivider()
                        NavItem(Icons.Outlined.Storage, stringResource(R.string.tautulli), stringResource(R.string.watch_history_from_tautulli)) { nav.go("admin/tautulli") }
                    }
                    ItemDivider()
                    NavItem(Icons.Outlined.Rule, stringResource(R.string.override_rules), stringResource(R.string.route_requests_to_specific_servers)) { nav.go("admin/overrides") }
                }
            }
            item {
                SettingsSection(stringResource(R.string.behaviour)) {
                    NavItem(Icons.Outlined.Notifications, stringResource(R.string.notifications), stringResource(R.string.email_discord_telegram_webhooks_and)) { nav.go("admin/notifications") }
                    ItemDivider()
                    NavItem(Icons.Outlined.Explore, stringResource(R.string.discover), stringResource(R.string.choose_and_order_the_discover)) { nav.go("admin/discover") }
                    ItemDivider()
                    NavItem(Icons.Outlined.Schedule, stringResource(R.string.jobs_cache), stringResource(R.string.run_scans_now_change_schedules_flush)) { nav.go("admin/jobs") }
                    ItemDivider()
                    NavItem(Icons.AutoMirrored.Outlined.Article, stringResource(R.string.logs), stringResource(R.string.server_log_filterable_by_level)) { nav.go("admin/logs") }
                }
            }
        }
    }
}

@Composable
fun AdminSectionScreen(section: String) {
    SecureWindow()
    when {
        section == "general" -> SettingsForm(stringResource(R.string.general), "/settings/main", generalSpec, hideKeys = usersKeys + setOf("defaultPermissions", "defaultQuotas"), regen = true)
        section == "users" -> UsersSettings()
        section == "network" -> SettingsForm(stringResource(R.string.network), "/settings/network", networkSpec)
        section == "mediaserver" -> MediaServerSettings()
        section == "metadata" -> SettingsForm(stringResource(R.string.metadata), "/settings/metadatas", FormSpec(), method = "PUT")
        section == "tautulli" -> SettingsForm(stringResource(R.string.tautulli), "/settings/tautulli", FormSpec(labels = mapOf("urlBase" to App.str(R.string.url_base), "externalUrl" to App.str(R.string.external_url))))
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
        "applicationTitle" to App.str(R.string.application_title), "applicationUrl" to App.str(R.string.application_url), "cacheImages" to App.str(R.string.cache_images_locally),
        "hideAvailable" to App.str(R.string.hide_available_media), "hideBlocklisted" to App.str(R.string.hide_blocklisted_items), "hideRequested" to App.str(R.string.hide_requested_media),
        "discoverRegion" to App.str(R.string.discover_region_iso_code), "streamingRegion" to App.str(R.string.streaming_region_iso_code), "originalLanguage" to App.str(R.string.discover_language_iso_codes),
        "partialRequestsEnabled" to App.str(R.string.allow_partial_series_requests), "enableSpecialEpisodes" to App.str(R.string.allow_special_episode_requests),
        "youtubeUrl" to App.str(R.string.youtube_url), "versionCheck" to App.str(R.string.check_for_updates), "blocklistedTags" to App.str(R.string.blocklisted_tags),
        "blocklistedTagsLimit" to App.str(R.string.blocklisted_tags_limit), "blocklistRegion" to App.str(R.string.blocklist_region), "blocklistLanguage" to App.str(R.string.blocklist_language),
    ),
    help = mapOf(
        "apiKey" to App.str(R.string.read_only_here_use_regenerate_to),
        "cacheImages" to App.str(R.string.proxy_and_cache_tmdb_images_through),
        "applicationUrl" to App.str(R.string.used_in_notifications_and_links),
    ),
    readOnly = setOf("apiKey", "mediaServerType"),
    hide = setOf("mediaServerType", "locale"),
    order = listOf("applicationTitle", "applicationUrl", "apiKey"),
)

private val networkSpec = FormSpec(
    labels = mapOf(
        "csrfProtection" to App.str(R.string.csrf_protection), "forceIpv4First" to App.str(R.string.force_ipv4_resolution_first), "trustProxy" to App.str(R.string.trust_proxy_behind_a_reverse_proxy),
        "apiRequestTimeout" to App.str(R.string.api_request_timeout_ms), "bypassFilter" to App.str(R.string.bypass_proxy_for_addresses), "bypassLocalAddresses" to App.str(R.string.bypass_proxy_for_local_addresses),
        "forceMinTtl" to App.str(R.string.minimum_ttl_s), "forceMaxTtl" to App.str(R.string.maximum_ttl_s_1_off),
    ),
    help = mapOf("csrfProtection" to App.str(R.string.only_turn_on_if_you_know_what_it)),
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
    Scaffold(topBar = { BackBar(title, { nav.back() }) }) { pad ->
        Loaded(load, Modifier.padding(pad)) { data ->
            val state = remember(data) { FormState(data) }
            Column(Modifier.padding(pad).verticalScroll(rememberScrollState())) {
                Spacer(Modifier.height(8.dp))
                JsonFields(state, spec.copy(hide = spec.hide + hideKeys))
                extra(state)
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                    if (regen) {
                        OutlinedButton({
                            act.run(App.str(R.string.new_api_key_generated), onOk = { load.reload(silent = true) }) { App.api.post("/settings/main/regenerate") }
                        }) { Text(stringResource(R.string.regenerate_api_key)) }
                    }
                    Button(
                        enabled = state.dirty && !act.busy,
                        onClick = {
                            act.run(App.str(R.string.saved), onOk = { state.saved(); try { App.session.refresh() } catch (_: Exception) {} }) {
                                if (method == "PUT") App.api.put(path, state.obj) else App.api.post(path, state.obj)
                            }
                        },
                    ) { Text(stringResource(R.string.save)) }
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
    Scaffold(topBar = { BackBar(stringResource(R.string.user_settings), { nav.back() }) }) { pad ->
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
                            "localLogin" to App.str(R.string.allow_email_password_sign_in), "mediaServerLogin" to App.str(R.string.allow_media_server_sign_in),
                            "newPlexLogin" to App.str(R.string.allow_new_plex_users_to_sign_in), "defaultQuotas" to App.str(R.string.default_request_limits),
                            "movie.quotaLimit" to App.str(R.string.movie_requests), "movie.quotaDays" to App.str(R.string.per_days_2), "quotaLimit" to App.str(R.string.request_limit), "quotaDays" to App.str(R.string.period_days),
                        ),
                        types = mapOf("quotaLimit" to "int", "quotaDays" to "int"),
                        hide = if (App.session.mediaServer != MediaServer.Plex) setOf("newPlexLogin") else emptySet(),
                    ),
                )
                SheetHeader(stringResource(R.string.default_permissions_for_new_users))
                Box(Modifier.padding(horizontal = 8.dp)) { PermissionEditor(perms) { perms = it } }
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.End) {
                    Button(
                        enabled = (state.dirty || perms != main.long("defaultPermissions")) && !act.busy,
                        onClick = {
                            act.run(App.str(R.string.saved), onOk = { state.saved(); load.reload(silent = true) }) {
                                val body = JSONObject(main.toString())
                                state.obj.keys().forEach { body.put(it, state.obj.opt(it)) }
                                body.put("defaultPermissions", perms)
                                App.api.post("/settings/main", body)
                            }
                        },
                    ) { Text(stringResource(R.string.save)) }
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

    Scaffold(topBar = { BackBar(ms.label, { nav.back() }) }) { pad ->
        Loaded(load, Modifier.padding(pad)) { data ->
            val state = remember(data) { FormState(data) }
            Column(Modifier.padding(pad).verticalScroll(rememberScrollState())) {
                SheetHeader(stringResource(R.string.connection))
                JsonFields(
                    state,
                    FormSpec(
                        hide = setOf("libraries", "machineId", "apiKey", "serverId"),
                        labels = mapOf("ip" to App.str(R.string.hostname_or_ip), "useSsl" to App.str(R.string.use_ssl), "urlBase" to App.str(R.string.url_base), "externalHostname" to App.str(R.string.external_url),
                            "jellyfinForgotPasswordUrl" to App.str(R.string.forgot_password_url), "webAppUrl" to App.str(R.string.web_app_url)),
                        readOnly = if (ms == MediaServer.Plex) emptySet() else setOf("name"),
                    ),
                )
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.End) {
                    Button(enabled = state.dirty && !act.busy, onClick = {
                        act.run(App.str(R.string.saved), onOk = { state.saved() }) { App.api.post("/settings/$key", state.obj) }
                    }) { Text(stringResource(R.string.save_connection)) }
                }

                SheetHeader(stringResource(R.string.libraries))
                Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton({
                        act.run(App.str(R.string.libraries_synced), onOk = { poll++ }) { App.api.post("/settings/$key/library/sync") }
                    }) { Icon(Icons.Outlined.Sync, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.sync_libraries)) }
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

                SheetHeader(stringResource(R.string.full_library_scan))
                val sc = scan
                Card(Modifier.padding(16.dp).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (sc?.bool("running") == true) {
                            val total = sc.int("total")
                            val prog = sc.int("progress")
                            Text(stringResource(R.string.scanning_of, sc.obj("currentLibrary").str("name") ?: "", prog, total))
                            LinearProgressIndicator({ if (total > 0) prog.toFloat() / total else 0f }, Modifier.fillMaxWidth())
                            OutlinedButton({ act.run(App.str(R.string.scan_cancelled), onOk = { poll++ }) { App.api.post("/settings/$key/sync", jsonOf("cancel" to true)) } }) {
                                Icon(Icons.Outlined.Stop, null); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.cancel_scan))
                            }
                        } else {
                            Text(stringResource(R.string.checks_every_enabled_library_for_new), style = MaterialTheme.typography.bodySmall)
                            FilledTonalButton({ act.run(App.str(R.string.scan_started), onOk = { poll++ }) { App.api.post("/settings/$key/sync", jsonOf("start" to true)) } }) {
                                Icon(Icons.Outlined.PlayArrow, null); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.start_full_scan))
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
    Scaffold(topBar = { BackBar(stringResource(R.string.services), { nav.back() }) { IconButton({ load.reload(silent = true) }) { Icon(Icons.Outlined.Sync, stringResource(R.string.refresh)) } } }) { pad ->
        Loaded(load, Modifier.padding(pad)) { (radarr, sonarr) ->
            LazyColumn(Modifier.padding(pad), contentPadding = ListBottom) {
                for ((kind, list) in listOf("radarr" to radarr, "sonarr" to sonarr)) {
                    item {
                        SettingsSection(if (kind == "radarr") stringResource(R.string.radarr) else stringResource(R.string.sonarr)) {
                            list.forEachIndexed { i, srv ->
                                if (i > 0) ItemDivider()
                                NavItem(
                                    Icons.Outlined.Dns, srv.str("name") ?: stringResource(R.string.group_server),
                                    listOfNotNull(
                                        (if (srv.bool("useSsl")) "https://" else "http://") + srv.str("hostname") + ":" + srv.int("port") + (srv.str("baseUrl") ?: ""),
                                        if (srv.bool("is4k")) "4K" else null,
                                        if (srv.bool("isDefault")) App.str(R.string.default_2) else null,
                                    ).joinToString(" · "),
                                ) { nav.go("admin/service:$kind:${srv.int("id")}") }
                            }
                            if (list.isNotEmpty()) ItemDivider()
                            NavItem(Icons.Outlined.Add, stringResource(R.string.add_server, if (kind == "radarr") "Radarr" else "Sonarr")) { nav.go("admin/service:$kind:new") }
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
    val closeScreen = rememberCloseScreen()
    val act = rememberActions()
    val label = if (kind == "radarr") App.str(R.string.radarr) else App.str(R.string.sonarr)
    val load = rememberLoad(kind, id) {
        if (id == null) newService(kind) else App.api.arr("/settings/$kind").objects().firstOrNull { it.int("id") == id } ?: error(App.str(R.string.server_not_found))
    }
    var test by remember { mutableStateOf<JSONObject?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    Scaffold(topBar = {
        BackBar(if (id == null) stringResource(R.string.add_server, label) else stringResource(R.string.edit_server, label), { nav.back() }) {
            if (id != null) IconButton({ confirmDelete = true }) { Icon(Icons.Outlined.Delete, stringResource(R.string.lock_delete)) }
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
                put("minimumAvailability", listOf("announced" to App.str(R.string.announced), "inCinemas" to App.str(R.string.in_cinemas), "released" to App.str(R.string.released)))
                put("seriesType", listOf("standard" to App.str(R.string.standard), "daily" to App.str(R.string.daily), "anime" to App.str(R.string.anime)))
                put("animeSeriesType", listOf("standard" to App.str(R.string.standard), "daily" to App.str(R.string.daily), "anime" to App.str(R.string.anime)))
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
                            "is4k" to App.str(R.string.4k_server), "isDefault" to App.str(R.string.default_server), "useSsl" to App.str(R.string.use_ssl), "apiKey" to App.str(R.string.api_key), "baseUrl" to App.str(R.string.url_base),
                            "activeProfileId" to App.str(R.string.quality_profile), "activeDirectory" to App.str(R.string.root_folder), "activeAnimeProfileId" to App.str(R.string.anime_quality_profile),
                            "activeAnimeDirectory" to App.str(R.string.anime_root_folder), "activeLanguageProfileId" to App.str(R.string.language_profile), "activeAnimeLanguageProfileId" to App.str(R.string.anime_language_profile),
                            "tags" to App.str(R.string.tag_ids), "animeTags" to App.str(R.string.anime_tag_ids), "externalUrl" to App.str(R.string.external_url), "syncEnabled" to App.str(R.string.enable_scan),
                            "preventSearch" to App.str(R.string.don_t_search_on_add), "tagRequests" to App.str(R.string.tag_requests_with_the_user), "enableSeasonFolders" to App.str(R.string.season_folders),
                        ),
                        help = mapOf("activeProfileId" to if (t == null) App.str(R.string.test_the_connection_to_pick_from_the) else ""),
                        types = mapOf("tags" to "intlist", "animeTags" to "intlist", "activeProfileId" to "int", "activeAnimeProfileId" to "int",
                            "activeLanguageProfileId" to "int", "activeAnimeLanguageProfileId" to "int"),
                        choices = choices,
                        order = listOf("name", "hostname", "port", "useSsl", "apiKey", "baseUrl", "is4k", "isDefault"),
                    ),
                )
                t?.objs("tags")?.takeIf { it.isNotEmpty() }?.let { tags ->
                    Text(
                        stringResource(R.string.tags_on_this_server, tags.joinToString(", ") { "${it.int("id")} = ${it.str("label")}" }),
                        style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                    OutlinedButton(enabled = !act.busy, onClick = {
                        act.run(App.str(R.string.connected_to, label)) {
                            val res = App.api.post("/settings/$kind/test", state.obj) as? JSONObject ?: JSONObject()
                            test = res
                            res.str("urlBase")?.let { if (state.get("baseUrl")?.toString().isNullOrEmpty()) state.set("baseUrl", it) }
                        }
                    }) { Text(stringResource(R.string.test)) }
                    Button(enabled = !act.busy && (state.dirty || id == null), onClick = {
                        act.run(App.str(R.string.saved), onOk = { closeScreen() }) {
                            if (id == null) App.api.post("/settings/$kind", state.obj) else App.api.put("/settings/$kind/$id", state.obj)
                        }
                    }) { Text(stringResource(R.string.save)) }
                }
                Spacer(Modifier.height(48.dp))
            }
        }
    }
    if (confirmDelete && id != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_this_server, label)) },
            confirmButton = {
                TextButton({
                    confirmDelete = false
                    act.run(App.str(R.string.deleted), onOk = { closeScreen() }) { App.api.delete("/settings/$kind/$id") }
                }) { Text(stringResource(R.string.lock_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton({ confirmDelete = false }) { Text(stringResource(R.string.lock_cancel)) } },
        )
    }
}

// ---------------- notifications ----------------

private val agents = listOf(
    "email" to App.str(R.string.email), "discord" to App.str(R.string.discord), "slack" to App.str(R.string.slack), "telegram" to App.str(R.string.telegram), "pushbullet" to App.str(R.string.pushbullet),
    "pushover" to App.str(R.string.pushover), "gotify" to App.str(R.string.gotify), "ntfy" to "ntfy", "webhook" to App.str(R.string.webhook), "webpush" to App.str(R.string.web_push),
)

@Composable
private fun NotificationAgentsScreen() {
    val nav = LocalNav.current
    val load = rememberLoad("agents") {
        agents.map { (k, l) -> Triple(k, l, try { App.api.obj("/settings/notifications/$k").bool("enabled") } catch (_: Exception) { false }) }
    }
    Scaffold(topBar = { BackBar(stringResource(R.string.notifications), { nav.back() }) }) { pad ->
        Loaded(load, Modifier.padding(pad)) { list ->
            LazyColumn(Modifier.padding(pad), contentPadding = ListBottom) {
                item {
                    SettingsSection(null) {
                        list.forEachIndexed { i, (k, l, on) ->
                            if (i > 0) ItemDivider()
                            NavItem(Icons.Outlined.Notifications, l, if (on) stringResource(R.string.on) else stringResource(R.string.off), trailing = {
                                if (on) Pill(stringResource(R.string.on), Status.available)
                            }) { nav.go("admin/notif:$k") }
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
    Scaffold(topBar = { BackBar(name, { nav.back() }) }) { pad ->
        Loaded(load, Modifier.padding(pad)) { data ->
            val state = remember(data) { FormState(data) }
            Column(Modifier.padding(pad).verticalScroll(rememberScrollState())) {
                JsonFields(
                    state,
                    FormSpec(
                        hide = setOf("types"),
                        labels = mapOf("enabled" to App.str(R.string.enable_agent), "embedPoster" to App.str(R.string.embed_poster), "options" to App.str(R.string.options),
                            "jsonPayload" to App.str(R.string.json_payload_base64), "authHeader" to App.str(R.string.authorization_header), "webhookUrl" to App.str(R.string.webhook_url),
                            "botAPI" to App.str(R.string.bot_token), "chatId" to App.str(R.string.chat_id), "sendSilently" to App.str(R.string.send_silently), "smtpHost" to App.str(R.string.smtp_host),
                            "smtpPort" to App.str(R.string.smtp_port), "authUser" to App.str(R.string.smtp_user), "authPass" to App.str(R.string.smtp_password), "emailFrom" to App.str(R.string.sender_address),
                            "senderName" to App.str(R.string.sender_name), "secure" to App.str(R.string.use_ssl), "ignoreTls" to App.str(R.string.ignore_tls), "requireTls" to App.str(R.string.require_tls),
                            "allowSelfSigned" to App.str(R.string.allow_self_signed_certificates), "userEmailRequired" to App.str(R.string.require_user_email), "pgpPrivateKey" to App.str(R.string.pgp_private_key),
                            "pgpPassword" to App.str(R.string.pgp_password), "accessToken" to App.str(R.string.access_token), "channelTag" to App.str(R.string.channel_tag), "userToken" to App.str(R.string.user_key),
                            "botUsername" to App.str(R.string.bot_username), "botAvatarUrl" to App.str(R.string.bot_avatar_url), "enableMentions" to App.str(R.string.enable_mentions),
                            "messageThreadId" to App.str(R.string.thread_id), "supportVariables" to App.str(R.string.support_url_variables), "priority" to App.str(R.string.priority),
                            "sound" to App.str(R.string.sound), "url" to App.str(R.string.server_url), "token" to App.str(R.string.application_token), "topic" to App.str(R.string.topic)),
                        multiline = setOf("pgpPrivateKey", "jsonPayload"),
                    ),
                )
                SheetHeader(stringResource(R.string.send_notifications_for))
                state.version
                val types = (state.get("types") as? Number)?.toLong() ?: 0L
                NotificationTypeList.forEach { (bit, label) ->
                    Row(Modifier.fillMaxWidth().clickable { state.set("types", types xor bit) }.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.Checkbox(types and bit != 0L, { state.set("types", if (it) types or bit else types and bit.inv()) })
                        Text(label)
                    }
                }
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                    OutlinedButton(enabled = !act.busy, onClick = { act.run(App.str(R.string.test_notification_sent)) { App.api.post("$path/test", state.obj) } }) { Text(stringResource(R.string.send_test)) }
                    Button(enabled = state.dirty && !act.busy, onClick = { act.run(App.str(R.string.saved), onOk = { state.saved() }) { App.api.post(path, state.obj) } }) { Text(stringResource(R.string.save)) }
                }
                Spacer(Modifier.height(48.dp))
            }
        }
    }
}

// ---------------- discover sliders ----------------

private val sliderNames = mapOf(
    1 to App.str(R.string.recently_added), 2 to App.str(R.string.recent_requests), 3 to App.str(R.string.watchlist), 4 to App.str(R.string.trending), 5 to App.str(R.string.popular_movies), 6 to App.str(R.string.movie_genres),
    7 to App.str(R.string.upcoming_movies), 8 to App.str(R.string.studios), 9 to App.str(R.string.popular_series), 10 to App.str(R.string.series_genres), 11 to App.str(R.string.upcoming_series), 12 to App.str(R.string.networks),
    13 to App.str(R.string.movie_keywords), 14 to App.str(R.string.movie_genre), 15 to App.str(R.string.series_keywords), 16 to App.str(R.string.series_genre), 17 to App.str(R.string.tab_search), 18 to App.str(R.string.studio),
    19 to App.str(R.string.network), 20 to App.str(R.string.movie_streaming_services), 21 to App.str(R.string.series_streaming_services),
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
            BackBar(stringResource(R.string.discover_sliders), { nav.back() }) {
                TextButton({ act.run(App.str(R.string.sliders_reset), onOk = { reload++ }) { App.api.get("/settings/discover/reset") } }) { Text(stringResource(R.string.reset)) }
            }
        },
        floatingActionButton = { FloatingActionButton({ add = true }) { Icon(Icons.Outlined.Add, stringResource(R.string.add_slider)) } },
    ) { pad ->
        Loaded(load, Modifier.padding(pad)) {
            LazyColumn(Modifier.padding(pad), contentPadding = PaddingValues(bottom = 120.dp)) {
                item {
                    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.End) {
                        Button(enabled = dirty && !act.busy, onClick = {
                            act.run(App.str(R.string.saved), onOk = { reload++ }) {
                                val arr = JSONArray()
                                sliders.forEachIndexed { i, s -> arr.put(JSONObject(s.toString()).put("order", i)) }
                                App.api.post("/settings/discover", arr)
                            }
                        }) { Text(stringResource(R.string.save_order)) }
                    }
                }
                items(sliders.size, key = { sliders[it].int("id") }) { i ->
                    val s = sliders[i]
                    ListItem(
                        headlineContent = { Text(s.str("title") ?: sliderNames[s.int("type")] ?: stringResource(R.string.slider)) },
                        supportingContent = { Text((if (s.bool("isBuiltIn")) App.str(R.string.built_in) else sliderNames[s.int("type")] ?: App.str(R.string.custom)) + (s.str("data")?.let { " · $it" } ?: "")) },
                        leadingContent = {
                            Switch(s.bool("enabled"), { on -> sliders[i] = JSONObject(s.toString()).put("enabled", on); dirty = true })
                        },
                        trailingContent = {
                            Row {
                                IconButton({ if (i > 0) { val x = sliders.removeAt(i); sliders.add(i - 1, x); dirty = true } }, enabled = i > 0) { Icon(Icons.Outlined.ArrowUpward, stringResource(R.string.up)) }
                                IconButton({ if (i < sliders.lastIndex) { val x = sliders.removeAt(i); sliders.add(i + 1, x); dirty = true } }, enabled = i < sliders.lastIndex) { Icon(Icons.Outlined.ArrowDownward, stringResource(R.string.down)) }
                                if (!s.bool("isBuiltIn")) IconButton({
                                    act.run(App.str(R.string.slider_deleted), onOk = { reload++ }) { App.api.delete("/settings/discover/${s.int("id")}") }
                                }) { Icon(Icons.Outlined.Delete, stringResource(R.string.lock_delete)) }
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
            "13" to App.str(R.string.keyword_ids_comma_separated), "15" to App.str(R.string.keyword_ids_comma_separated), "14" to App.str(R.string.genre_id), "16" to App.str(R.string.genre_id),
            "17" to App.str(R.string.search_text), "18" to App.str(R.string.studio_id), "19" to App.str(R.string.network_id), "20" to App.str(R.string.region_provider_ids_e_g_us_8), "21" to App.str(R.string.region_provider_ids_e_g_us_8),
        )
        AlertDialog(
            onDismissRequest = { add = false },
            title = { Text(stringResource(R.string.add_slider)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    com.vknxworks.seerrctrl.ui.Dropdown(stringResource(R.string.type), (13..21).map { it.toString() to (sliderNames[it] ?: "") }, type) { type = it }
                    OutlinedTextField(title, { title = it }, label = { Text(stringResource(R.string.title)) }, singleLine = true)
                    OutlinedTextField(data, { data = it }, label = { Text(hints[type] ?: stringResource(R.string.value)) }, singleLine = true)
                    Text(stringResource(R.string.ids_are_tmdb_ids_find_them_in_the), style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton({
                    add = false
                    act.run(App.str(R.string.slider_added), onOk = { reload++ }) {
                        App.api.post("/settings/discover/add", jsonOf("type" to type.toInt(), "title" to title, "data" to data, "enabled" to true))
                    }
                }, enabled = title.isNotBlank() && data.isNotBlank()) { Text(stringResource(R.string.add)) }
            },
            dismissButton = { TextButton({ add = false }) { Text(stringResource(R.string.lock_cancel)) } },
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
    Scaffold(topBar = { BackBar(stringResource(R.string.jobs_cache), { nav.back() }) { IconButton({ reload++ }) { Icon(Icons.Outlined.Sync, stringResource(R.string.refresh)) } } }) { pad ->
        Loaded(load, Modifier.padding(pad)) { (jobs, cache) ->
            LazyColumn(Modifier.padding(pad), contentPadding = ListBottom) {
                item { SheetHeader(stringResource(R.string.jobs)) }
                items(jobs, key = { it.str("id") ?: "" }) { j ->
                    ListItem(
                        headlineContent = { Text(j.str("name") ?: j.str("id") ?: "") },
                        supportingContent = {
                            Text(
                                if (j.bool("running")) stringResource(R.string.running)
                                else stringResource(R.string.next, prettyDateTime(j.str("nextExecutionTime")) ?: "—", j.str("cronSchedule") ?: "")
                            )
                        },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton({ editing = j }) { Icon(Icons.Outlined.Schedule, stringResource(R.string.change_schedule)) }
                                if (j.bool("running")) {
                                    IconButton({ act.run(App.str(R.string.cancelled), onOk = { reload++ }) { App.api.post("/settings/jobs/${j.str("id")}/cancel") } }) { Icon(Icons.Outlined.Stop, stringResource(R.string.lock_cancel)) }
                                } else {
                                    IconButton({ act.run(App.str(R.string.started, j.str("name")), onOk = { reload++ }) { App.api.post("/settings/jobs/${j.str("id")}/run") } }) { Icon(Icons.Outlined.PlayArrow, stringResource(R.string.run_now)) }
                                }
                            }
                        },
                    )
                }
                item { SheetHeader(stringResource(R.string.api_caches)) }
                items(cache.objs("apiCaches"), key = { "c" + it.str("id") }) { c ->
                    val st = c.obj("stats")
                    ListItem(
                        headlineContent = { Text(c.str("name") ?: c.str("id") ?: "") },
                        supportingContent = { Text(stringResource(R.string.keys_hits_misses, st.int("keys"), st.int("hits"), st.int("misses"), humanBytes(st.long("ksize") + st.long("vsize")))) },
                        trailingContent = {
                            TextButton({ act.run(App.str(R.string.cache_flushed), onOk = { reload++ }) { App.api.post("/settings/cache/${c.str("id")}/flush") } }) { Text(stringResource(R.string.flush)) }
                        },
                    )
                }
                item { SheetHeader(stringResource(R.string.image_cache)) }
                for (k in listOf("tmdb", "avatar")) {
                    cache.obj("imageCache").obj(k)?.let { ic ->
                        item {
                            ListItem(
                                headlineContent = { Text(if (k == "tmdb") stringResource(R.string.tmdb_images) else stringResource(R.string.avatars)) },
                                supportingContent = { Text(stringResource(R.string.images, ic.int("imageCount"), humanBytes(ic.long("size")))) },
                            )
                        }
                    }
                }
                cache.obj("dnsCache").obj("stats")?.let { dns ->
                    item { SheetHeader(stringResource(R.string.dns_cache)) }
                    item {
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.entries, dns.int("size"))) },
                            supportingContent = { Text(stringResource(R.string.hits_misses, dns.int("hits"), dns.int("misses"))) },
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
            title = { Text(j.str("name") ?: stringResource(R.string.schedule)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(cron, { cron = it }, label = { Text(stringResource(R.string.cron_schedule)) }, singleLine = true)
                    Text(stringResource(R.string.six_fields_second_minute_hour_day), style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton({
                    editing = null
                    act.run(App.str(R.string.schedule_updated), onOk = { reload++ }) { App.api.post("/settings/jobs/${j.str("id")}/schedule", jsonOf("schedule" to cron.trim())) }
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = { TextButton({ editing = null }) { Text(stringResource(R.string.lock_cancel)) } },
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
    Scaffold(topBar = { BackBar(stringResource(R.string.logs), { nav.back() }) { IconButton({ reload++ }) { Icon(Icons.Outlined.Sync, stringResource(R.string.refresh)) } } }) { pad ->
        Column(Modifier.padding(pad)) {
            OutlinedTextField(
                search, { search = it }, placeholder = { Text(stringResource(R.string.search_logs)) }, leadingIcon = { Icon(Icons.Outlined.Search, null) },
                singleLine = true, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            )
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("debug" to App.str(R.string.debug), "info" to App.str(R.string.info), "warn" to App.str(R.string.warnings), "error" to App.str(R.string.errors)).forEach { (k, l) ->
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
                    if (logs.isEmpty()) item { EmptyBox(Icons.AutoMirrored.Outlined.Article, stringResource(R.string.no_log_lines)) }
                    if (logs.size >= take) item { TextButton({ take += 200 }) { Text(stringResource(R.string.load_more)) } }
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
    Scaffold(topBar = { BackBar(stringResource(R.string.group_about), { nav.back() }) }) { pad ->
        Loaded(load, Modifier.padding(pad)) { (about, status) ->
            Column(Modifier.padding(pad).verticalScroll(rememberScrollState())) {
                SettingsSection(stringResource(R.string.group_server)) {
                    Fact(stringResource(R.string.version), about.str("version") ?: status.str("version") ?: "?")
                    status.str("commitTag")?.let { Fact(stringResource(R.string.commit), it) }
                    if (status.bool("updateAvailable")) Fact(stringResource(R.string.update), status.intOrNull("commitsBehind")?.let { pluralStringResource(R.plurals.update_available_behind, it, it) } ?: stringResource(R.string.an_update_is_available))
                    if (status.bool("restartRequired")) Fact(stringResource(R.string.restart), stringResource(R.string.a_restart_is_required_to_apply))
                    Fact(stringResource(R.string.media_items), "${about.int("totalMediaItems")}")
                    Fact(stringResource(R.string.requests), "${about.int("totalRequests")}")
                    about.str("tz")?.let { Fact(stringResource(R.string.time_zone), it) }
                    about.str("appDataPath")?.let { Fact(stringResource(R.string.data_folder), it) }
                }
                SettingsSection(stringResource(R.string.links)) {
                    val ctx = androidx.compose.ui.platform.LocalContext.current
                    NavItem(Icons.Outlined.Info, stringResource(R.string.documentation), "docs.seerr.dev") { com.vknxworks.seerrctrl.ui.openUrl(ctx, "https://docs.seerr.dev") }
                    ItemDivider()
                    NavItem(Icons.Outlined.Info, stringResource(R.string.github), "seerr-team/seerr") { com.vknxworks.seerrctrl.ui.openUrl(ctx, "https://github.com/seerr-team/seerr") }
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
    Scaffold(topBar = { BackBar(stringResource(R.string.override_rules), { nav.back() }) }) { pad ->
        Loaded(load, Modifier.padding(pad)) { rules ->
            LazyColumn(Modifier.padding(pad), contentPadding = ListBottom) {
                item {
                    Text(
                        stringResource(R.string.rules_send_matching_requests_to_a),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp),
                    )
                }
                items(rules, key = { it.int("id") }) { r ->
                    val conds = listOfNotNull(
                        r.str("users")?.let { App.str(R.string.users_3, it) }, r.str("genre")?.let { App.str(R.string.genres_2, it) },
                        r.str("language")?.let { App.str(R.string.language, it) }, r.str("keywords")?.let { App.str(R.string.keywords_2, it) },
                    )
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.rule_title, r.intOrNull("radarrServiceId")?.let { stringResource(R.string.radarr_2, it) } ?: r.intOrNull("sonarrServiceId")?.let { stringResource(R.string.sonarr_2, it) } ?: stringResource(R.string.rule_2), r.int("id"))) },
                        supportingContent = { Text(conds.joinToString(" · ").ifEmpty { App.str(R.string.no_conditions) }) },
                        trailingContent = {
                            Row {
                                IconButton({ editing = r }) { Icon(Icons.Outlined.Tune, stringResource(R.string.edit)) }
                                IconButton({ act.run(App.str(R.string.rule_deleted), onOk = { reload++ }) { App.api.delete("/overrideRule/${r.int("id")}") } }) { Icon(Icons.Outlined.Delete, stringResource(R.string.lock_delete)) }
                            }
                        },
                    )
                }
                if (rules.isEmpty()) item { EmptyBox(Icons.Outlined.Rule, stringResource(R.string.no_override_rules)) }
            }
        }
    }
    editing?.let { r ->
        val state = remember(r) { FormState(r) }
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(stringResource(R.string.edit_rule, r.int("id"))) },
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
                    act.run(App.str(R.string.rule_saved), onOk = { reload++ }) { App.api.put("/overrideRule/${r.int("id")}", state.obj) }
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = { TextButton({ editing = null }) { Text(stringResource(R.string.lock_cancel)) } },
        )
    }
}
