package com.vknxworks.seerrctrl.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.vknxworks.seerrctrl.App
import com.vknxworks.seerrctrl.data.MediaStatus
import com.vknxworks.seerrctrl.data.Perm
import com.vknxworks.seerrctrl.data.arr
import com.vknxworks.seerrctrl.data.int
import com.vknxworks.seerrctrl.data.jsonOf
import com.vknxworks.seerrctrl.data.objs
import com.vknxworks.seerrctrl.data.prettyDate
import com.vknxworks.seerrctrl.data.str
import com.vknxworks.seerrctrl.data.strings
import com.vknxworks.seerrctrl.data.toTile
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import com.vknxworks.seerrctrl.R

@Composable
private fun FloatingBack() {
    val nav = LocalNav.current
    FilledIconButton(
        onClick = { nav.back() },
        colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.Black.copy(alpha = 0.45f), contentColor = Color.White),
        modifier = Modifier.statusBarsPadding().padding(8.dp),
    ) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.back)) }
}

@Composable
fun PersonScreen(id: Int) {
    val nav = LocalNav.current
    val load = rememberLoad(id) {
        coroutineScope {
            val p = async { App.api.obj("/person/$id") }
            val c = async { App.api.obj("/person/$id/combined_credits") }
            p.await() to c.await()
        }
    }
    var crewMode by remember { mutableStateOf(false) }
    var bioOpen by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        Loaded(load, Modifier.statusBarsPadding()) { (p, credits) ->
            val cast = credits.objs("cast").distinctBy { it.str("mediaType") + it.int("id") }
                .sortedByDescending { (it.str("releaseDate") ?: it.str("firstAirDate") ?: "") }.mapNotNull { it.toTile() }
            val crew = credits.objs("crew").distinctBy { it.str("mediaType") + it.int("id") }
                .sortedByDescending { (it.str("releaseDate") ?: it.str("firstAirDate") ?: "") }.mapNotNull { it.toTile() }
            val shown = if (crewMode && crew.isNotEmpty() || cast.isEmpty()) crew else cast
            LazyVerticalGrid(
                columns = GridCells.Adaptive(posterWidth()),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 64.dp, bottom = 128.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.statusBarsPadding(),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column {
                        Row {
                            Box(Modifier.width(110.dp).aspectRatio(2f / 3f).clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
                                App.session.tmdb(p.str("profilePath"), "w342")?.let { AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(p.str("name") ?: "", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                p.str("knownForDepartment")?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                                val born = prettyDate(p.str("birthday"))
                                val died = prettyDate(p.str("deathday"))
                                if (born != null) Text(p.str("placeOfBirth")?.let { stringResource(R.string.born_in, born, it) } ?: stringResource(R.string.born, born), style = MaterialTheme.typography.bodySmall)
                                if (died != null) Text(stringResource(R.string.died, died), style = MaterialTheme.typography.bodySmall)
                                val aka = p.arr("alsoKnownAs").strings().take(3)
                                if (aka.isNotEmpty()) Text(stringResource(R.string.also_known_as, aka.joinToString(", ")), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        p.str("biography")?.let { bio ->
                            Text(
                                bio, style = MaterialTheme.typography.bodyMedium, maxLines = if (bioOpen) Int.MAX_VALUE else 6,
                                modifier = Modifier.padding(top = 12.dp).animateContentSize().clickable { bioOpen = !bioOpen },
                            )
                        }
                        if (cast.isNotEmpty() && crew.isNotEmpty()) {
                            Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(!crewMode, { crewMode = false }, label = { Text(stringResource(R.string.appearances, cast.size)) })
                                FilterChip(crewMode, { crewMode = true }, label = { Text(stringResource(R.string.crew_2, crew.size)) })
                            }
                        }
                    }
                }
                items(shown, key = { it.type + it.id }) { t -> PosterCard(t, null, { nav.openTile(t) }) }
            }
        }
        FloatingBack()
    }
}

@Composable
fun CollectionScreen(id: Int) {
    val nav = LocalNav.current
    val s = App.session
    val act = rememberActions()
    val load = rememberLoad(id) { App.api.obj("/collection/$id") }
    var confirm by remember { mutableStateOf<Boolean?>(null) } // 4k?

    Box(Modifier.fillMaxSize()) {
        Loaded(load, Modifier.statusBarsPadding()) { c ->
            val parts = c.objs("parts").sortedBy { it.str("releaseDate") ?: "9999" }
            val tiles = parts.mapNotNull { it.put("mediaType", "movie").toTile() }
            val missing = tiles.filter { it.status == MediaStatus.Unknown || it.status == MediaStatus.Deleted }
            val missing4k = tiles.filter { it.status4k == MediaStatus.Unknown || it.status4k == MediaStatus.Deleted }
            LazyVerticalGrid(
                columns = GridCells.Adaptive(posterWidth()),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 128.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column {
                        Box(Modifier.padding(horizontal = 0.dp)) { Backdrop(c.str("backdropPath"), 220.dp) }
                        Text(c.str("name") ?: "", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text(pluralStringResource(R.plurals.movies_count, tiles.size, tiles.size), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        c.str("overview")?.let { Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp)) }
                        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (missing.isNotEmpty() && s.can(Perm.REQUEST, Perm.REQUEST_MOVIE, any = true)) {
                                Button({ confirm = false }) { Icon(Icons.Outlined.Add, null); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.request_2, missing.size)) }
                            }
                            if (s.movie4k && missing4k.isNotEmpty() && s.can(Perm.REQUEST_4K, Perm.REQUEST_4K_MOVIE, any = true)) {
                                OutlinedButton({ confirm = true }) { Text(stringResource(R.string.request_in_4k)) }
                            }
                            if (s.can(Perm.MANAGE_BLOCKLIST)) {
                                OutlinedButton({
                                    act.run(App.str(R.string.collection_blocklisted), onOk = { load.reload(silent = true) }) { App.api.post("/blocklist/collection/$id") }
                                }) { Icon(Icons.Outlined.Block, null) }
                            }
                        }
                    }
                }
                items(tiles, key = { it.id }) { t -> PosterCard(t, null, { nav.openTile(t) }) }
            }
            confirm?.let { fourK ->
                val list = if (fourK) missing4k else missing
                AlertDialog(
                    onDismissRequest = { confirm = null },
                    title = { Text(if (fourK) pluralStringResource(R.plurals.request_movies_4k, list.size, list.size) else pluralStringResource(R.plurals.request_movies, list.size, list.size)) },
                    text = { Text(list.joinToString("\n") { "• " + it.title + (it.year?.let { y -> " ($y)" } ?: "") }) },
                    confirmButton = {
                        TextButton({
                            confirm = null
                            act.run(App.plural(R.plurals.requested_movies, list.size, list.size), onOk = { load.reload(silent = true) }) {
                                var failed = 0
                                list.forEach { t ->
                                    try { App.api.post("/request", jsonOf("mediaType" to "movie", "mediaId" to t.id, "is4k" to fourK)) }
                                    catch (_: Exception) { failed++ }
                                }
                                if (failed > 0) error(App.str(R.string.of_requests_failed, failed, list.size))
                            }
                        }) { Text(stringResource(R.string.request)) }
                    },
                    dismissButton = { TextButton({ confirm = null }) { Text(stringResource(R.string.lock_cancel)) } },
                )
            }
        }
        FloatingBack()
    }
}
