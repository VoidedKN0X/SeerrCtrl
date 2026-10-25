package com.vknxworks.seerrctrl.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Person
import com.vknxworks.seerrctrl.ui.components.Button
import com.vknxworks.seerrctrl.ui.components.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import com.vknxworks.seerrctrl.ui.components.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import com.vknxworks.seerrctrl.App
import com.vknxworks.seerrctrl.data.MediaStatus
import com.vknxworks.seerrctrl.data.RequestStatus
import com.vknxworks.seerrctrl.data.Tile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import com.vknxworks.seerrctrl.R
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.statusBarsPadding
import com.vknxworks.seerrctrl.ui.components.CtrlBottomBarClearance
import com.vknxworks.seerrctrl.ui.components.CtrlDot
import com.vknxworks.seerrctrl.ui.components.CtrlSquareButton
import com.vknxworks.seerrctrl.ui.components.CtrlStatus
import com.vknxworks.seerrctrl.ui.theme.CtrlColors
import com.vknxworks.seerrctrl.ui.theme.CtrlShapes
import com.vknxworks.seerrctrl.ui.theme.CtrlType

// ---------------- navigation ----------------

val LocalNav = staticCompositionLocalOf<NavHostController> { error("no nav") }
val LocalSnackbar = staticCompositionLocalOf<SnackbarHostState> { error("no snackbar") }

/** A paged list from a discover-style endpoint (page/totalPages/results). */
data class BrowseSpec(val title: String, val path: String, val params: Map<String, Any?> = emptyMap())

object Browse {
    private val specs = mutableMapOf<String, BrowseSpec>()
    fun put(spec: BrowseSpec): String {
        val key = (spec.path + spec.params.toString()).hashCode().toUInt().toString(16)
        specs[key] = spec
        return key
    }
    fun get(key: String): BrowseSpec? = specs[key]
}

/**
 * Back from a screen the user is looking at. Repeat taps while a screen change is still animating are ignored,
 * and the first screen of the stack is never popped (that emptied the stack and left a black screen).
 */
fun NavHostController.back() {
    val current = currentBackStackEntry ?: return
    if (previousBackStackEntry == null) return
    if (!current.lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) return
    popBackStack()
}

/**
 * Closes the screen [owner] belongs to when it finishes on its own (saved, deleted), but only if it's still the
 * one on top: if the user already went back meanwhile, nothing else gets popped.
 */
fun NavHostController.close(owner: androidx.lifecycle.LifecycleOwner) {
    val current = currentBackStackEntry ?: return
    if (previousBackStackEntry == null) return
    if (owner is androidx.navigation.NavBackStackEntry && owner.id != current.id) return
    popBackStack()
}

/** Opens a screen from a tap, ignoring taps on a screen that's already on its way out or not shown yet. */
fun NavHostController.go(route: String) {
    if (currentBackStackEntry?.lifecycle?.currentState?.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED) == false) return
    navigate(route)
}

/** [close] for the screen this is called from. */
@Composable
fun rememberCloseScreen(): () -> Unit {
    val nav = LocalNav.current
    val owner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    return remember(nav, owner) { { nav.close(owner) } }
}

fun NavHostController.openTile(t: Tile) = when (t.type) {
    "person" -> go("person/${t.id}")
    "collection" -> go("collection/${t.id}")
    "tv" -> go("tv/${t.id}")
    else -> go("movie/${t.id}")
}

fun NavHostController.openMedia(type: String?, tmdbId: Int) =
    go(if (type == "tv") "tv/$tmdbId" else "movie/$tmdbId")

fun NavHostController.browse(spec: BrowseSpec) = go("browse/${Browse.put(spec)}")

// ---------------- loading ----------------

/** Result of a screen load: loading, failed or a value, with a reload hook. */
@Stable
class Load<T>(private val scope: CoroutineScope, private val loader: suspend () -> T) {
    var value by mutableStateOf<T?>(null)
    var error by mutableStateOf<String?>(null)
    var loading by mutableStateOf(true)
    var refreshing by mutableStateOf(false)

