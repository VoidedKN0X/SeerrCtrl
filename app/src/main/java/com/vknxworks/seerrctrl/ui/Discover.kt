package com.vknxworks.seerrctrl.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.vknxworks.seerrctrl.App
import com.vknxworks.seerrctrl.data.MediaStatus
import com.vknxworks.seerrctrl.data.Perm
import androidx.compose.foundation.border
import androidx.compose.ui.res.pluralStringResource
import com.vknxworks.seerrctrl.notify.Notifier
import com.vknxworks.seerrctrl.ui.components.CtrlBottomBarClearance
import com.vknxworks.seerrctrl.ui.components.CtrlHeroCard
import com.vknxworks.seerrctrl.ui.theme.CtrlColors
import com.vknxworks.seerrctrl.ui.theme.CtrlShapes
import com.vknxworks.seerrctrl.ui.theme.CtrlType
import com.vknxworks.seerrctrl.data.Tile
import com.vknxworks.seerrctrl.data.arr
import com.vknxworks.seerrctrl.data.bool
import com.vknxworks.seerrctrl.data.int
import com.vknxworks.seerrctrl.data.intOrNull
import com.vknxworks.seerrctrl.data.obj
import com.vknxworks.seerrctrl.data.objects
import com.vknxworks.seerrctrl.data.objs
import com.vknxworks.seerrctrl.data.str
import com.vknxworks.seerrctrl.data.strings
import com.vknxworks.seerrctrl.data.toTile
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.json.JSONObject
import java.time.LocalDate
import androidx.compose.ui.res.stringResource
import com.vknxworks.seerrctrl.R

/** Movie/series details are fetched for many small cards (requests, recently added) — keep them around. */
object MediaCache {
    private val cache = object : android.util.LruCache<String, JSONObject>(300) {}

    suspend fun get(type: String, tmdbId: Int): JSONObject {
        val key = "$type/$tmdbId"
        cache.get(key)?.let { return it }
        val j = App.api.obj(if (type == "tv") "/tv/$tmdbId" else "/movie/$tmdbId")
        cache.put(key, j)
        return j
    }

    fun peek(type: String, tmdbId: Int): JSONObject? = cache.get("$type/$tmdbId")
    fun invalidate(type: String, tmdbId: Int) { cache.remove("$type/$tmdbId") }

    /** On sign-out, so the next account never sees this one's request and availability details. */
    fun clear() = cache.evictAll()

    suspend fun tile(type: String, tmdbId: Int): Tile? = try {
        get(type, tmdbId).also { if (!it.has("mediaType")) it.put("mediaType", type) }.toTile()
    } catch (_: Exception) { null }
}

/** Loads tiles for (type, tmdbId) pairs in parallel. */
suspend fun tilesFor(items: List<Pair<String, Int>>): List<Tile> = coroutineScope {
    items.map { (t, id) -> async { MediaCache.tile(t, id) } }.awaitAll().filterNotNull()
}

private data class Studio(val id: Int, val name: String, val logo: String)

