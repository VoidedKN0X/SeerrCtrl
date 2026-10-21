package com.vknxworks.seerrctrl.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Theaters
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
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
import com.vknxworks.seerrctrl.data.MediaStatus
import com.vknxworks.seerrctrl.data.Perm
import com.vknxworks.seerrctrl.data.RequestStatus
import com.vknxworks.seerrctrl.data.bool
import com.vknxworks.seerrctrl.data.int
import com.vknxworks.seerrctrl.data.intOrNull
import com.vknxworks.seerrctrl.data.obj
import com.vknxworks.seerrctrl.data.objs
import com.vknxworks.seerrctrl.data.str
import com.vknxworks.seerrctrl.data.timeAgo
import com.vknxworks.seerrctrl.data.year
import kotlinx.coroutines.launch
import org.json.JSONObject

private val requestFilters = listOf(
    "all" to "All", "pending" to "Pending", "approved" to "Approved", "processing" to "Processing",
    "available" to "Available", "unavailable" to "Unavailable", "failed" to "Failed", "deleted" to "Deleted", "completed" to "Completed",
)

/** The request list (everyone's for managers, your own otherwise), with filters and approve/decline. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestsTab(onChanged: () -> Unit = {}, userId: Int? = null) {
    var filter by rememberSaveable { mutableStateOf(if (App.session.can(Perm.MANAGE_REQUESTS) && userId == null) "pending" else "all") }
    var sort by rememberSaveable { mutableStateOf("added") }
    var mediaType by rememberSaveable { mutableStateOf("all") }
    var reloadKey by remember { mutableIntStateOf(0) }
    val items = remember(filter, sort, mediaType, reloadKey) { mutableStateListOf<JSONObject>() }
    var total by remember(filter, sort, mediaType, reloadKey) { mutableIntStateOf(-1) }
    var loading by remember { mutableStateOf(false) }
    var refreshing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val list = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val swipeActs = rememberActions()

    suspend fun more() {
        if (loading || (total >= 0 && items.size >= total)) return
        loading = true; error = null
        try {
            val j = App.api.obj(
                "/request",
                mapOf("take" to 20, "skip" to items.size, "filter" to filter, "sort" to sort, "mediaType" to mediaType, "requestedBy" to userId),
            )
            items.addAll(j.objs("results").filter { r -> items.none { it.int("id") == r.int("id") } })
            total = j.obj("pageInfo").int("results", items.size)
        } catch (e: Exception) { error = e.message }
        loading = false; refreshing = false
    }
    LaunchedEffect(filter, sort, mediaType, reloadKey) { more() }
    val nearEnd by remember { derivedStateOf { (list.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) >= list.layoutInfo.totalItemsCount - 4 } }
    LaunchedEffect(nearEnd) { if (nearEnd && items.isNotEmpty()) more() }

    fun changed() { reloadKey++; onChanged() }

    Column(Modifier.fillMaxSize()) {
        if (userId == null) TabTitle("Requests")
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            requestFilters.forEach { (k, l) -> FilterChip(filter == k, { filter = k }, label = { Text(l, maxLines = 1) }) }
        }
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("all" to "Movies & series", "movie" to "Movies", "tv" to "Series").forEach { (k, l) ->
                FilterChip(mediaType == k, { mediaType = k }, label = { Text(l, maxLines = 1) })
            }
            FilterChip(sort == "modified", { sort = if (sort == "added") "modified" else "added" }, label = { Text("Recently changed", maxLines = 1) })
        }
        PullToRefreshBox(refreshing, { refreshing = true; scope.launch { changed() } }, Modifier.fillMaxSize()) {
            LazyColumn(state = list, contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 128.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (App.session.can(Perm.MANAGE_REQUESTS) && items.any { RequestStatus.of(it.int("status")) == RequestStatus.Pending }) {
                    item(key = "hint") {
                        Text(
                            "Swipe a pending request right to approve, left to decline",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                items(items, key = { it.int("id") }) { r ->
                    SwipeToDecide(
                        r, swipeActs,
                        onDecided = { approved ->
                            // Drop it straight away from the pending list; elsewhere refresh to show the new status.
                            if (filter == "pending") { items.remove(r); total = (total - 1).coerceAtLeast(0); onChanged() }
                            else changed()
                        },
                        onFailed = ::changed,
                    ) { RequestCard(r, onChanged = ::changed) }
                }
                if (loading) item { Box(Modifier.fillMaxWidth().padding(24.dp), Alignment.Center) { CircularProgressIndicator() } }
                if (!loading && items.isEmpty()) {
                    item {
                        if (error != null) ErrorBox(error!!) { scope.launch { more() } }
                        else EmptyBox(Icons.Outlined.Theaters, "No requests here")
                    }
                }
            }
        }
    }
}

/** Movie/series details for a request's media, from the shared cache. */
@Composable
fun rememberMediaFor(r: JSONObject): JSONObject? {
    val media = r.obj("media")
    val type = media.str("mediaType") ?: r.str("type") ?: "movie"
    val tmdbId = media.int("tmdbId")
    val d by produceState(MediaCache.peek(type, tmdbId), tmdbId) {
        if (value == null) value = try { MediaCache.get(type, tmdbId) } catch (_: Exception) { null }
    }
    return d
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RequestCard(r: JSONObject, onChanged: () -> Unit) {
    val nav = LocalNav.current
    val s = App.session
    val act = rememberActions()
    val media = r.obj("media")
    val type = media.str("mediaType") ?: r.str("type") ?: "movie"
    val tmdbId = media.int("tmdbId")
    val d = rememberMediaFor(r)
    val status = RequestStatus.of(r.int("status"))
    val is4k = r.bool("is4k")
    val avail = MediaStatus.of(media.intOrNull(if (is4k) "status4k" else "status"))
    val by = r.obj("requestedBy")
    val mine = by.int("id") == s.userId
    val manage = s.can(Perm.MANAGE_REQUESTS)
    var edit by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth().clickable { nav.openMedia(type, tmdbId) },
    ) {
        Row(Modifier.padding(12.dp)) {
            Box(Modifier.width(76.dp).aspectRatio(2f / 3f).clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
                s.tmdb(d.str("posterPath"), "w185")?.let { AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    d.str("title") ?: d.str("name") ?: "Loading…", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
                Text(
                    listOfNotNull(year(d.str("releaseDate") ?: d.str("firstAirDate")), if (type == "tv") "Series" else "Movie").joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (is4k) Pill("4K", MaterialTheme.colorScheme.tertiary)
                    RequestPill(status)
                    if (status == RequestStatus.Approved || status == RequestStatus.Completed) MediaStatusPill(avail)
                }
                val seasons = r.objs("seasons").map { it.int("seasonNumber") }.sorted()
                if (type == "tv" && seasons.isNotEmpty()) {
                    Text(
                        "Seasons " + seasons.joinToString(", "), style = MaterialTheme.typography.bodySmall,
                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar(by.str("avatar"), by.str("displayName") ?: "?", 20.dp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "${by.str("displayName") ?: "Someone"} · ${timeAgo(r.str("createdAt")) ?: ""}",
                        style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable { nav.go("user/${by.int("id")}") },
                    )
                }
                r.obj("modifiedBy")?.let { m ->
                    if (status != RequestStatus.Pending) {
                        Text(
                            "${if (status == RequestStatus.Declined) "Declined" else "Approved"} by ${m.str("displayName") ?: "?"} · ${timeAgo(r.str("updatedAt")) ?: ""}",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        val canEdit = status == RequestStatus.Pending && (manage || mine)
        val canDelete = manage || (mine && status == RequestStatus.Pending)
        if ((manage && (status == RequestStatus.Pending || status == RequestStatus.Failed)) || canEdit || canDelete) {
            FlowRow(
                Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (manage && status == RequestStatus.Pending) {
                    FilledTonalButton(enabled = !act.busy, onClick = {
                        act.run("Approved", onOk = { onChanged() }) { App.api.post("/request/${r.int("id")}/approve") }
                    }) { Icon(Icons.Outlined.Check, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Approve") }
                    OutlinedButton(enabled = !act.busy, onClick = {
                        act.run("Declined", onOk = { onChanged() }) { App.api.post("/request/${r.int("id")}/decline") }
                    }) { Icon(Icons.Outlined.Close, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Decline") }
                }
                if (manage && status == RequestStatus.Failed) {
                    FilledTonalButton(enabled = !act.busy, onClick = {
                        act.run("Retrying", onOk = { onChanged() }) { App.api.post("/request/${r.int("id")}/retry") }
                    }) { Icon(Icons.Outlined.Refresh, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Retry") }
                }
                if (canEdit && d != null) {
                    OutlinedButton({ edit = true }) { Icon(Icons.Outlined.Edit, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Edit") }
                }
                if (canDelete) {
                    TextButton({ confirmDelete = true }) {
                        Icon(Icons.Outlined.Delete, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp))
                        Text(if (mine && status == RequestStatus.Pending && !manage) "Cancel request" else "Delete")
                    }
                }
            }
        }
    }

    if (edit && d != null) {
        RequestDialog(type, d, is4k, r, onDismiss = { edit = false }, onDone = { edit = false; onChanged() })
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this request?") },
            text = { Text("The request is removed from Seerr. Anything already downloaded stays.") },
            confirmButton = {
                TextButton({
                    confirmDelete = false
                    act.run("Request deleted", onOk = { MediaCache.invalidate(type, tmdbId); onChanged() }) { App.api.delete("/request/${r.int("id")}") }
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton({ confirmDelete = false }) { Text("Keep") } },
        )
    }
}

/** Compact request row on a media page (no poster). */
@Composable
fun RequestInline(r: JSONObject, onChanged: () -> Unit, onEdit: () -> Unit) {
    val s = App.session
    val act = rememberActions()
    val status = RequestStatus.of(r.int("status"))
    val by = r.obj("requestedBy")
    val manage = s.can(Perm.MANAGE_REQUESTS)
    val mine = by.int("id") == s.userId
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(by.str("avatar"), by.str("displayName") ?: "?", 28.dp)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(by.str("displayName") ?: "Someone", fontWeight = FontWeight.SemiBold)
                    Text(timeAgo(r.str("createdAt")) ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (r.bool("is4k")) { Pill("4K", MaterialTheme.colorScheme.tertiary); Spacer(Modifier.width(6.dp)) }
                RequestPill(status)
            }
            val seasons = r.objs("seasons").map { it.int("seasonNumber") }.sorted()
            if (seasons.isNotEmpty()) Text("Seasons " + seasons.joinToString(", "), style = MaterialTheme.typography.bodySmall)
            if (status == RequestStatus.Pending && (manage || mine) || manage && status == RequestStatus.Failed) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (manage && status == RequestStatus.Pending) {
                        FilledTonalButton({ act.run("Approved", onOk = { onChanged() }) { App.api.post("/request/${r.int("id")}/approve") } }) { Text("Approve") }
                        OutlinedButton({ act.run("Declined", onOk = { onChanged() }) { App.api.post("/request/${r.int("id")}/decline") } }) { Text("Decline") }
                    }
                    if (manage && status == RequestStatus.Failed) {
                        FilledTonalButton({ act.run("Retrying", onOk = { onChanged() }) { App.api.post("/request/${r.int("id")}/retry") } }) { Text("Retry") }
                    }
                    if (status == RequestStatus.Pending) TextButton(onEdit) { Text("Edit") }
                }
            }
        }
    }
}

/** Small card for the "Recent requests" row on Discover. */
@Composable
fun RequestMiniCard(r: JSONObject) {
    val nav = LocalNav.current
    val d = rememberMediaFor(r)
    val media = r.obj("media")
    val type = media.str("mediaType") ?: "movie"
    val src = remember { MutableInteractionSource() }
    val by = r.obj("requestedBy")
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.width(280.dp).pressScale(src).clickable(src, null) { nav.openMedia(type, media.int("tmdbId")) },
    ) {
        Row(Modifier.padding(10.dp)) {
            Box(Modifier.width(56.dp).aspectRatio(2f / 3f).clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
                App.session.tmdb(d.str("posterPath"), "w154")?.let { AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(d.str("title") ?: d.str("name") ?: "…", fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(by.str("displayName") ?: "", style = MaterialTheme.typography.bodySmall, maxLines = 1)
                RequestPill(RequestStatus.of(r.int("status")))
            }
        }
    }
}

/** Pending requests can be swiped: right approves, left declines. Other cards don't move. */
@Composable
fun SwipeToDecide(
    r: JSONObject,
    act: Actions,
    onDecided: (Boolean) -> Unit,
    onFailed: () -> Unit,
    content: @Composable () -> Unit,
) {
    val canSwipe = App.session.can(Perm.MANAGE_REQUESTS) && RequestStatus.of(r.int("status")) == RequestStatus.Pending
    if (!canSwipe) { content(); return }
    val haptics = LocalHapticFeedback.current
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { v ->
            val approve = when (v) {
                SwipeToDismissBoxValue.StartToEnd -> true
                SwipeToDismissBoxValue.EndToStart -> false
                else -> return@rememberSwipeToDismissBoxState false
            }
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            act.run(if (approve) "Approved" else "Declined", onOk = { onDecided(approve) }) {
                try { App.api.post("/request/${r.int("id")}/${if (approve) "approve" else "decline"}") }
                catch (e: Exception) { onFailed(); throw e }
            }
            true
        },
        positionalThreshold = { it * 0.35f },
    )
    SwipeToDismissBox(
        state = state,
        backgroundContent = {
            val dir = state.dismissDirection
            val approve = dir == SwipeToDismissBoxValue.StartToEnd
            val color = when (dir) {
                SwipeToDismissBoxValue.StartToEnd -> Status.available
                SwipeToDismissBoxValue.EndToStart -> Status.declined
                else -> Color.Transparent
            }
            Box(
                Modifier.fillMaxSize().clip(CardDefaults.shape).background(color).padding(horizontal = 24.dp),
                contentAlignment = if (approve) Alignment.CenterStart else Alignment.CenterEnd,
            ) {
                if (dir != SwipeToDismissBoxValue.Settled) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (approve) Icons.Outlined.Check else Icons.Outlined.Close, null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text(if (approve) "Approve" else "Decline", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
    ) { content() }
}
