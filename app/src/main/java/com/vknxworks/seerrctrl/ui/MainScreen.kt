package com.vknxworks.seerrctrl.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.material3.NavigationRailItemDefaults
import com.vknxworks.seerrctrl.ui.components.CtrlBottomBar
import com.vknxworks.seerrctrl.ui.components.CtrlTab
import com.vknxworks.seerrctrl.ui.theme.CtrlColors
import com.vknxworks.seerrctrl.ui.theme.CtrlShapes
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
import com.vknxworks.seerrctrl.ui.components.FilterChip
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
import com.vknxworks.seerrctrl.ui.components.OutlinedTextField
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
import com.vknxworks.seerrctrl.R
import androidx.compose.ui.res.stringResource
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
        add(TabDef(MainTab.DISCOVER, Icons.Outlined.Explore, App.str(R.string.discover)))
        add(TabDef(MainTab.SEARCH, Icons.Outlined.Search, App.str(R.string.tab_search)))
        add(TabDef(MainTab.REQUESTS, Icons.Outlined.Theaters, App.str(R.string.requests), pending.value ?: 0))
        if (showIssues) add(TabDef(MainTab.ISSUES, Icons.Outlined.ReportProblem, App.str(R.string.issues), openIssues.value ?: 0))
        add(TabDef(MainTab.MORE, Icons.Outlined.MoreHoriz, App.str(R.string.tab_more)))
    }

    BackHandler(MainTab.selected != MainTab.DISCOVER) { MainTab.selected = MainTab.DISCOVER }

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
            NavigationRail(
                Modifier.statusBarsPadding(),
                containerColor = MaterialTheme.colorScheme.background,
            ) {
                Spacer(Modifier.height(8.dp))
                tabs.forEach { t ->
                    NavigationRailItem(
                        selected = MainTab.selected == t.index,
                        onClick = { MainTab.selected = t.index },
                        icon = { TabIcon(t) },
                        label = { Text(t.label, maxLines = 1) },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = CtrlColors.OnAccent,
                            selectedTextColor = CtrlColors.AccentText,
                            indicatorColor = CtrlColors.Accent,
                            unselectedIconColor = CtrlColors.Muted,
                            unselectedTextColor = CtrlColors.Muted,
                        ),
                    )
                }
            }
            Box(Modifier.weight(1f).fillMaxSize().statusBarsPadding().navigationBarsPadding()) { content() }
        }
    } else {
        // Content runs edge to edge under the floating tab bar.
        Box(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().statusBarsPadding()) { content() }
            CtrlBottomBar(
                tabs = tabs.map { CtrlTab(it.index, it.icon, it.label, it.badge) },
                selected = MainTab.selected,
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
    All(App.str(R.string.all), Icons.Outlined.Search), Keyword(App.str(R.string.keywords), Icons.Outlined.Tag), Company(App.str(R.string.studios), Icons.Outlined.WorkOutline)
}

@Composable
fun SearchTab() {
    val nav = LocalNav.current
    var query by rememberSaveable { mutableStateOf("") }
    var debounced by rememberSaveable { mutableStateOf("") }
    var kind by rememberSaveable { mutableStateOf(SearchKind.All) }
    LaunchedEffect(query) { delay(400); debounced = query.trim() }

    Column(Modifier.fillMaxSize()) {
        TabTitle(stringResource(R.string.tab_search))
        OutlinedTextField(
            value = query, onValueChange = { query = it },
            placeholder = { Text(stringResource(R.string.search_movies_series_and_people)) },
            leadingIcon = { Icon(Icons.Outlined.Search, null) },
            trailingIcon = { if (query.isNotEmpty()) IconButton({ query = "" }) { Icon(Icons.Outlined.Close, stringResource(R.string.clear)) } },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { debounced = query.trim() }),
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 4.dp),
        )
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                            SignalRow(
                                lead = "#${r.int("id")}",
                                title = name,
                                trailing = r.str("origin_country")?.let { c ->
                                    @Composable { Text(c.uppercase(), style = com.vknxworks.seerrctrl.ui.theme.CtrlType.Status, color = CtrlColors.Muted) }
                                },
                                icon = kind.icon,
                            ) {
                                if (kind == SearchKind.Keyword) nav.browse(BrowseSpec(name, "/discover/movies", mapOf("keywords" to r.int("id"))))
                                else nav.browse(BrowseSpec(name, "/discover/movies/studio/${r.int("id")}"))
                            }
                        }
                        if (list.isEmpty()) item { EmptyBox(Icons.Outlined.Search, stringResource(R.string.no_matches)) }
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
        item { SectionHeader(stringResource(R.string.browse)) }
        item {
            NavItem(Icons.Outlined.Theaters, stringResource(R.string.movies), stringResource(R.string.popular_movies_with_filters)) {
                nav.browse(BrowseSpec(App.str(R.string.movies), "/discover/movies"))
            }
        }
        item {
            NavItem(Icons.Outlined.Tv, stringResource(R.string.series_2), stringResource(R.string.popular_series_with_filters)) {
                nav.browse(BrowseSpec(App.str(R.string.series_2), "/discover/tv"))
            }
        }
        item { NavItem(Icons.Outlined.Explore, stringResource(R.string.trending), null) { nav.browse(BrowseSpec(App.str(R.string.trending), "/discover/trending")) } }
        item {
            NavItem(Icons.Outlined.Theaters, stringResource(R.string.upcoming_movies), null) {
                nav.browse(BrowseSpec(App.str(R.string.upcoming_movies), "/discover/movies/upcoming"))
            }
        }
        item {
            NavItem(Icons.Outlined.Tv, stringResource(R.string.upcoming_series), null) {
                nav.browse(BrowseSpec(App.str(R.string.upcoming_series), "/discover/tv/upcoming"))
            }
        }
    }
}

