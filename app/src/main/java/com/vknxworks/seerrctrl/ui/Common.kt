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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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

fun NavHostController.openTile(t: Tile) = when (t.type) {
    "person" -> navigate("person/${t.id}")
    "collection" -> navigate("collection/${t.id}")
    "tv" -> navigate("tv/${t.id}")
    else -> navigate("movie/${t.id}")
}

fun NavHostController.openMedia(type: String?, tmdbId: Int) =
    navigate(if (type == "tv") "tv/$tmdbId" else "movie/$tmdbId")

fun NavHostController.browse(spec: BrowseSpec) = navigate("browse/${Browse.put(spec)}")

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
        else -> ErrorBox(load.error ?: "Couldn't load", modifier) { load.reload() }
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
            Button(onRetry) { Text("Try again") }
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
                snack.showSnackbar(e.message ?: "Something went wrong")
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackBar(
    title: String,
    onBack: () -> Unit,
    subtitle: String? = null,
    scroll: TopAppBarScrollBehavior? = null,
    actions: @Composable () -> Unit = {},
) {
    TopAppBar(
        title = {
            Column {
                Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                subtitle?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
            }
        },
        navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") } },
        actions = { actions() },
        scrollBehavior = scroll,
        colors = TopAppBarDefaults.topAppBarColors(scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer),
    )
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

@Composable
fun Pill(text: String, color: Color, modifier: Modifier = Modifier) {
    Surface(color = color, contentColor = Status.onBadge, shape = RoundedCornerShape(50), modifier = modifier) {
        Text(
            text, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), maxLines = 1,
        )
    }
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
            Modifier.fillMaxWidth().aspectRatio(2f / 3f).clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
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
                Surface(
                    color = Color.Black.copy(alpha = 0.55f), contentColor = Color.White, shape = RoundedCornerShape(50),
                    modifier = Modifier.align(Alignment.TopStart).padding(6.dp),
                ) {
                    Text(
                        if (tile.type == "tv") "SERIES" else "MOVIE",
                        style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
                val c = statusColor(tile.status)
                if (c != null && tile.status != MediaStatus.Unknown) {
                    Box(
                        Modifier.align(Alignment.TopEnd).padding(7.dp).size(12.dp).clip(CircleShape).background(c)
                    )
                }
            }
        }
        Text(
            tile.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium,
            maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp, start = 2.dp, end = 2.dp),
        )
        val sub = tile.subtitle ?: tile.year
        if (sub != null) {
            Text(
                sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 2.dp),
            )
        }
    }
}

fun posterWidth(): Dp = when (App.settings.posterSize) { 0 -> 104.dp; 2 -> 150.dp; else -> 124.dp }

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, onMore: (() -> Unit)? = null) {
    Row(
        modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 18.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        if (onMore != null) {
            TextButton(onMore) {
                Text("See all")
                Spacer(Modifier.width(4.dp))
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun TileRow(title: String, tiles: List<Tile>, onMore: (() -> Unit)? = null) {
    if (tiles.isEmpty()) return
    val nav = LocalNav.current
    SectionHeader(title, onMore = onMore)
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(tiles, key = { it.type + it.id }) { t -> PosterCard(t, posterWidth(), { nav.openTile(t) }) }
    }
}

@Composable
fun Avatar(url: String?, name: String, size: Dp = 40.dp) {
    Box(
        Modifier.size(size).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            name.trim().take(1).uppercase().ifEmpty { "?" },
            color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold,
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

// ---------------- settings-style lists ----------------

@Composable
fun SettingsSection(title: String?, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier.padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(18.dp))
        if (title != null) {
            Text(
                title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
            )
        }
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            modifier = Modifier.fillMaxWidth(),
        ) { Column { content() } }
    }
}

@Composable
fun itemColors() = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)

@Composable
fun ItemDivider() = HorizontalDivider(
    Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
)

@Composable
fun NavItem(icon: ImageVector?, title: String, subtitle: String? = null, trailing: (@Composable () -> Unit)? = null, onClick: () -> Unit) {
    ListItem(
        leadingContent = icon?.let { { Icon(it, null) } },
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it) } },
        trailingContent = trailing,
        colors = itemColors(),
        modifier = Modifier.clickable(onClick = onClick),
    )
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
    ListItem(
        leadingContent = icon?.let { { Icon(it, null) } },
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it) } },
        trailingContent = { Switch(checked, onChange, enabled = enabled) },
        colors = itemColors(),
        modifier = Modifier.clickable(enabled) { onChange(!checked) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChoiceItem(icon: ImageVector?, title: String, options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Column(Modifier.padding(bottom = 12.dp)) {
        ListItem(leadingContent = icon?.let { { Icon(it, null) } }, headlineContent = { Text(title) }, colors = itemColors())
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            options.forEachIndexed { i, label ->
                SegmentedButton(
                    selected = i == selected,
                    onClick = { onSelect(i) },
                    shape = SegmentedButtonDefaults.itemShape(i, options.size),
                ) { Text(label, maxLines = 1) }
            }
        }
    }
}

// ---------------- misc ----------------

fun openUrl(context: Context, url: String) {
    try {
        if (App.settings.useCustomTabs && (url.startsWith("http://") || url.startsWith("https://"))) {
            CustomTabsIntent.Builder().setShowTitle(true).build().launchUrl(context, Uri.parse(url))
        } else {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    } catch (_: Exception) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {}
    }
}

/** Bottom padding that keeps the last list item clear of the gesture bar. */
val ListBottom = PaddingValues(bottom = 128.dp)

@Composable
fun rememberIntState(initial: Int = 0) = remember { mutableIntStateOf(initial) }
