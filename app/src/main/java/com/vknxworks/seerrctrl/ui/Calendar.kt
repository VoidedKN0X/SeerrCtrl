package com.vknxworks.seerrctrl.ui

import android.content.Intent
import android.provider.CalendarContract
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.vknxworks.seerrctrl.App
import com.vknxworks.seerrctrl.data.MediaStatus
import com.vknxworks.seerrctrl.data.Perm
import com.vknxworks.seerrctrl.data.int
import com.vknxworks.seerrctrl.data.intOrNull
import com.vknxworks.seerrctrl.data.obj
import com.vknxworks.seerrctrl.data.objs
import com.vknxworks.seerrctrl.data.str
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

/** One dated thing on the calendar: a release (cinema, digital, disc) or an episode airing. */
data class CalEvent(
    val date: LocalDate,
    val type: String,
    val tmdbId: Int,
    val title: String,
    val poster: String?,
    val label: String,
    val kind: Kind,
) {
    enum class Kind { Cinema, Digital, Physical, Release, Episode, Premiere }
}

private enum class Source(val label: String) { Mine("My requests"), All("All requests"), Watchlist("Watchlist") }

private fun parseDay(raw: String?): LocalDate? = try {
    when {
        raw.isNullOrBlank() -> null
        raw.length <= 10 -> LocalDate.parse(raw)
        else -> java.time.Instant.parse(raw).atZone(ZoneId.systemDefault()).toLocalDate()
    }
} catch (_: Exception) { null }

/** Upcoming dates for one movie or series, from its details. */
fun eventsFor(type: String, d: JSONObject, region: String): List<CalEvent> {
    val id = d.int("id")
    val title = d.str("title") ?: d.str("name") ?: return emptyList()
    val poster = d.str("posterPath")
    val out = mutableListOf<CalEvent>()
    if (type == "tv") {
        d.obj("nextEpisodeToAir")?.let { e ->
            parseDay(e.str("airDate"))?.let { day ->
                out += CalEvent(day, type, id, title, poster, "S${e.int("seasonNumber")}E${e.int("episodeNumber")}" + (e.str("name")?.let { " · $it" } ?: ""), CalEvent.Kind.Episode)
            }
        }
        d.objs("seasons").filter { it.int("seasonNumber") > 0 }.forEach { s ->
            val day = parseDay(s.str("airDate")) ?: return@forEach
            if (out.none { it.date == day }) out += CalEvent(day, type, id, title, poster, "${s.str("name") ?: "Season ${s.int("seasonNumber")}"} premiere", CalEvent.Kind.Premiere)
        }
    } else {
        val rel = d.obj("releases").objs("results")
        val local = rel.firstOrNull { it.str("iso_3166_1") == region } ?: rel.firstOrNull { it.str("iso_3166_1") == "US" }
        val seen = mutableSetOf<CalEvent.Kind>()
        local.objs("release_dates").sortedBy { it.str("release_date") }.forEach { r ->
            val day = parseDay(r.str("release_date")) ?: return@forEach
            val kind = when (r.int("type")) {
                1, 2, 3 -> CalEvent.Kind.Cinema
                4 -> CalEvent.Kind.Digital
                5 -> CalEvent.Kind.Physical
                else -> return@forEach
            }
            if (!seen.add(kind)) return@forEach
            out += CalEvent(
                day, type, id, title, poster,
                when (kind) { CalEvent.Kind.Cinema -> "In cinemas"; CalEvent.Kind.Digital -> "Digital release"; else -> "Disc release" } +
                    (r.str("note")?.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""),
                kind,
            )
        }
        if (out.isEmpty()) parseDay(d.str("releaseDate"))?.let { out += CalEvent(it, type, id, title, poster, "Release", CalEvent.Kind.Release) }
    }
    return out
}