fun Modifier.clickableRow(onClick: () -> Unit) = this.clickable(onClick = onClick)

// ---------------- more ----------------

/** Your lists and, with the right permissions, managing the server. Account and settings are behind the profile picture. */
@Composable
fun MoreTab() {
    val nav = LocalNav.current
    val s = App.session
    LazyColumn(contentPadding = ListBottom) {
        item { TabTitle(stringResource(R.string.tab_more)) }
        item {
            SettingsSection(null) {
                NavItem(Icons.Outlined.BookmarkBorder, stringResource(R.string.watchlist), null) { nav.go("watchlist/${s.userId}") }
                ItemDivider()
                NavItem(Icons.Outlined.CalendarMonth, stringResource(R.string.upcoming), stringResource(R.string.release_dates_and_new_episodes_for)) { nav.go("calendar") }
            }
        }
        if (s.can(Perm.MANAGE_USERS, Perm.MANAGE_SETTINGS, Perm.MANAGE_BLOCKLIST, Perm.VIEW_BLOCKLIST, any = true)) {
            item {
                SettingsSection(stringResource(R.string.manage)) {
                    var first = true
                    if (s.can(Perm.MANAGE_USERS)) {
                        NavItem(Icons.Outlined.Group, stringResource(R.string.users), stringResource(R.string.accounts_permissions_and_quotas)) { nav.go("users") }
                        first = false
                    }
                    if (s.can(Perm.MANAGE_BLOCKLIST, Perm.VIEW_BLOCKLIST, any = true)) {
                        if (!first) ItemDivider()
                        NavItem(Icons.Outlined.Block, stringResource(R.string.blocklist), stringResource(R.string.titles_hidden_from_discovery)) { nav.go("blocklist") }
                        first = false
                    }
                    if (s.can(Perm.MANAGE_SETTINGS)) {
                        if (!first) ItemDivider()
                        NavItem(Icons.Outlined.AdminPanelSettings, stringResource(R.string.server_settings), stringResource(R.string.media_server_services_notifications)) {
                            nav.go("admin")
                        }
                    }
                }
            }
        }
    }
}
