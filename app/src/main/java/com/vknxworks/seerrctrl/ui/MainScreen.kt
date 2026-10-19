package com.vknxworks.seerrctrl.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.ManageAccounts
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material.icons.outlined.Theaters
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.foundation.layout.navigationBarsPadding
import com.vknxworks.seerrctrl.notify.Notifier
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.vknxworks.seerrctrl.App
import com.vknxworks.seerrctrl.data.Perm
import com.vknxworks.seerrctrl.data.UserTypes
import com.vknxworks.seerrctrl.data.int
import com.vknxworks.seerrctrl.data.objs
import com.vknxworks.seerrctrl.data.str
import kotlinx.coroutines.delay
import org.json.JSONObject

/** Which tab is showing; other screens can switch it (e.g. "See all requests"). */
object MainTab {
    const val DISCOVER = 0
    const val SEARCH = 1
    const val REQUESTS = 2
    const val ISSUES = 3
    const val MORE = 4
    var selected by mutableIntStateOf(DISCOVER)
    /** Wide window: tabs go in a side rail instead of the bottom bar. */
    var rail by mutableStateOf(false)
}

private data class TabDef(val index: Int, val icon: androidx.compose.ui.graphics.vector.ImageVector, val label: String, val badge: Int = 0)

@Composable
fun MainScreen() {
    val s = App.session
    val showIssues = s.can(Perm.MANAGE_ISSUES, Perm.VIEW_ISSUES, Perm.CREATE_ISSUES, any = true)
    val pending = rememberLoad(s.me, Notifier.version) {
        if (s.can(Perm.MANAGE_REQUESTS)) App.api.obj("/request/count").int("pending") else 0
    }
    val openIssues = rememberLoad(s.me, "issues", Notifier.version) {
        if (s.can(Perm.MANAGE_ISSUES)) App.api.obj("/issue/count").int("open") else 0
    }
    val tabs = buildList {
        add(TabDef(MainTab.DISCOVER, Icons.Outlined.Explore, "Discover"))
        add(TabDef(MainTab.SEARCH, Icons.Outlined.Search, "Search"))
        add(TabDef(MainTab.REQUESTS, Icons.Outlined.Theaters, "Requests", pending.value ?: 0))
        if (showIssues) add(TabDef(MainTab.ISSUES, Icons.Outlined.ReportProblem, "Issues", openIssues.value ?: 0))
        add(TabDef(MainTab.MORE, Icons.Outlined.MoreHoriz, "More"))
    }

    BackHandler(MainTab.selected != MainTab.DISCOVER) { MainTab.selected = MainTab.DISCOVER }

    // Usage stats (only if the user opted in): which tab, by a fixed name.
    LaunchedEffect(MainTab.selected) {
        val tab = when (MainTab.selected) {
            MainTab.SEARCH -> "search"
            MainTab.REQUESTS -> "requests"
            MainTab.ISSUES -> "issues"
            MainTab.MORE -> "more"
            else -> "discover"
        }
        App.telemetry.track("screen_viewed", mapOf("screen" to "tab_$tab"))
    }

    val content: @Composable () -> Unit = {
        AnimatedContent(MainTab.selected, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "tab") { t ->
            when (t) {
                MainTab.SEARCH -> SearchTab()
                MainTab.REQUESTS -> RequestsTab(onChanged = { pending.reload(silent = true) })
                MainTab.ISSUES -> IssuesTab()
                MainTab.MORE -> MoreTab()
                else -> DiscoverTab()
            }
        }
    }

    if (MainTab.rail) {
        Row(Modifier.fillMaxSize()) {
            NavigationRail(Modifier.statusBarsPadding()) {
                Spacer(Modifier.height(8.dp))
                tabs.forEach { t ->
                    NavigationRailItem(
                        selected = MainTab.selected == t.index,
                        onClick = { MainTab.selected = t.index },
                        icon = { TabIcon(t) },
                        label = { Text(t.label, maxLines = 1) },
                    )
                }
            }
            Box(Modifier.weight(1f).fillMaxSize().statusBarsPadding().navigationBarsPadding()) { content() }
        }
    } else {
        // Content runs edge to edge under the floating island, which blurs it.
        val haze = remember { HazeState() }
        Box(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().hazeSource(haze).statusBarsPadding()) { content() }
            TabIsland(
                tabs = tabs.map { IslandTab(it.index, it.icon, it.label, it.badge) },
                selected = MainTab.selected,
                hazeState = haze,
                onSelect = { MainTab.selected = it },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun TabIcon(t: TabDef) {
    BadgedBox(badge = { if (t.badge > 0) Badge { Text(if (t.badge > 99) "99+" else "${t.badge}") } }) { Icon(t.icon, null) }
}

// ---------------- search ----------------

private enum class SearchKind(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    All("All", Icons.Outlined.Search), Keyword("Keywords", Icons.Outlined.Tag), Company("Studios", Icons.Outlined.WorkOutline)
}

@Composable
fun SearchTab() {
    val nav = LocalNav.current
    var query by rememberSaveable { mutableStateOf("") }
    var debounced by rememberSaveable { mutableStateOf("") }
    var kind by rememberSaveable { mutableStateOf(SearchKind.All) }
    LaunchedEffect(query) { delay(400); debounced = query.trim() }
    // Usage stats: only that a search ran and which kind (all, keywords, studios), never what was typed.
    LaunchedEffect(debounced, kind) {
        if (debounced.isNotEmpty()) App.telemetry.track("search_used", mapOf("kind" to kind.name.lowercase()))
    }

    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query, onValueChange = { query = it },
            placeholder = { Text("Search movies, series and people") },
            leadingIcon = { Icon(Icons.Outlined.Search, null) },
            trailingIcon = { if (query.isNotEmpty()) IconButton({ query = "" }) { Icon(Icons.Outlined.Close, "Clear") } },
            singleLine = true, shape = MaterialTheme.shapes.extraLarge,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { debounced = query.trim() }),
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp),
        )
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SearchKind.entries.forEach { k -> FilterChip(kind == k, { kind = k }, label = { Text(k.label, maxLines = 1) }) }
        }
        if (debounced.isEmpty()) {
            SearchShortcuts()
            return
        }
        when (kind) {
            SearchKind.All -> PagedTileGrid("/search", mapOf("query" to debounced))
            SearchKind.Keyword, SearchKind.Company -> {
                val load = rememberLoad(debounced, kind) {
                    App.api.obj(if (kind == SearchKind.Keyword) "/search/keyword" else "/search/company", mapOf("query" to debounced)).objs("results")
                }
                Loaded(load) { list ->
                    LazyColumn(contentPadding = ListBottom) {
                        items(list, key = { it.int("id") }) { r ->
                            val name = r.str("name") ?: ""
                            ListItem(
                                leadingContent = { Icon(kind.icon, null) },
                                headlineContent = { Text(name) },
                                supportingContent = r.str("origin_country")?.let { { Text(it) } },
                                modifier = Modifier.clickableRow {
                                    if (kind == SearchKind.Keyword) nav.browse(BrowseSpec(name, "/discover/movies", mapOf("keywords" to r.int("id"))))
                                    else nav.browse(BrowseSpec(name, "/discover/movies/studio/${r.int("id")}"))
                                },
                            )
                        }
                        if (list.isEmpty()) item { EmptyBox(Icons.Outlined.Search, "No matches") }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchShortcuts() {
    val nav = LocalNav.current
    LazyColumn(contentPadding = ListBottom) {
        item { SectionHeader("Browse") }
        item {
            NavItem(Icons.Outlined.Theaters, "Movies", "Popular movies, with filters") {
                nav.browse(BrowseSpec("Movies", "/discover/movies"))
            }
        }
        item {
            NavItem(Icons.Outlined.Tv, "Series", "Popular series, with filters") {
                nav.browse(BrowseSpec("Series", "/discover/tv"))
            }
        }
        item { NavItem(Icons.Outlined.Explore, "Trending", null) { nav.browse(BrowseSpec("Trending", "/discover/trending")) } }
        item {
            NavItem(Icons.Outlined.Theaters, "Upcoming movies", null) {
                nav.browse(BrowseSpec("Upcoming movies", "/discover/movies/upcoming"))
            }
        }
        item {
            NavItem(Icons.Outlined.Tv, "Upcoming series", null) {
                nav.browse(BrowseSpec("Upcoming series", "/discover/tv/upcoming"))
            }
        }
    }
}

fun Modifier.clickableRow(onClick: () -> Unit) = this.clickable(onClick = onClick)

// ---------------- more ----------------

@Composable
fun MoreTab() {
    val nav = LocalNav.current
    val s = App.session
    val me = s.me
    LazyColumn(contentPadding = ListBottom) {
        item {
            Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Avatar(me.str("avatar"), s.displayName, 56.dp)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(s.displayName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        listOfNotNull(UserTypes.name(me.int("userType")), me.str("email")).joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(s.appTitle + " · " + App.api.baseUrl.substringAfter("://"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            SettingsSection("You") {
                NavItem(Icons.Outlined.Person, "Profile", "Your requests, quota and stats") { nav.go("user/${s.userId}") }
                ItemDivider()
                NavItem(Icons.Outlined.BookmarkBorder, "Watchlist", null) { nav.go("watchlist/${s.userId}") }
                ItemDivider()
                NavItem(Icons.Outlined.CalendarMonth, "Upcoming", "Release dates and new episodes for your requests") { nav.go("calendar") }
                ItemDivider()
                NavItem(Icons.Outlined.ManageAccounts, "Account settings", "Name, region, notifications, password") {
                    nav.go("user/${s.userId}/settings")
                }
            }
        }
        if (s.can(Perm.MANAGE_USERS, Perm.MANAGE_SETTINGS, Perm.MANAGE_BLOCKLIST, Perm.VIEW_BLOCKLIST, any = true)) {
            item {
                SettingsSection("Manage") {
                    var first = true
                    if (s.can(Perm.MANAGE_USERS)) {
                        NavItem(Icons.Outlined.Group, "Users", "Accounts, permissions and quotas") { nav.go("users") }
                        first = false
                    }
                    if (s.can(Perm.MANAGE_BLOCKLIST, Perm.VIEW_BLOCKLIST, any = true)) {
                        if (!first) ItemDivider()
                        NavItem(Icons.Outlined.Block, "Blocklist", "Titles hidden from discovery") { nav.go("blocklist") }
                        first = false
                    }
                    if (s.can(Perm.MANAGE_SETTINGS)) {
                        if (!first) ItemDivider()
                        NavItem(Icons.Outlined.AdminPanelSettings, "Server settings", "Media server, services, notifications, jobs, logs") {
                            nav.go("admin")
                        }
                    }
                }
            }
        }
        item {
            SettingsSection("App") {
                NavItem(Icons.Outlined.Settings, "App settings", "Theme, app lock, server, log out") { nav.go("appsettings") }
            }
        }
    }
}