/** TMDB image size for studio and network logos (white duotone), as Seerr's own web app uses. */
private const val LOGO_SIZE = "w780_filter(duotone,ffffff,bababa)"
private val studios = listOf(
    Studio(2, "Disney", "/wdrCwmRnLFJhEoH8GSfymY85KHT.png"), Studio(127928, "20th Century Studios", "/h0rjX5vjW5r8yEnUBStFarjcLT4.png"),
    Studio(34, "Sony Pictures", "/GagSvqWlyPdkFHMfQ3pNq6ix9P.png"), Studio(174, "Warner Bros. Pictures", "/ky0xOc5OrhzkZ1N6KyUxacfQsCk.png"),
    Studio(33, "Universal", "/8lvHyhjr8oUKOOy2dKXoALWKdp0.png"), Studio(4, "Paramount", "/fycMZt242LVjagMByZOLUGbCvv3.png"),
    Studio(3, "Pixar", "/1TjvGVDMYsj6JBxOAkUHpPEwLf7.png"), Studio(521, "Dreamworks", "/kP7t6RwGz2AvvTkvnI1uteEwHet.png"),
    Studio(420, "Marvel Studios", "/hUzeosd33nzE5MCNsZxCGEKTXaQ.png"), Studio(9993, "DC", "/2Tc1P3Ac8M479naPp1kYT3izLS5.png"),
    Studio(41077, "A24", "/1ZXsGaFPgrgS6ZZGS37AqD5uU12.png"),
)
private val networks = listOf(
    Studio(213, "Netflix", "/wwemzKWzjKYJFfCeiB57q3r4Bcm.png"), Studio(2739, "Disney+", "/gJ8VX6JSu3ciXHuC2dDGAo2lvwM.png"),
    Studio(1024, "Prime Video", "/ifhbNuuVnlwYy5oXA5VIb2YR8AZ.png"), Studio(2552, "Apple TV+", "/4KAy34EHvRM25Ih8wb82AuGU7zJ.png"),
    Studio(453, "Hulu", "/pqUTCleNUiTLAVlelGxUgWn1ELh.png"), Studio(49, "HBO", "/tuomPhY2UtuPTqqFnKMVHvSb724.png"),
    Studio(4353, "Discovery+", "/1D1bS3Dyw4ScYnFWTlBOvJXC3nb.png"), Studio(2, "ABC", "/ndAvF4JLsliGreX87jAc9GdjmJY.png"),
    Studio(19, "FOX", "/1DSpHrWyOORkL9N2QHX7Adt31mQ.png"), Studio(359, "Cinemax", "/6mSHSquNpfLgDdv6VnOOvC5Uz2h.png"),
    Studio(174, "AMC", "/pmvRmATOCaDykE6JrVoeYxlFHw3.png"), Studio(67, "Showtime", "/Allse9kbjiP6ExaQrnSpIhkurEi.png"),
    Studio(318, "Starz", "/8GJjw3HHsAJYwIWKIPBPfqMxlEa.png"), Studio(71, "The CW", "/ge9hzeaU7nMtQ4PjkFlc68dGAJ9.png"),
    Studio(6, "NBC", "/o3OedEP0f9mfZr33jz2BfXOUK5.png"), Studio(16, "CBS", "/nm8d7P7MJNiBLdgIzUK0gkuEA4r.png"),
    Studio(4330, "Paramount+", "/fi83B1oztoS47xxcemFdPMhIzK.png"), Studio(4, "BBC One", "/mVn7xESaTNmjBUyUtGNvDQd3CT1.png"),
    Studio(56, "Cartoon Network", "/c5OC6oVCg6QP4eqzW6XIq17CQjI.png"), Studio(80, "Adult Swim", "/9AKyspxVzywuaMuZ1Bvilu8sXly.png"),
    Studio(13, "Nickelodeon", "/ikZXxg6GnwpzqiZbRPhJGaZapqB.png"), Studio(3353, "Peacock", "/gIAcGTjKKr0KOHL5s4O36roJ8p7.png"),
)

/** One slider from the server's discover layout, already resolved to what it shows. */
private sealed interface SliderRow {
    val key: String
    data class Tiles(override val key: String, val title: String, val tiles: List<Tile>, val more: BrowseSpec?) : SliderRow
    data class Genres(override val key: String, val title: String, val tv: Boolean, val genres: List<JSONObject>) : SliderRow
    data class Logos(override val key: String, val title: String, val tv: Boolean, val items: List<Studio>) : SliderRow
    data class Requests(override val key: String, val requests: List<JSONObject>) : SliderRow
}

private fun filterAvailable(tiles: List<Tile>): List<Tile> {
    val pub = App.session.public
    return tiles.filter { t ->
        !(App.settings.hideAvailable || pub.bool("hideAvailable")) || (t.status != MediaStatus.Available && t.status != MediaStatus.Partial)
    }.filter { t -> !pub.bool("hideBlocklisted") || t.status != MediaStatus.Blocklisted }
}