    fun reload(silent: Boolean = false) {
        scope.launch {
            if (silent || value != null) refreshing = true else loading = true
            error = null
            try { value = loader() } catch (e: Exception) { error = e.message ?: e.toString() }
            loading = false; refreshing = false
        }
    }
}

@Composable
fun <T> rememberLoad(vararg keys: Any?, loader: suspend () -> T): Load<T> {
    val scope = rememberCoroutineScope()
    val load = remember(*keys) { Load(scope, loader) }
    LaunchedEffect(load) { load.reload() }
    return load
}

/** Shows a spinner, an error with retry, or [content] for the loaded value. */
@Composable
fun <T> Loaded(load: Load<T>, modifier: Modifier = Modifier, content: @Composable (T) -> Unit) {
    val v = load.value
    when {
        v != null -> content(v)
        load.loading -> LoadingBox(modifier)
        else -> ErrorBox(load.error ?: stringResource(R.string.couldn_t_load), modifier) { load.reload() }
    }
}

@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

@Composable
fun ErrorBox(message: String, modifier: Modifier = Modifier, onRetry: (() -> Unit)? = null) {
    Column(
        modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Outlined.CloudOff, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        Text(message, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (onRetry != null) {
            Spacer(Modifier.height(16.dp))
            Button(onRetry) { Text(stringResource(R.string.try_again)) }
        }
    }
}

@Composable
fun EmptyBox(icon: ImageVector, text: String, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(vertical = 48.dp, horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, null, Modifier.size(44.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(10.dp))
        Text(text, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Runs a server action, showing its error (or [done]) in the snackbar. */
class Actions(private val scope: CoroutineScope, private val snack: SnackbarHostState) {
    var busy by mutableStateOf(false)
        private set

    fun run(done: String? = null, onOk: suspend () -> Unit = {}, block: suspend () -> Unit) {
        scope.launch {
            busy = true
            try {
                block()
                onOk()
                done?.let { snack.showSnackbar(it) }
            } catch (e: Exception) {
                snack.showSnackbar(e.message ?: App.str(R.string.something_went_wrong))
            }
            busy = false
        }
    }

    fun say(text: String) { scope.launch { snack.showSnackbar(text) } }
}

@Composable
fun rememberActions(): Actions {
    val scope = rememberCoroutineScope()
    val snack = LocalSnackbar.current
    return remember { Actions(scope, snack) }
}

// ---------------- app bars ----------------

/**
 * Top bar of detail screens in the Signal look: a 44 dp square back button and the title in Space Grotesk,
 * with the screen's own actions on the right. No filled app bar. [scroll] is accepted for the screens that
 * pass one; the bar itself stays put.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackBar(
    title: String,
    onBack: () -> Unit,
    subtitle: String? = null,
    @Suppress("UNUSED_PARAMETER") scroll: TopAppBarScrollBehavior? = null,
    actions: @Composable () -> Unit = {},
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CtrlSquareButton(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.back), onBack)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            subtitle?.let {
                Text(it, style = CtrlType.Data, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) { actions() }
    }
}

// ---------------- media tiles ----------------

@Composable
fun statusColor(s: MediaStatus): Color? = when (s) {
    MediaStatus.Available -> Status.available
    MediaStatus.Partial -> Status.partial
    MediaStatus.Processing -> Status.processing
    MediaStatus.Pending -> Status.pending
    MediaStatus.Blocklisted, MediaStatus.Deleted -> Status.declined
    MediaStatus.Unknown -> null
}

@Composable
fun requestColor(s: RequestStatus): Color = when (s) {
    RequestStatus.Pending -> Status.pending
    RequestStatus.Approved -> Status.processing
    RequestStatus.Declined, RequestStatus.Failed -> Status.declined
    RequestStatus.Completed -> Status.available
}

/** A status word in mono capitals with a square dot in its colour (the Signal take on a badge). */
@Composable
fun Pill(text: String, color: Color, modifier: Modifier = Modifier) {
    CtrlStatus(text, color, modifier, dotFirst = true)
}

@Composable
fun MediaStatusPill(s: MediaStatus, is4k: Boolean = false, modifier: Modifier = Modifier) {
    val c = statusColor(s) ?: return
    Pill(if (is4k) "4K ${s.label}" else s.label, c, modifier)
}

@Composable
fun RequestPill(s: RequestStatus, modifier: Modifier = Modifier) = Pill(s.label, requestColor(s), modifier)

/** A poster with its availability badge and a title line underneath. */
@Composable
fun PosterCard(tile: Tile, width: Dp?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val src = remember { MutableInteractionSource() }
    val m = if (width != null) modifier.width(width) else modifier
    Column(m.pressScale(src).clickable(src, null, onClick = onClick)) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(2f / 3f).clip(CtrlShapes.Poster)
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .border(1.dp, CtrlColors.Outline, CtrlShapes.Poster)
        ) {
            val url = App.session.tmdb(tile.image, "w342")
            if (url != null) {
                AsyncImage(url, tile.title, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Icon(
                    if (tile.type == "person") Icons.Outlined.Person else Icons.Outlined.Movie, null,
                    Modifier.align(Alignment.Center).size(36.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (tile.type == "movie" || tile.type == "tv") {
                Box(
                    Modifier.align(Alignment.TopStart).padding(6.dp).clip(CtrlShapes.Badge)
                        .background(CtrlColors.Background.copy(alpha = 0.78f)).padding(horizontal = 6.dp, vertical = 3.dp),
                ) {
                    Text(
                        (if (tile.type == "tv") stringResource(R.string.series) else stringResource(R.string.movie)).uppercase(),
                        style = CtrlType.Status, color = CtrlColors.Text,
                    )
                }
                val c = statusColor(tile.status)
                if (c != null && tile.status != MediaStatus.Unknown) {
                    Box(
                        Modifier.align(Alignment.TopEnd).padding(7.dp).size(14.dp).clip(RoundedCornerShape(4.dp))
                            .background(CtrlColors.Background).padding(3.dp)
                    ) { CtrlDot(c, size = 8.dp) }
                }
            }
        }
        Text(
            tile.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = CtrlColors.Text,
            maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp, start = 2.dp, end = 2.dp),
        )
        val sub = tile.subtitle ?: tile.year
        if (sub != null) {
            Text(
                sub, style = CtrlType.Data, color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 2.dp),
            )
        }
    }
}

fun posterWidth(): Dp = when (App.settings.posterSize) { 0 -> 104.dp; 2 -> 150.dp; else -> 124.dp }

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, onMore: (() -> Unit)? = null) {
    Row(
        modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 22.dp, bottom = 8.dp).heightIn(min = 40.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (onMore != null) {
            TextButton(onMore) {
                Text(stringResource(R.string.see_all).uppercase(), style = CtrlType.Status, color = CtrlColors.AccentText)
                Spacer(Modifier.width(4.dp))
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, Modifier.size(14.dp), tint = CtrlColors.AccentText)
            }
        }
    }
}

@Composable
fun TileRow(title: String, tiles: List<Tile>, onMore: (() -> Unit)? = null) {
    if (tiles.isEmpty()) return
    val nav = LocalNav.current
    SectionHeader(title, onMore = onMore)
    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        // TMDB lists sometimes name the same title twice; a repeated key would crash the row.
        items(tiles.distinctBy { it.type + it.id }, key = { it.type + it.id }) { t -> PosterCard(t, posterWidth(), { nav.openTile(t) }) }
    }
}

@Composable
fun Avatar(url: String?, name: String, size: Dp = 40.dp) {
    Box(
        Modifier.size(size).clip(RoundedCornerShape(size * 0.32f)).background(CtrlColors.Raised)
            .border(1.dp, CtrlColors.ControlOutline, RoundedCornerShape(size * 0.32f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            name.trim().take(1).uppercase().ifEmpty { "?" },
            color = CtrlColors.Text, fontWeight = FontWeight.SemiBold,
        )
        val u = App.session.avatar(url)
        if (u != null) AsyncImage(u, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    }
}

/** Backdrop image fading into the background colour. */
@Composable
fun Backdrop(path: String?, height: Dp, modifier: Modifier = Modifier) {
    val bg = MaterialTheme.colorScheme.background
    Box(modifier.fillMaxWidth().height(height)) {
        App.session.tmdb(path, "w1280")?.let {
            AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(bg.copy(alpha = 0.25f), bg.copy(alpha = 0.55f), bg))
            )
        )
    }
}

/**
 * A plain list row in the Signal look: min 52 dp, a 1 dp divider on top, an optional muted mono ID or
 * icon at the start, the title (16 sp medium) with an optional line under it, and [trailing] at the end
 * (usually a [CtrlStatus]).
 */
@Composable
fun SignalRow(
    title: String,
    modifier: Modifier = Modifier,
    lead: String? = null,
    icon: ImageVector? = null,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    Column(modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)) {
        HorizontalDivider(Modifier.padding(horizontal = 20.dp), thickness = 1.dp, color = CtrlColors.Divider)
        Row(
            Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(horizontal = 24.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(icon, null, Modifier.size(20.dp), tint = CtrlColors.Muted)
                Spacer(Modifier.width(14.dp))
            } else if (lead != null) {
                Text(lead, style = CtrlType.Data, color = CtrlColors.Muted, maxLines = 1, modifier = Modifier.widthIn(min = 30.dp))
                Spacer(Modifier.width(14.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (subtitle != null) Text(subtitle, style = CtrlType.Data, color = CtrlColors.Muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            if (trailing != null) {
                Spacer(Modifier.width(12.dp))
                trailing()
            }
        }
    }
}

// ---------------- settings-style lists ----------------

/** A titled block of rows, the same as the settings groups in every Ctrl app. */
@Composable
fun SettingsSection(title: String?, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    com.vknxworks.seerrctrl.ui.settings.Group(title, modifier) { content() }
}

@Composable
fun itemColors() = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer)

/** Rows in a group are set apart by their spacing, as in the other Ctrl apps. */
@Composable
fun ItemDivider() = Unit

@Composable
fun NavItem(icon: ImageVector?, title: String, subtitle: String? = null, trailing: (@Composable () -> Unit)? = null, onClick: () -> Unit) {
    com.vknxworks.seerrctrl.ui.settings.GroupRow(title, subtitle, icon, onClick = onClick, trailing = trailing)
}

@Composable
fun ToggleItem(
    icon: ImageVector?,
    title: String,
    subtitle: String?,
    checked: Boolean,
    enabled: Boolean = true,
    onChange: (Boolean) -> Unit,
) {
    com.vknxworks.seerrctrl.ui.settings.GroupSwitch(title, subtitle, checked, icon, enabled, onChange)
}

/** A row showing the current choice; tapping it opens a list to pick another. */
@Composable
fun ChoiceItem(icon: ImageVector?, title: String, options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    com.vknxworks.seerrctrl.ui.settings.GroupChoice(title, options.mapIndexed { i, label -> i to label }, selected, icon) { onSelect(it) }
}

// ---------------- misc ----------------

/**
 * Opens a web link. Links come from the server (play links, trailers, ratings, Radarr/Sonarr pages), so only
 * http and https are opened; anything else (content:, file:, intent:, other apps' schemes) is ignored.
 */
fun openUrl(context: Context, url: String) {
    // Opening a link isn't leaving the app: coming straight back doesn't ask for the PIN again.
    com.vknxworks.seerrctrl.ui.lock.expectReturn()
    val uri = Uri.parse(url.trim())
    val scheme = uri.scheme?.lowercase()
    if ((scheme != "http" && scheme != "https") || uri.host.isNullOrEmpty()) return
    try {
        if (App.settings.useCustomTabs) {
            CustomTabsIntent.Builder().setShowTitle(true).build().launchUrl(context, uri)
        } else {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    } catch (_: Exception) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {}
    }
}

/** Bottom padding that keeps the last list item clear of the floating tab bar and the gesture bar. */
val ListBottom = PaddingValues(bottom = CtrlBottomBarClearance)

@Composable
fun rememberIntState(initial: Int = 0) = remember { mutableIntStateOf(initial) }
