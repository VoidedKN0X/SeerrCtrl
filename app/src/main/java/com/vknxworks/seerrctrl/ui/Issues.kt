package com.vknxworks.seerrctrl.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.vknxworks.seerrctrl.App
import com.vknxworks.seerrctrl.data.IssueTypes
import com.vknxworks.seerrctrl.data.Perm
import com.vknxworks.seerrctrl.data.int
import com.vknxworks.seerrctrl.data.jsonOf
import com.vknxworks.seerrctrl.data.obj
import com.vknxworks.seerrctrl.data.objs
import com.vknxworks.seerrctrl.data.prettyDateTime
import com.vknxworks.seerrctrl.data.str
import com.vknxworks.seerrctrl.data.timeAgo
import kotlinx.coroutines.launch
import org.json.JSONObject

private fun issueWhere(i: JSONObject): String? {
    val s = i.int("problemSeason")
    val e = i.int("problemEpisode")
    return when {
        s > 0 && e > 0 -> "Season $s, episode $e"
        s > 0 -> "Season $s"
        else -> null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IssuesTab() {
    var filter by rememberSaveable { mutableStateOf("open") }
    var sort by rememberSaveable { mutableStateOf("added") }
    var reloadKey by remember { mutableIntStateOf(0) }
    val items = remember(filter, sort, reloadKey) { mutableStateListOf<JSONObject>() }
    var total by remember(filter, sort, reloadKey) { mutableIntStateOf(-1) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val list = rememberLazyListState()
    val scope = rememberCoroutineScope()

    suspend fun more() {
        if (loading || (total >= 0 && items.size >= total)) return
        loading = true; error = null
        try {
            val j = App.api.obj("/issue", mapOf("take" to 20, "skip" to items.size, "filter" to filter, "sort" to sort))
            items.addAll(j.objs("results").filter { r -> items.none { it.int("id") == r.int("id") } })
            total = j.obj("pageInfo").int("results", items.size)
        } catch (e: Exception) { error = e.message }
        loading = false
    }
    LaunchedEffect(filter, sort, reloadKey) { more() }
    val nearEnd by remember { derivedStateOf { (list.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) >= list.layoutInfo.totalItemsCount - 4 } }
    LaunchedEffect(nearEnd) { if (nearEnd && items.isNotEmpty()) more() }

    Column(Modifier.fillMaxSize()) {
        Text("Issues", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 16.dp, top = 8.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("open" to "Open", "resolved" to "Resolved", "all" to "All").forEach { (k, l) -> FilterChip(filter == k, { filter = k }, label = { Text(l, maxLines = 1) }) }
            FilterChip(sort == "modified", { sort = if (sort == "added") "modified" else "added" }, label = { Text("Recently changed", maxLines = 1) })
        }
        PullToRefreshBox(false, { reloadKey++ }, Modifier.fillMaxSize()) {
            LazyColumn(state = list, contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(items, key = { it.int("id") }) { i -> IssueCard(i) }
                if (loading) item { Box(Modifier.fillMaxWidth().padding(24.dp), Alignment.Center) { CircularProgressIndicator() } }
                if (!loading && items.isEmpty()) {
                    item {
                        if (error != null) ErrorBox(error!!) { scope.launch { more() } }
                        else EmptyBox(Icons.Outlined.CheckCircle, if (filter == "open") "No open issues" else "No issues")
                    }
                }
            }
        }
    }
}

@Composable
private fun IssueCard(i: JSONObject) {
    val nav = LocalNav.current
    val fake = JSONObject().put("media", i.obj("media"))
    val d = rememberMediaFor(fake)
    val open = i.int("status") == 1
    val by = i.obj("createdBy")
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth().clickable { nav.navigate("issue/${i.int("id")}") },
    ) {
        Row(Modifier.padding(12.dp)) {
            Box(Modifier.width(64.dp).aspectRatio(2f / 3f).clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
                App.session.tmdb(d.str("posterPath"), "w154")?.let { AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(d.str("title") ?: d.str("name") ?: "…", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Pill(IssueTypes.name(i.int("issueType")), MaterialTheme.colorScheme.secondary)
                    Pill(if (open) "Open" else "Resolved", if (open) Status.pending else Status.available)
                }
                issueWhere(i)?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                i.objs("comments").firstOrNull()?.str("message")?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Text(
                    "${by.str("displayName") ?: "Someone"} · ${timeAgo(i.str("createdAt")) ?: ""} · ${i.objs("comments").size} comments",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** One issue with its comment thread; managers (and the reporter) can resolve, reopen or delete it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IssueScreen(id: Int) {
    val nav = LocalNav.current
    val s = App.session
    val act = rememberActions()
    val load = rememberLoad(id) { App.api.obj("/issue/$id") }
    var reply by remember { mutableStateOf("") }
    var confirmDelete by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<JSONObject?>(null) }

    Scaffold(
        topBar = {
            BackBar("Issue #$id", { nav.popBackStack() }) {
                val i = load.value
                if (i != null && (s.can(Perm.MANAGE_ISSUES) || i.obj("createdBy").int("id") == s.userId)) {
                    IconButton({ confirmDelete = true }) { Icon(Icons.Outlined.Delete, "Delete issue") }
                }
            }
        },
        bottomBar = {
            val i = load.value
            if (i != null) Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                Row(Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(reply, { reply = it }, placeholder = { Text("Add a comment") }, modifier = Modifier.weight(1f), maxLines = 4)
                    Spacer(Modifier.width(8.dp))
                    FilledIconButton(
                        enabled = reply.isNotBlank() && !act.busy,
                        onClick = {
                            act.run(onOk = { reply = ""; load.reload(silent = true) }) {
                                App.api.post("/issue/${i.int("id")}/comment", jsonOf("message" to reply.trim()))
                            }
                        },
                    ) { Icon(Icons.AutoMirrored.Outlined.Send, "Send") }
                }
            }
        },
    ) { pad ->
        Loaded(load, Modifier.padding(pad)) { i ->
            val media = i.obj("media")
            val type = media.str("mediaType") ?: "movie"
            val d = rememberMediaFor(JSONObject().put("media", media))
            val open = i.int("status") == 1
            val canManage = s.can(Perm.MANAGE_ISSUES) || i.obj("createdBy").int("id") == s.userId
            LazyColumn(Modifier.padding(pad), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Row(Modifier.clickable { nav.openMedia(type, media.int("tmdbId")) }) {
                        Box(Modifier.width(80.dp).aspectRatio(2f / 3f).clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
                            s.tmdb(d.str("posterPath"), "w185")?.let { AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(d.str("title") ?: d.str("name") ?: "…", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Pill(IssueTypes.name(i.int("issueType")) + " issue", MaterialTheme.colorScheme.secondary)
                                Pill(if (open) "Open" else "Resolved", if (open) Status.pending else Status.available)
                            }
                            issueWhere(i)?.let { Text(it) }
                            Text(
                                "Reported by ${i.obj("createdBy").str("displayName") ?: "?"} · ${prettyDateTime(i.str("createdAt")) ?: ""}",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                if (canManage) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (open) {
                                FilledTonalButton({
                                    act.run("Marked as resolved", onOk = { load.reload(silent = true) }) { App.api.post("/issue/${i.int("id")}/resolved") }
                                }) { Icon(Icons.Outlined.CheckCircle, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Resolve") }
                            } else {
                                OutlinedButton({
                                    act.run("Reopened", onOk = { load.reload(silent = true) }) { App.api.post("/issue/${i.int("id")}/open") }
                                }) { Icon(Icons.AutoMirrored.Outlined.Undo, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Reopen") }
                            }
                        }
                    }
                }
                items(i.objs("comments"), key = { it.int("id") }) { c ->
                    Comment(c, onEdit = { editing = c }, onDelete = {
                        act.run("Comment deleted", onOk = { load.reload(silent = true) }) { App.api.delete("/issueComment/${c.int("id")}") }
                    })
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this issue?") },
            confirmButton = {
                TextButton({
                    confirmDelete = false
                    act.run("Issue deleted", onOk = { nav.popBackStack() }) { App.api.delete("/issue/$id") }
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton({ confirmDelete = false }) { Text("Cancel") } },
        )
    }
    editing?.let { c ->
        var text by remember(c) { mutableStateOf(c.str("message") ?: "") }
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text("Edit comment") },
            text = { OutlinedTextField(text, { text = it }, minLines = 3, modifier = Modifier.fillMaxWidth()) },
            confirmButton = {
                TextButton({
                    editing = null
                    act.run(onOk = { load.reload(silent = true) }) { App.api.put("/issueComment/${c.int("id")}", jsonOf("message" to text.trim())) }
                }) { Text("Save") }
            },
            dismissButton = { TextButton({ editing = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun Comment(c: JSONObject, onEdit: () -> Unit, onDelete: () -> Unit) {
    val s = App.session
    val user = c.obj("user")
    val mine = user.int("id") == s.userId
    var menu by remember { mutableStateOf(false) }
    Row {
        Avatar(user.str("avatar"), user.str("displayName") ?: "?", 36.dp)
        Spacer(Modifier.width(10.dp))
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh), modifier = Modifier.weight(1f)) {
            Column(Modifier.padding(start = 12.dp, top = 4.dp, bottom = 12.dp, end = 4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(user.str("displayName") ?: "Someone", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Text(timeAgo(c.str("createdAt")) ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (mine || s.can(Perm.MANAGE_ISSUES)) {
                        Box {
                            IconButton({ menu = true }) { Icon(Icons.Outlined.MoreVert, "More") }
                            DropdownMenu(menu, { menu = false }) {
                                if (mine) DropdownMenuItem({ Text("Edit") }, { menu = false; onEdit() }, leadingIcon = { Icon(Icons.Outlined.Edit, null) })
                                DropdownMenuItem({ Text("Delete") }, { menu = false; onDelete() }, leadingIcon = { Icon(Icons.Outlined.Delete, null) })
                            }
                        }
                    }
                }
                Text(c.str("message") ?: "", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