suspend fun pageTiles(path: String, params: Map<String, Any?>, page: Int): Pair<List<Tile>, Int> {
    val j = App.api.obj(path, params + ("page" to page))
    val results = j.objs("results")
    val tiles = if (results.firstOrNull()?.has("tmdbId") == true && !results.first().has("posterPath")) {
        // Watchlist entries only carry ids.
        tilesFor(results.map { (it.str("mediaType") ?: "movie") to it.int("tmdbId") })
    } else results.mapNotNull { it.toTile() }
    return tiles to j.int("totalPages", 1)
}

private suspend fun buildRow(slider: JSONObject): SliderRow? {
    val type = slider.int("type")
    val id = slider.int("id")
    val title = slider.str("title")
    val data = slider.str("data").orEmpty()
    val today = LocalDate.now().toString()
    suspend fun tiles(t: String, path: String, params: Map<String, Any?> = emptyMap()): SliderRow {
        val (list, _) = pageTiles(path, params, 1)
        return SliderRow.Tiles("s$id", t, filterAvailable(list), BrowseSpec(t, path, params))
    }
    val region = data.substringBefore(',')
    val providers = data.substringAfter(',', "")
    return when (type) {
        1 -> { // recently added
            if (!App.session.can(Perm.MANAGE_REQUESTS, Perm.RECENT_VIEW, any = true)) return null
            val media = App.api.obj("/media", mapOf("filter" to "allavailable", "take" to 20, "sort" to "mediaAdded")).objs("results")
            SliderRow.Tiles("s$id", App.str(R.string.recently_added), tilesFor(media.map { (it.str("mediaType") ?: "movie") to it.int("tmdbId") }), null)
        }
        2 -> SliderRow.Requests("s$id", App.api.obj("/request", mapOf("filter" to "all", "take" to 10, "sort" to "added")).objs("results"))
        3 -> tiles(App.str(R.string.your_watchlist), "/discover/watchlist")
        4 -> tiles(App.str(R.string.trending), "/discover/trending")
        5 -> tiles(App.str(R.string.popular_movies), "/discover/movies")
        6 -> SliderRow.Genres("s$id", App.str(R.string.movie_genres), false, App.api.arr("/discover/genreslider/movie").objects())
        7 -> tiles(App.str(R.string.upcoming_movies), "/discover/movies", mapOf("primaryReleaseDateGte" to today))
        8 -> SliderRow.Logos("s$id", App.str(R.string.studios), false, studios)
        9 -> tiles(App.str(R.string.popular_series), "/discover/tv")
        10 -> SliderRow.Genres("s$id", App.str(R.string.series_genres), true, App.api.arr("/discover/genreslider/tv").objects())
        11 -> tiles(App.str(R.string.upcoming_series), "/discover/tv", mapOf("firstAirDateGte" to today))
        12 -> SliderRow.Logos("s$id", App.str(R.string.networks), true, networks)
        13 -> tiles(title ?: App.str(R.string.movies), "/discover/movies", mapOf("keywords" to data))
        14 -> tiles(title ?: App.str(R.string.movies), "/discover/movies", mapOf("genre" to data))
        15 -> tiles(title ?: App.str(R.string.series_2), "/discover/tv", mapOf("keywords" to data))
        16 -> tiles(title ?: App.str(R.string.series_2), "/discover/tv", mapOf("genre" to data))
        17 -> tiles(title ?: App.str(R.string.tab_search), "/search", mapOf("query" to data))
        18 -> tiles(title ?: App.str(R.string.studio), "/discover/movies/studio/$data")
        19 -> tiles(title ?: App.str(R.string.network), "/discover/tv/network/$data")
        20 -> tiles(title ?: App.str(R.string.streaming), "/discover/movies", mapOf("watchRegion" to region, "watchProviders" to providers))
        21 -> tiles(title ?: App.str(R.string.streaming), "/discover/tv", mapOf("watchRegion" to region, "watchProviders" to providers))
        else -> null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverTab() {
    val nav = LocalNav.current
    val rows = remember { mutableStateListOf<SliderRow>() }
    val load = rememberLoad("discover") {
        val sliders = App.api.arr("/settings/discover").objects().filter { it.bool("enabled", true) }
            .sortedBy { it.int("order") }
        val built = coroutineScope {
            sliders.map { s -> async { try { buildRow(s) } catch (_: Exception) { null } } }.awaitAll()
        }.filterNotNull()
        rows.clear(); rows.addAll(built)
        built
    }

    // The hero card: requests waiting for approval for managers, your own requests for everyone else.
    val s = App.session
    val manager = s.can(Perm.MANAGE_REQUESTS)
    val hero = rememberLoad("hero", s.me, Notifier.version) {
        if (s.me == null) null
        else if (manager) App.api.obj("/request/count").int("pending")
        else App.api.obj("/request", mapOf("take" to 1, "skip" to 0, "filter" to "all", "requestedBy" to s.userId)).obj("pageInfo").int("results")
    }

    PullToRefreshBox(isRefreshing = load.refreshing, onRefresh = { load.reload(silent = true); hero.reload(silent = true) }, modifier = Modifier.fillMaxSize()) {
        Loaded(load) {
            LazyColumn(contentPadding = ListBottom, modifier = Modifier.fillMaxSize()) {
                item { TabTitle(stringResource(R.string.discover)) }
                hero.value?.let { n ->
                    item(key = "hero") {
                        CtrlHeroCard(
                            label = stringResource(if (manager) R.string.hero_pending_label else R.string.hero_yours_label),
                            trailingLabel = s.appTitle,
                            value = "%02d".format(n),
                            onClick = { MainTab.selected = MainTab.REQUESTS },
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                        ) {
                            Text(
                                if (manager) pluralStringResource(R.plurals.hero_pending_line, n, n) else pluralStringResource(R.plurals.hero_yours_line, n, n),
                                style = CtrlType.Data, color = CtrlColors.OnAccent,
                            )
                        }
                    }
                }
                items(rows, key = { it.key }) { row ->
                    when (row) {
                        is SliderRow.Tiles -> TileRow(row.title, row.tiles, row.more?.let { { nav.browse(it) } })
                        is SliderRow.Genres -> GenreRow(row)
                        is SliderRow.Logos -> LogoRow(row)
                        is SliderRow.Requests -> RequestRow(row.requests)
                    }
                }
            }
        }
    }
}

@Composable
private fun GenreRow(row: SliderRow.Genres) {
    if (row.genres.isEmpty()) return
    val nav = LocalNav.current
    SectionHeader(row.title)
    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(row.genres, key = { it.int("id") }) { g ->
            val name = g.str("name") ?: ""
            val src = remember { MutableInteractionSource() }
            Box(
                Modifier.width(180.dp).height(96.dp).pressScale(src).clip(CtrlShapes.Poster)
                    .background(CtrlColors.Surface)
                    .border(1.dp, CtrlColors.Outline, CtrlShapes.Poster)
                    .clickable(src, null) {
                        nav.browse(BrowseSpec(name, if (row.tv) "/discover/tv" else "/discover/movies", mapOf("genre" to g.int("id"))))
                    },
                contentAlignment = Alignment.Center,
            ) {
                App.session.tmdb(g.arr("backdrops").strings().firstOrNull(), "w780")?.let {
                    AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    Box(Modifier.fillMaxSize().background(CtrlColors.Background.copy(alpha = 0.62f)))
                }
                Text(name, color = CtrlColors.Text, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun LogoRow(row: SliderRow.Logos) {
    val nav = LocalNav.current
    SectionHeader(row.title)
    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(row.items, key = { it.id }) { s ->
            val src = remember { MutableInteractionSource() }
            Box(
                Modifier.width(150.dp).height(80.dp).pressScale(src).clip(CtrlShapes.Poster)
                    .background(CtrlColors.Surface)
                    .border(1.dp, CtrlColors.Outline, CtrlShapes.Poster)
                    .clickable(src, null) {
                        nav.browse(BrowseSpec(s.name, if (row.tv) "/discover/tv/network/${s.id}" else "/discover/movies/studio/${s.id}"))
                    }
                    .padding(14.dp),
                contentAlignment = Alignment.Center,
            ) {
                // Through Seerr's image cache when it's on, like every other TMDB image.
                App.session.tmdb(s.logo, LOGO_SIZE)?.let { AsyncImage(it, s.name, Modifier.fillMaxSize(), contentScale = ContentScale.Fit) }
            }
        }
    }
}

@Composable
private fun RequestRow(requests: List<JSONObject>) {
    if (requests.isEmpty()) return
    val nav = LocalNav.current
    SectionHeader(stringResource(R.string.recent_requests), onMore = { MainTab.selected = MainTab.REQUESTS })
    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(requests, key = { it.int("id") }) { r -> RequestMiniCard(r) }
    }
}

/** Paged grid for any discover-style endpoint ("See all", genres, studios, keywords…). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseScreen(key: String) {
    val nav = LocalNav.current
    val spec = remember(key) { Browse.get(key) }
    if (spec == null) {
        // The list behind this screen is gone (the app was restarted): just close it.
        val closeScreen = rememberCloseScreen()
        LaunchedEffect(Unit) { closeScreen() }
        return
    }
    var mediaFilter by remember { mutableStateOf(spec.params) }
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val canSort = spec.path == "/discover/movies" || spec.path == "/discover/tv"
    var showFilters by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            BackBar(spec.title, { nav.back() }, scroll = scroll) {
                if (canSort) IconButton({ showFilters = true }) { Icon(Icons.Outlined.Tune, stringResource(R.string.filters)) }
            }
        },
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
    ) { pad ->
        PagedTileGrid(spec.path, mediaFilter, Modifier.padding(pad))
    }
    if (showFilters) {
        DiscoverFilterSheet(tv = spec.path.contains("/tv"), current = mediaFilter, onDismiss = { showFilters = false }) {
            mediaFilter = it; showFilters = false
        }
    }
}

@Composable
fun PagedTileGrid(path: String, params: Map<String, Any?>, modifier: Modifier = Modifier, header: (@Composable () -> Unit)? = null) {
    val nav = LocalNav.current
    val tiles = remember(path, params) { mutableStateListOf<Tile>() }
    var page by remember(path, params) { mutableIntStateOf(0) }
    var total by remember(path, params) { mutableIntStateOf(1) }
    var loading by remember(path, params) { mutableStateOf(false) }
    var error by remember(path, params) { mutableStateOf<String?>(null) }
    var retry by remember(path, params) { mutableIntStateOf(0) }
    val state = rememberLazyGridState()

    suspend fun next() {
        if (loading || page >= total) return
        loading = true; error = null
        try {
            val (list, tp) = pageTiles(path, params, page + 1)
            val seen = tiles.map { it.type + it.id }.toHashSet()
            tiles.addAll(list.filter { seen.add(it.type + it.id) })
            total = tp; page++
        } catch (e: Exception) { error = e.message }
        loading = false
    }

    LaunchedEffect(path, params, retry) { next() }
    val nearEnd by remember {
        derivedStateOf {
            val last = state.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= state.layoutInfo.totalItemsCount - 8
        }
    }
    LaunchedEffect(nearEnd, page) { if (nearEnd && tiles.isNotEmpty()) next() }

    if (tiles.isEmpty() && error != null) { ErrorBox(error!!, modifier) { error = null; retry++ }; return }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(posterWidth()),
        state = state,
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = CtrlBottomBarClearance),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxSize(),
    ) {
        if (header != null) item(span = { GridItemSpan(maxLineSpan) }) { header() }
        items(tiles, key = { it.type + it.id }) { t -> PosterCard(t, null, { nav.openTile(t) }) }
        if (loading) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            }
        }
        if (!loading && tiles.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) { EmptyBox(Icons.Outlined.Movie, stringResource(R.string.nothing_here)) }
        }
    }
}
