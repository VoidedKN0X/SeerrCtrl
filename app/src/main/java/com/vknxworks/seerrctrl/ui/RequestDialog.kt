package com.vknxworks.seerrctrl.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vknxworks.seerrctrl.App
import com.vknxworks.seerrctrl.data.MediaStatus
import com.vknxworks.seerrctrl.data.Perm
import com.vknxworks.seerrctrl.data.RequestStatus
import com.vknxworks.seerrctrl.data.arr
import com.vknxworks.seerrctrl.data.bool
import com.vknxworks.seerrctrl.data.humanBytes
import com.vknxworks.seerrctrl.data.int
import com.vknxworks.seerrctrl.data.intOrNull
import com.vknxworks.seerrctrl.data.ints
import com.vknxworks.seerrctrl.data.long
import com.vknxworks.seerrctrl.data.obj
import com.vknxworks.seerrctrl.data.objects
import com.vknxworks.seerrctrl.data.objs
import com.vknxworks.seerrctrl.data.str
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

private const val ANIME_KEYWORD = 210024

/**
 * Request (or edit a request for) a movie or series: seasons, 4K, and the advanced options —
 * server, quality profile, root folder, language profile, tags and "request as".
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RequestDialog(
    type: String,
    details: JSONObject,
    is4k: Boolean,
    existing: JSONObject?,
    onDismiss: () -> Unit,
    onDone: () -> Unit,
) {
    val s = App.session
    val scope = rememberCoroutineScope()
    val snack = LocalSnackbar.current
    val tv = type == "tv"
    val tmdbId = details.int("id")
    val title = details.str("title") ?: details.str("name") ?: ""
    val info = details.obj("mediaInfo")
    val service = if (tv) "sonarr" else "radarr"
    val advanced = s.can(Perm.REQUEST_ADVANCED, Perm.MANAGE_REQUESTS, any = true)
    val canRequestAs = s.can(Perm.MANAGE_REQUESTS) && s.can(Perm.MANAGE_USERS)
    val isAnime = details.objs("keywords").any { it.int("id") == ANIME_KEYWORD }

    // ----- seasons -----
    val allSeasons = details.objs("seasons").map { it.int("seasonNumber") }.filter { s.specialEpisodes || it > 0 }
    // Seasons already available or with an open request can't be requested again.
    val taken = remember(details) {
        val set = mutableSetOf<Int>()
        info.objs("seasons").forEach {
            val st = MediaStatus.of(if (is4k) it.intOrNull("status4k") else it.intOrNull("status"))
            if (st == MediaStatus.Available || st == MediaStatus.Partial || st == MediaStatus.Processing) set += it.int("seasonNumber")
        }
        info.objs("requests").filter { it.bool("is4k") == is4k && it.int("id") != existing.int("id") }
            .filter { RequestStatus.of(it.int("status")) in setOf(RequestStatus.Pending, RequestStatus.Approved) }
            .forEach { r -> r.objs("seasons").forEach { set += it.int("seasonNumber") } }
        set
    }
    val selected = remember {
        mutableStateListOf<Int>().apply {
            if (existing != null) addAll(existing.objs("seasons").map { it.int("seasonNumber") })
            else if (!s.partialRequests) addAll(allSeasons.filter { it !in taken })
        }
    }

    // ----- advanced -----
    var servers by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var serverId by remember { mutableStateOf(existing.intOrNull("serverId")) }
    var detailsOfServer by remember { mutableStateOf<JSONObject?>(null) }
    var profileId by remember { mutableStateOf(existing.intOrNull("profileId")) }
    var rootFolder by remember { mutableStateOf(existing.str("rootFolder")) }
    var languageProfileId by remember { mutableStateOf(existing.intOrNull("languageProfileId")) }
    val tags = remember { mutableStateListOf<Int>().apply { addAll(existing.arr("tags").ints()) } }
    var users by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var asUser by remember { mutableStateOf(existing.obj("requestedBy").intOrNull("id")) }
    var quota by remember { mutableStateOf<JSONObject?>(null) }
    var showAdvanced by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (advanced) {
            try {
                servers = App.api.arr("/service/$service").objects().filter { it.bool("is4k") == is4k }
                if (serverId == null) serverId = (servers.firstOrNull { it.bool("isDefault") } ?: servers.firstOrNull())?.intOrNull("id")
            } catch (_: Exception) {}
        }
        if (canRequestAs) {
            try { users = App.api.obj("/user", mapOf("take" to 1000, "sort" to "displayname")).objs("results") } catch (_: Exception) {}
        }
    }
    LaunchedEffect(asUser) {
        try { quota = App.api.obj("/user/${asUser ?: s.userId}/quota") } catch (_: Exception) {}
    }
    LaunchedEffect(serverId) {
        val id = serverId ?: return@LaunchedEffect
        try {
            val det = App.api.obj("/service/$service/$id")
            detailsOfServer = det
            val srv = det.obj("server")
            val fresh = existing == null || existing.intOrNull("serverId") != id
            if (fresh || profileId == null) profileId = srv.intOrNull(if (isAnime && tv) "activeAnimeProfileId" else "activeProfileId") ?: srv.intOrNull("activeProfileId")
            if (fresh || rootFolder == null) rootFolder = srv.str(if (isAnime && tv) "activeAnimeDirectory" else "activeDirectory") ?: srv.str("activeDirectory")
            if (fresh || languageProfileId == null) languageProfileId = srv.intOrNull(if (isAnime) "activeAnimeLanguageProfileId" else "activeLanguageProfileId")
            if (fresh) { tags.clear(); tags.addAll(srv.arr(if (isAnime && tv) "activeAnimeTags" else "activeTags").ints()) }
        } catch (_: Exception) {}
    }

    fun submit() {
        if (tv && selected.isEmpty()) { error = "Pick at least one season"; return }
        scope.launch {
            busy = true; error = null
            try {
                val body = JSONObject()
                    .put("mediaType", type)
                    .put("mediaId", tmdbId)
                    .put("is4k", is4k)
                if (tv) {
                    details.obj("externalIds")?.intOrNull("tvdbId")?.let { body.put("tvdbId", it) }
                    body.put("seasons", JSONArray(selected.sorted()))
                }
                if (advanced && serverId != null) {
                    body.put("serverId", serverId)
                    profileId?.let { body.put("profileId", it) }
                    rootFolder?.let { body.put("rootFolder", it) }
                    languageProfileId?.let { body.put("languageProfileId", it) }
                    body.put("tags", JSONArray(tags.toList()))
                }
                asUser?.let { if (it != s.userId) body.put("userId", it) }
                if (existing != null) {
                    App.api.put("/request/${existing.int("id")}", body)
                    snack.showSnackbar("Request updated")
                } else {
                    App.api.post("/request", body)
                    // Usage stats: only movie or series, 4K or not, never which title.
                    App.telemetry.track(
                        "request_made",
                        mapOf("media" to if (tv) "tv" else "movie", "is4k" to is4k.toString()),
                    )
                    snack.showSnackbar("Requested $title" + if (is4k) " in 4K" else "")
                }
                MediaCache.invalidate(type, tmdbId)
                onDone()
            } catch (e: Exception) { error = e.message }
            busy = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                (if (existing != null) "Edit request" else if (is4k) "Request in 4K" else "Request") + "\n" + title,
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                quota?.let { q ->
                    val part = q.obj(if (tv) "tv" else "movie")
                    val limit = part.int("limit")
                    if (limit > 0) {
                        Text(
                            "${part.int("remaining")} of $limit ${if (tv) "season" else "movie"} requests left (resets every ${part.int("days")} days)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (part.bool("restricted")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                if (tv) {
                    val free = allSeasons.filter { it !in taken || it in selected }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(free.isNotEmpty() && selected.containsAll(free), { on ->
                            selected.clear(); if (on) selected.addAll(free)
                        })
                        Text("All seasons", fontWeight = FontWeight.SemiBold)
                    }
                    HorizontalDivider()
                    details.objs("seasons").filter { it.int("seasonNumber") in allSeasons }.forEach { season ->
                        val n = season.int("seasonNumber")
                        val blocked = n in taken && n !in selected
                        val st = info.objs("seasons").firstOrNull { it.int("seasonNumber") == n }
                            ?.let { MediaStatus.of(if (is4k) it.intOrNull("status4k") else it.intOrNull("status")) }
                        Row(
                            Modifier.fillMaxWidth().clickable(enabled = !blocked) { if (n in selected) selected.remove(n) else selected.add(n) },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(n in selected, { if (it) selected.add(n) else selected.remove(n) }, enabled = !blocked)
                            Column(Modifier.weight(1f)) {
                                Text(season.str("name") ?: "Season $n")
                                Text("${season.int("episodeCount")} episodes", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (st != null && st != MediaStatus.Unknown) MediaStatusPill(st)
                            else if (blocked) Pill("Requested", Status.processing)
                        }
                    }
                }

                if (advanced && servers.isNotEmpty() || canRequestAs) {
                    Row(
                        Modifier.fillMaxWidth().clickable { showAdvanced = !showAdvanced }.padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Advanced", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        Icon(if (showAdvanced) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null)
                    }
                }
                if (showAdvanced) {
                    if (advanced && servers.isNotEmpty()) {
                        if (servers.size > 1) {
                            Dropdown("Server", servers.map { it.int("id").toString() to (it.str("name") ?: "Server") + if (it.bool("isDefault")) " (default)" else "" }, serverId?.toString() ?: "") {
                                serverId = it.toIntOrNull()
                            }
                        }
                        val det = detailsOfServer
                        if (det == null) CircularProgressIndicator(Modifier.padding(8.dp))
                        else {
                            Dropdown("Quality profile", det.objs("profiles").map { it.int("id").toString() to (it.str("name") ?: "") }, profileId?.toString() ?: "") {
                                profileId = it.toIntOrNull()
                            }
                            Dropdown(
                                "Root folder",
                                det.objs("rootFolders").map { (it.str("path") ?: "") to ((it.str("path") ?: "") + (it.long("freeSpace").takeIf { f -> f > 0 }?.let { f -> " (${humanBytes(f)} free)" } ?: "")) },
                                rootFolder ?: "",
                            ) { rootFolder = it }
                            val langs = det.objs("languageProfiles")
                            if (langs.isNotEmpty()) {
                                Dropdown("Language profile", langs.map { it.int("id").toString() to (it.str("name") ?: "") }, languageProfileId?.toString() ?: "") {
                                    languageProfileId = it.toIntOrNull()
                                }
                            }
                            val tagList = det.objs("tags")
                            if (tagList.isNotEmpty()) {
                                Text("Tags", style = MaterialTheme.typography.labelLarge)
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    tagList.forEach { t ->
                                        val id = t.int("id")
                                        FilterChip(id in tags, { if (id in tags) tags.remove(id) else tags.add(id) }, label = { Text(t.str("label") ?: "$id") })
                                    }
                                }
                            }
                        }
                    }
                    if (canRequestAs && users.isNotEmpty()) {
                        Dropdown(
                            "Request as",
                            users.map { it.int("id").toString() to (it.str("displayName") ?: it.str("email") ?: "User ${it.int("id")}") },
                            (asUser ?: s.userId).toString(),
                        ) { asUser = it.toIntOrNull() }
                    }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Spacer(Modifier.height(4.dp))
            }
        },
        confirmButton = {
            TextButton(::submit, enabled = !busy) {
                if (busy) CircularProgressIndicator(Modifier.width(18.dp).height(18.dp), strokeWidth = 2.dp)
                else Text(
                    when {
                        existing != null -> "Save"
                        tv && selected.isNotEmpty() -> "Request ${selected.size} season" + if (selected.size > 1) "s" else ""
                        else -> "Request"
                    }
                )
            }
        },
        dismissButton = { TextButton(onDismiss) { Text("Cancel") } },
    )
}

