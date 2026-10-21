package com.vknxworks.seerrctrl.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.vknxworks.seerrctrl.App
import com.vknxworks.seerrctrl.data.MediaStatus
import com.vknxworks.seerrctrl.data.Perm
import com.vknxworks.seerrctrl.data.RequestStatus
import com.vknxworks.seerrctrl.data.Tile
import com.vknxworks.seerrctrl.data.arr
import com.vknxworks.seerrctrl.data.bool
import com.vknxworks.seerrctrl.data.dbl
import com.vknxworks.seerrctrl.data.humanBytes
import com.vknxworks.seerrctrl.data.int
import com.vknxworks.seerrctrl.data.intOrNull
import com.vknxworks.seerrctrl.data.jsonOf
import com.vknxworks.seerrctrl.data.long
import com.vknxworks.seerrctrl.data.obj
import com.vknxworks.seerrctrl.data.objects
import com.vknxworks.seerrctrl.data.objs
import com.vknxworks.seerrctrl.data.prettyDate
import com.vknxworks.seerrctrl.data.runtime
import com.vknxworks.seerrctrl.data.str
import com.vknxworks.seerrctrl.data.toTile
import com.vknxworks.seerrctrl.data.year
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.json.JSONObject
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import com.vknxworks.seerrctrl.R

private class MediaPage(
    val d: JSONObject,
    val ratings: JSONObject?,
    val recommended: List<Tile>,
    val similar: List<Tile>,
)