/** Release dates and new episodes for what you (or everyone) requested, or your watchlist. */
@Composable
fun CalendarScreen() {
    val nav = LocalNav.current
    val context = LocalContext.current
    val s = App.session
    val canAll = s.can(Perm.MANAGE_REQUESTS, Perm.REQUEST_VIEW, any = true)
    var source by rememberSaveable { mutableStateOf(Source.Mine) }
    var month by remember { mutableStateOf(YearMonth.now()) }
    var selected by remember { mutableStateOf<LocalDate?>(null) }
    val region = s.streamingRegion

    val load = rememberLoad(source) {
        val pairs: List<Pair<String, Int>> = when (source) {
            Source.Watchlist -> (1..3).flatMap { p ->
                try { App.api.obj("/discover/watchlist", mapOf("page" to p)).objs("results") } catch (_: Exception) { emptyList() }
            }.map { (it.str("mediaType") ?: "movie") to it.int("tmdbId") }
            else -> App.api.obj(
                "/request",
                mapOf("take" to 100, "sort" to "modified", "filter" to "all", "requestedBy" to if (source == Source.Mine) s.userId else null),
            ).objs("results")
                // Leave out titles that are fully on the server already, unless a series might get new episodes.
                .filter { r ->
                    val m = r.obj("media")
                    m.str("mediaType") == "tv" || MediaStatus.of(m.intOrNull("status")) != MediaStatus.Available
                }
                .map { (it.obj("media").str("mediaType") ?: "movie") to it.obj("media").int("tmdbId") }
        }.distinct()
        val gate = Semaphore(6)
        coroutineScope {
            pairs.map { (t, id) ->
                async { gate.withPermit { try { eventsFor(t, MediaCache.get(t, id), region) } catch (_: Exception) { emptyList() } } }
            }.awaitAll().flatten().sortedBy { it.date }
        }
    }

    Scaffold(topBar = { BackBar("Upcoming", { nav.back() }) }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Source.entries.filter { it != Source.All || canAll }.forEach { src ->
                    FilterChip(source == src, { source = src; selected = null }, label = { Text(src.label) })
                }
            }
            Loaded(load) { events ->
                val byDay = remember(events) { events.groupBy { it.date } }
                val today = LocalDate.now()
                val shown = when (val sel = selected) {
                    null -> events.filter { YearMonth.from(it.date) == month && (month != YearMonth.now() || !it.date.isBefore(today)) }
                    else -> byDay[sel].orEmpty()
                }
                LazyColumn(contentPadding = PaddingValues(bottom = 128.dp)) {
                    item {
                        MonthGrid(month, byDay, selected, onMonth = { month = it; selected = null }, onDay = { selected = if (selected == it) null else it })
                    }
                    item {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                selected?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL))
                                    ?: if (month == YearMonth.now()) "Coming up this month" else month.month.getDisplayName(TextStyle.FULL, Locale.getDefault()) + " " + month.year,
                                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f),
                            )
                            if (selected != null) TextButton({ selected = null }) { Text("Whole month") }
                        }
                    }
                    val rows = shown.mapIndexed { i, e -> Triple(i, e, selected == null && (i == 0 || shown[i - 1].date != e.date)) }
                    items(rows, key = { it.first }) { (_, e, header) ->
                        Column {
                            if (header) {
                                Text(
                                    e.date.format(DateTimeFormatter.ofPattern("EEE d MMM")),
                                    style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(start = 16.dp, top = 10.dp, bottom = 4.dp),
                                )
                            }
                            EventRow(e, onOpen = { nav.openMedia(e.type, e.tmdbId) }, onAdd = { addToCalendar(context, e) })
                        }
                    }
                    if (shown.isEmpty()) {
                        item { EmptyBox(Icons.Outlined.EventNote, if (events.isEmpty()) "Nothing dated is coming up for these titles" else "Nothing on these dates") }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    byDay: Map<LocalDate, List<CalEvent>>,
    selected: LocalDate?,
    onMonth: (YearMonth) -> Unit,
    onDay: (LocalDate) -> Unit,
) {
    val firstDow = WeekFields.of(Locale.getDefault()).firstDayOfWeek
    val days = (0 until 7).map { firstDow.plus(it.toLong()) }
    val lead = (month.atDay(1).dayOfWeek.value - firstDow.value + 7) % 7
    val cells = List(lead) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
    val today = LocalDate.now()
    Column(Modifier.padding(horizontal = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton({ onMonth(month.minusMonths(1)) }) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, "Previous month") }
            Text(
                month.month.getDisplayName(TextStyle.FULL, Locale.getDefault()) + " " + month.year,
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f),
            )
            IconButton({ onMonth(month.plusMonths(1)) }) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, "Next month") }
        }
        Row {
            days.forEach { d: DayOfWeek ->
                Text(
                    d.getDisplayName(TextStyle.NARROW, Locale.getDefault()), Modifier.weight(1f), textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        cells.chunked(7).forEach { week ->
            Row {
                week.forEach { day ->
                    Box(Modifier.weight(1f).aspectRatio(1f).padding(2.dp), contentAlignment = Alignment.Center) {
                        if (day != null) {
                            val has = byDay[day].orEmpty()
                            val isSel = day == selected
                            Column(
                                Modifier.fillMaxSize().clip(CircleShape)
                                    .background(
                                        when {
                                            isSel -> MaterialTheme.colorScheme.primary
                                            day == today -> MaterialTheme.colorScheme.secondaryContainer
                                            else -> androidx.compose.ui.graphics.Color.Transparent
                                        }
                                    )
                                    .clickable(enabled = has.isNotEmpty()) { onDay(day) },
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    "${day.dayOfMonth}",
                                    color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (has.isNotEmpty()) FontWeight.Bold else FontWeight.Normal,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                if (has.isNotEmpty()) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                        has.take(3).forEach { e ->
                                            Box(
                                                Modifier.size(5.dp).clip(CircleShape).background(
                                                    if (isSel) MaterialTheme.colorScheme.onPrimary
                                                    else if (e.kind == CalEvent.Kind.Episode || e.kind == CalEvent.Kind.Premiere) Status.processing else Status.available
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun EventRow(e: CalEvent, onOpen: () -> Unit, onAdd: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(44.dp).aspectRatio(2f / 3f).clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
            App.session.tmdb(e.poster, "w92")?.let { AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(e.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(e.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onAdd) { Icon(Icons.Outlined.EventAvailable, "Add to calendar") }
    }
}

private fun addToCalendar(context: android.content.Context, e: CalEvent) {
    val start = e.date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val i = Intent(Intent.ACTION_INSERT)
        .setData(CalendarContract.Events.CONTENT_URI)
        .putExtra(CalendarContract.Events.TITLE, "${e.title}: ${e.label}")
        .putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, true)
        .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start)
        .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, start + 86_400_000L)
        .putExtra(CalendarContract.Events.DESCRIPTION, "From SeerrCtrl")
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try { context.startActivity(i) } catch (_: Exception) {}
}