/** Movie or series details with everything the web page offers. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MediaScreen(type: String, tmdbId: Int) {
    val nav = LocalNav.current
    val context = LocalContext.current
    val s = App.session
    val act = rememberActions()
    val tv = type == "tv"
    val load = rememberLoad(type, tmdbId) {
        coroutineScope {
            val base = if (tv) "/tv/$tmdbId" else "/movie/$tmdbId"
            val ratings = async { try { App.api.obj(if (tv) "$base/ratings" else "$base/ratingscombined") } catch (_: Exception) { null } }
            val rec = async { try { App.api.obj("$base/recommendations").objs("results").mapNotNull { it.toTile() } } catch (_: Exception) { emptyList() } }
            val sim = async { try { App.api.obj("$base/similar").objs("results").mapNotNull { it.toTile() } } catch (_: Exception) { emptyList() } }
            MediaCache.invalidate(type, tmdbId)
            val d = MediaCache.get(type, tmdbId)
            MediaPage(d, ratings.await(), rec.await(), sim.await())
        }
    }
    var requestDialog by remember { mutableStateOf<Boolean?>(null) } // value = is4k
    var editRequest by remember { mutableStateOf<JSONObject?>(null) }
    var issueDialog by remember { mutableStateOf(false) }
    var manage by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        PullToRefreshBox(load.refreshing, { load.reload(silent = true) }, Modifier.fillMaxSize()) {
            Loaded(load, Modifier.statusBarsPadding()) { page ->
                val d = page.d
                val info = d.obj("mediaInfo")
                val title = d.str("title") ?: d.str("name") ?: ""
                val status = MediaStatus.of(info.intOrNull("status"))
                val status4k = MediaStatus.of(info.intOrNull("status4k"))
                val requests = info.objs("requests")

                LazyColumn(contentPadding = PaddingValues(bottom = 128.dp)) {
                    item {
                        Box {
                            Backdrop(d.str("backdropPath"), 300.dp)
                            Row(
                                Modifier.padding(start = 16.dp, end = 16.dp, top = 170.dp),
                                verticalAlignment = Alignment.Bottom,
                            ) {
                                Box(
                                    Modifier.width(120.dp).aspectRatio(2f / 3f).clip(MaterialTheme.shapes.medium)
                                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                ) {
                                    s.tmdb(d.str("posterPath"), "w342")?.let { AsyncImage(it, title, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
                                }
                                Spacer(Modifier.width(14.dp))
                                Column(Modifier.weight(1f)) {
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        MediaStatusPill(status)
                                        if (s.movie4k && !tv || s.series4k && tv) MediaStatusPill(status4k, is4k = true)
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                    val facts = listOfNotNull(
                                        year(d.str("releaseDate") ?: d.str("firstAirDate")),
                                        if (tv) d.int("numberOfSeasons").takeIf { it > 0 }?.let { App.plural(R.plurals.seasons_count, it, it) }
                                        else runtime(d.int("runtime")),
                                        d.objs("genres").take(3).mapNotNull { it.str("name") }.joinToString(", ").ifEmpty { null },
                                    )
                                    Text(facts.joinToString(" · "), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    // ----- actions -----
                    item {
                        ActionRow(
                            d = d, tv = tv, status = status, status4k = status4k,
                            onRequest = { requestDialog = it },
                            onIssue = { issueDialog = true },
                            onManage = { manage = true },
                            onTrailer = { url -> openUrl(context, url) },
                            onWatchlist = {
                                act.run(App.str(R.string.added_to_watchlist), onOk = { load.reload(silent = true) }) {
                                    App.api.post("/watchlist", jsonOf("tmdbId" to tmdbId, "mediaType" to type, "title" to title))
                                }
                            },
                            onWatchlistRemove = {
                                act.run(App.str(R.string.removed_from_watchlist), onOk = { load.reload(silent = true) }) {
                                    App.api.delete("/watchlist/$tmdbId", query = mapOf("mediaType" to type))
                                }
                            },
                            onPlay = { url -> openUrl(context, url) },
                        )
                    }

                    d.str("tagline")?.let { tag ->
                        item {
                            Text(
                                tag, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            )
                        }
                    }
                    item {
                        Text(
                            d.str("overview") ?: stringResource(R.string.no_overview_available),
                            style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        )
                    }

                    // ----- downloads -----
                    val downloads = info.objs("downloadStatus") + info.objs("downloadStatus4k")
                    if (downloads.isNotEmpty()) {
                        item { SectionHeader(stringResource(R.string.downloading)) }
                        items(downloads) { dl -> DownloadRow(dl) }
                    }

                    // ----- requests on this title -----
                    if (requests.isNotEmpty() && s.can(Perm.MANAGE_REQUESTS, Perm.REQUEST_VIEW, any = true) || requests.any { it.obj("requestedBy").int("id") == s.userId }) {
                        val visible = if (s.can(Perm.MANAGE_REQUESTS, Perm.REQUEST_VIEW, any = true)) requests else requests.filter { it.obj("requestedBy").int("id") == s.userId }
                        if (visible.isNotEmpty()) {
                            item { SectionHeader(stringResource(R.string.requests)) }
                            items(visible, key = { "r" + it.int("id") }) { r ->
                                RequestInline(r, onChanged = { load.reload(silent = true) }, onEdit = { editRequest = r })
                            }
                        }
                    }

                    // ----- ratings & facts -----
                    item { RatingsRow(d, page.ratings) }
                    item { Facts(d, tv) }

                    // ----- where to watch -----
                    val region = s.streamingRegion
                    val providers = d.objs("watchProviders").firstOrNull { it.str("iso_3166_1") == region }
                    val streams = providers.objs("flatrate")
                    if (streams.isNotEmpty()) {
                        item { SectionHeader(stringResource(R.string.streaming_in, region)) }
                        item {
                            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(streams) { p ->
                                    AsyncImage(
                                        s.tmdb(p.str("logoPath"), "w92"), p.str("name"),
                                        Modifier.size(48.dp).clip(MaterialTheme.shapes.small)
                                            .clickable { providers.str("link")?.let { openUrl(context, it) } },
                                    )
                                }
                            }
                        }
                    }

                    // ----- seasons -----
                    if (tv) {
                        val seasons = d.objs("seasons").filter { s.specialEpisodes || it.int("seasonNumber") > 0 }
                        if (seasons.isNotEmpty()) {
                            item { SectionHeader(stringResource(R.string.seasons)) }
                            items(seasons, key = { "s" + it.int("seasonNumber") }) { season ->
                                SeasonRow(tmdbId, season, info.objs("seasons").firstOrNull { it.int("seasonNumber") == season.int("seasonNumber") })
                            }
                        }
                    }

                    // ----- collection -----
                    d.obj("collection")?.let { c ->
                        item { CollectionCard(c) }
                    }

                    // ----- cast & crew -----
                    val cast = d.obj("credits").objs("cast").distinctBy { it.int("id") }.take(30).mapNotNull { it.put("mediaType", "person").toTile() }
                    item { TileRow(stringResource(R.string.cast), cast) }
                    val crew = d.obj("credits").objs("crew")
                        .filter { it.str("job") in setOf("Director", "Screenplay", "Writer", "Producer", "Original Music Composer", "Director of Photography", "Novel", "Creator") }
                        .distinctBy { it.int("id") }.take(20)
                        .mapNotNull { it.put("mediaType", "person").toTile() }
                    item { TileRow(stringResource(R.string.crew), crew) }
                    if (tv) {
                        val creators = d.objs("createdBy").distinctBy { it.int("id") }.mapNotNull { it.put("mediaType", "person").put("job", App.str(R.string.creator)).toTile() }
                        item { TileRow(stringResource(R.string.created_by), creators) }
                    }

                    // ----- issues -----
                    val issues = info.objs("issues")
                    if (issues.isNotEmpty() && s.can(Perm.MANAGE_ISSUES, Perm.VIEW_ISSUES, any = true)) {
                        item { SectionHeader(stringResource(R.string.issues)) }
                        items(issues, key = { "i" + it.int("id") }) { iss -> IssueMini(iss) }
                    }

                    // ----- keywords -----
                    val keywords = d.objs("keywords")
                    if (keywords.isNotEmpty()) {
                        item { SectionHeader(stringResource(R.string.keywords)) }
                        item {
                            FlowRow(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                keywords.forEach { k ->
                                    AssistChip(
                                        onClick = {
                                            nav.browse(BrowseSpec(k.str("name") ?: "", if (tv) "/discover/tv" else "/discover/movies", mapOf("keywords" to k.int("id"))))
                                        },
                                        label = { Text(k.str("name") ?: "") },
                                    )
                                }
                            }
                        }
                    }

                    item { TileRow(stringResource(R.string.recommendations_2), page.recommended, onMore = { nav.browse(BrowseSpec(App.str(R.string.recommendations, title), "/$type/$tmdbId/recommendations")) }) }
                    item { TileRow(stringResource(R.string.similar), page.similar, onMore = { nav.browse(BrowseSpec(App.str(R.string.similar_to, title), "/$type/$tmdbId/similar")) }) }
                }

                if (requestDialog != null) {
                    RequestDialog(
                        type = type, details = d, is4k = requestDialog == true, existing = null,
                        onDismiss = { requestDialog = null },
                        onDone = { requestDialog = null; load.reload(silent = true) },
                    )
                }
                editRequest?.let { r ->
                    RequestDialog(
                        type = type, details = d, is4k = r.bool("is4k"), existing = r,
                        onDismiss = { editRequest = null },
                        onDone = { editRequest = null; load.reload(silent = true) },
                    )
                }
                if (issueDialog) {
                    ReportIssueDialog(d, tv, onDismiss = { issueDialog = false }) { id ->
                        issueDialog = false
                        nav.go("issue/$id")
                    }
                }
                if (manage) {
                    ManageMediaSheet(d, type, onDismiss = { manage = false }, onChanged = { load.reload(silent = true) })
                }
            }
        }
        FilledIconButton(
            onClick = { nav.back() },
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.Black.copy(alpha = 0.45f), contentColor = Color.White),
            modifier = Modifier.statusBarsPadding().padding(8.dp),
        ) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.back)) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActionRow(
    d: JSONObject,
    tv: Boolean,
    status: MediaStatus,
    status4k: MediaStatus,
    onRequest: (Boolean) -> Unit,
    onIssue: () -> Unit,
    onManage: () -> Unit,
    onTrailer: (String) -> Unit,
    onWatchlist: () -> Unit,
    onWatchlistRemove: () -> Unit,
    onPlay: (String) -> Unit,
) {
    val s = App.session
    val info = d.obj("mediaInfo")
    val canRequest = s.can(Perm.REQUEST, if (tv) Perm.REQUEST_TV else Perm.REQUEST_MOVIE, any = true)
    val can4k = (if (tv) s.series4k else s.movie4k) && s.can(Perm.REQUEST_4K, if (tv) Perm.REQUEST_4K_TV else Perm.REQUEST_4K_MOVIE, any = true)
    val requestable = { st: MediaStatus ->
        st == MediaStatus.Unknown || st == MediaStatus.Deleted || (tv && st == MediaStatus.Partial) ||
            (tv && st == MediaStatus.Processing && s.partialRequests)
    }
    val trailer = d.objs("relatedVideos").firstOrNull { it.str("type") == "Trailer" } ?: d.objs("relatedVideos").firstOrNull()
    val playUrl = info.str("mediaUrl")
    val play4k = info.str("mediaUrl4k")
    var more by remember { mutableStateOf(false) }

    FlowRow(
        Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (playUrl != null && (status == MediaStatus.Available || status == MediaStatus.Partial)) {
            Button({ onPlay(playUrl) }) {
                Icon(Icons.Outlined.PlayArrow, null); Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.play_on, s.mediaServer.label))
            }
        }
        if (play4k != null && (status4k == MediaStatus.Available || status4k == MediaStatus.Partial)) {
            FilledTonalButton({ onPlay(play4k) }) { Icon(Icons.Outlined.PlayArrow, null); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.play_4k)) }
        }
        if (status == MediaStatus.Blocklisted) {
            OutlinedButton({}, enabled = false) { Icon(Icons.Outlined.Block, null); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.blocklisted)) }
        } else {
            if (canRequest && requestable(status)) {
                Button({ onRequest(false) }) {
                    Icon(Icons.Outlined.Add, null); Spacer(Modifier.width(6.dp))
                    Text(if (tv && status != MediaStatus.Unknown && status != MediaStatus.Deleted) stringResource(R.string.request_more) else stringResource(R.string.request))
                }
            }
            if (can4k && requestable(status4k)) {
                FilledTonalButton({ onRequest(true) }) { Icon(Icons.Outlined.Add, null); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.request_4k)) }
            }
        }
        trailer?.str("url")?.let { url ->
            OutlinedButton({ onTrailer(url) }) { Icon(Icons.Outlined.PlayArrow, null); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.trailer)) }
        }
        val onList = d.bool("onUserWatchlist")
        IconButton({ if (onList) onWatchlistRemove() else onWatchlist() }) {
            Icon(if (onList) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder, if (onList) stringResource(R.string.remove_from_watchlist) else stringResource(R.string.add_to_watchlist))
        }
        if (info != null && s.can(Perm.CREATE_ISSUES, Perm.MANAGE_ISSUES, any = true) &&
            (status == MediaStatus.Available || status == MediaStatus.Partial || status4k == MediaStatus.Available || status4k == MediaStatus.Partial)
        ) {
            IconButton(onIssue) { Icon(Icons.Outlined.ReportProblem, stringResource(R.string.report_an_issue)) }
        }
        if (s.can(Perm.MANAGE_REQUESTS, Perm.MANAGE_BLOCKLIST, any = true)) {
            IconButton(onManage) { Icon(Icons.Outlined.Build, stringResource(R.string.manage)) }
        }
    }
}

@Composable
private fun DownloadRow(dl: JSONObject) {
    val size = dl.long("size")
    val left = dl.long("sizeLeft")
    val progress = if (size > 0) (1f - left.toFloat() / size).coerceIn(0f, 1f) else 0f
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Text(dl.str("title") ?: stringResource(R.string.download), style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator({ progress }, Modifier.fillMaxWidth())
        Text(
            listOfNotNull(
                "${(progress * 100).toInt()}%",
                if (size > 0) App.str(R.string.of, humanBytes(size - left), humanBytes(size)) else null,
                dl.str("status")?.replaceFirstChar { it.uppercase() },
                dl.str("estimatedCompletionTime")?.let { App.str(R.string.eta, com.vknxworks.seerrctrl.data.prettyDateTime(it) ?: "") },
            ).joinToString(" · "),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RatingsRow(d: JSONObject, ratings: JSONObject?) {
    val rt = ratings.obj("rt") ?: ratings?.takeIf { it.has("criticsScore") && !it.has("imdb") }
    val imdb = ratings.obj("imdb")
    val tmdb = d.dbl("voteAverage")
    val context = LocalContext.current
    Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (tmdb != null && tmdb > 0) RatingChip(stringResource(R.string.tmdb), "${(tmdb * 10).toInt()}%") {}
        rt?.intOrNull("criticsScore")?.let { sc -> RatingChip(stringResource(R.string.critics), "$sc%") { rt.str("url")?.let { openUrl(context, it) } } }
        rt?.intOrNull("audienceScore")?.let { sc -> RatingChip(stringResource(R.string.audience), "$sc%") { rt.str("url")?.let { openUrl(context, it) } } }
        imdb?.dbl("criticsScore")?.let { sc -> RatingChip("IMDb", "%.1f".format(sc)) { imdb.str("url")?.let { openUrl(context, it) } } }
    }
}

@Composable
private fun RatingChip(label: String, value: String, onClick: () -> Unit) {
    Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceContainerHigh, onClick = onClick) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Star, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(4.dp))
                Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun Facts(d: JSONObject, tv: Boolean) {
    val rows = buildList {
        d.str("status")?.let { add(App.str(R.string.status) to it) }
        if (tv) {
            prettyDate(d.str("firstAirDate"))?.let { add(App.str(R.string.first_aired) to it) }
            prettyDate(d.str("lastAirDate"))?.let { add(App.str(R.string.last_aired) to it) }
            d.obj("nextEpisodeToAir")?.let { n -> add(App.str(R.string.next_episode) to App.str(R.string.s_e, n.int("seasonNumber"), n.int("episodeNumber"), prettyDate(n.str("airDate")) ?: "TBA")) }
            d.int("numberOfEpisodes").takeIf { it > 0 }?.let { add(App.str(R.string.episodes) to "$it") }
            d.objs("networks").mapNotNull { it.str("name") }.joinToString(", ").ifEmpty { null }?.let { add(App.str(R.string.network) to it) }
            d.arr("episodeRunTime")?.optInt(0)?.takeIf { it > 0 }?.let { add(App.str(R.string.episode_runtime) to App.str(R.string.min, it)) }
        } else {
            prettyDate(d.str("releaseDate"))?.let { add(App.str(R.string.release_date) to it) }
            d.long("budget").takeIf { it > 0 }?.let { add(App.str(R.string.budget) to "$" + "%,d".format(it)) }
            d.long("revenue").takeIf { it > 0 }?.let { add(App.str(R.string.revenue) to "$" + "%,d".format(it)) }
        }
        d.str("originalLanguage")?.let { add(App.str(R.string.original_language) to java.util.Locale.forLanguageTag(it).displayLanguage.ifEmpty { it }) }
        d.objs("productionCountries").mapNotNull { it.str("name") }.joinToString(", ").ifEmpty { null }?.let { add(App.str(R.string.countries) to it) }
        d.objs("productionCompanies").take(4).mapNotNull { it.str("name") }.joinToString(", ").ifEmpty { null }?.let { add(App.str(R.string.studios) to it) }
    }
    if (rows.isEmpty()) return
    Card(
        Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        rows.forEachIndexed { i, (k, v) ->
            if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Text(k, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(0.45f))
                Text(v, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(0.55f))
            }
        }
    }
}

@Composable
private fun SeasonRow(tvId: Int, season: JSONObject, info: JSONObject?) {
    var open by remember { mutableStateOf(false) }
    val n = season.int("seasonNumber")
    val st = MediaStatus.of(info.intOrNull("status"))
    val st4k = MediaStatus.of(info.intOrNull("status4k"))
    Column(Modifier.fillMaxWidth()) {
        ListItem(
            headlineContent = { Text(season.str("name") ?: stringResource(R.string.season_2, n), fontWeight = FontWeight.SemiBold) },
            supportingContent = {
                Text(listOfNotNull(pluralStringResource(R.plurals.episodes_count, season.int("episodeCount"), season.int("episodeCount")), year(season.str("airDate"))).joinToString(" · "))
            },
            trailingContent = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MediaStatusPill(st)
                    if (App.session.series4k) MediaStatusPill(st4k, is4k = true)
                    Icon(if (open) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null)
                }
            },
            modifier = Modifier.clickable { open = !open },
        )
        AnimatedVisibility(open) { Episodes(tvId, n) }
    }
}

@Composable
private fun Episodes(tvId: Int, season: Int) {
    val load = rememberLoad(tvId, season) { App.api.obj("/tv/$tvId/season/$season").objs("episodes") }
    Loaded(load, Modifier.height(80.dp)) { eps ->
        Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            eps.forEach { e ->
                Row {
                    Box(Modifier.width(120.dp).aspectRatio(16f / 9f).clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
                        App.session.tmdb(e.str("stillPath"), "w300")?.let { AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("${e.int("episodeNumber")}. ${e.str("name") ?: ""}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        prettyDate(e.str("airDate"))?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        e.str("overview")?.let { Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis) }
                    }
                }
            }
        }
    }
}

@Composable
private fun CollectionCard(c: JSONObject) {
    val nav = LocalNav.current
    SectionHeader(stringResource(R.string.part_of_a_collection))
    Box(
        Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(110.dp).clip(MaterialTheme.shapes.large)
            .clickable { nav.go("collection/${c.int("id")}") }
    ) {
        App.session.tmdb(c.str("backdropPath"), "w780")?.let { AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)))
        Column(Modifier.align(Alignment.CenterStart).padding(20.dp)) {
            Text(c.str("name") ?: stringResource(R.string.collection), color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.view_collection), color = Color.White.copy(alpha = 0.85f))
        }
    }
}

@Composable
private fun IssueMini(iss: JSONObject) {
    val nav = LocalNav.current
    ListItem(
        leadingContent = { Icon(Icons.Outlined.ReportProblem, null, tint = if (iss.int("status") == 1) Status.pending else Status.available) },
        headlineContent = { Text(stringResource(R.string.issue_title, com.vknxworks.seerrctrl.data.IssueTypes.name(iss.int("issueType")))) },
        supportingContent = { Text(if (iss.int("status") == 1) stringResource(R.string.open) else stringResource(R.string.resolved)) },
        modifier = Modifier.clickable { nav.go("issue/${iss.int("id")}") },
    )
}
