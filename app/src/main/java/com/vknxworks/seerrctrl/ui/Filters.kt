package com.vknxworks.seerrctrl.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.vknxworks.seerrctrl.App
import com.vknxworks.seerrctrl.data.int
import com.vknxworks.seerrctrl.data.objects
import com.vknxworks.seerrctrl.data.str

private val movieSorts = listOf(
    "popularity.desc" to "Most popular", "popularity.asc" to "Least popular",
    "primary_release_date.desc" to "Newest", "primary_release_date.asc" to "Oldest",
    "vote_average.desc" to "Highest rated", "vote_average.asc" to "Lowest rated",
    "vote_count.desc" to "Most votes", "revenue.desc" to "Highest revenue",
    "original_title.asc" to "Title A–Z", "original_title.desc" to "Title Z–A",
)
private val tvSorts = listOf(
    "popularity.desc" to "Most popular", "popularity.asc" to "Least popular",
    "first_air_date.desc" to "Newest", "first_air_date.asc" to "Oldest",
    "vote_average.desc" to "Highest rated", "vote_average.asc" to "Lowest rated",
    "vote_count.desc" to "Most votes", "original_name.asc" to "Title A–Z", "original_name.desc" to "Title Z–A",
)

/** The discover filters from the web UI: sort, genres, dates, language, runtime, rating, streaming services. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DiscoverFilterSheet(tv: Boolean, current: Map<String, Any?>, onDismiss: () -> Unit, onApply: (Map<String, Any?>) -> Unit) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val dateFrom = if (tv) "firstAirDateGte" else "primaryReleaseDateGte"
    val dateTo = if (tv) "firstAirDateLte" else "primaryReleaseDateLte"
    val region = App.session.streamingRegion

    var sort by remember { mutableStateOf(current["sortBy"]?.toString() ?: "popularity.desc") }
    val genres = remember { mutableStateListOf<Int>().apply { addAll(current["genre"]?.toString()?.split(',')?.mapNotNull { it.toIntOrNull() }.orEmpty()) } }
    var from by remember { mutableStateOf(current[dateFrom]?.toString()?.take(4).orEmpty()) }
    var to by remember { mutableStateOf(current[dateTo]?.toString()?.take(4).orEmpty()) }
    var lang by remember { mutableStateOf(current["language"]?.toString().orEmpty()) }
    var runtime by remember {
        mutableStateOf(
            (current["withRuntimeGte"]?.toString()?.toFloatOrNull() ?: 0f)..(current["withRuntimeLte"]?.toString()?.toFloatOrNull() ?: 400f)
        )
    }
    var rating by remember {
        mutableStateOf((current["voteAverageGte"]?.toString()?.toFloatOrNull() ?: 0f)..(current["voteAverageLte"]?.toString()?.toFloatOrNull() ?: 10f))
    }
    var minVotes by remember { mutableStateOf(current["voteCountGte"]?.toString().orEmpty()) }
    val providers = remember { mutableStateListOf<Int>().apply { addAll(current["watchProviders"]?.toString()?.split('|', ',')?.mapNotNull { it.toIntOrNull() }.orEmpty()) } }

    val genreList = rememberLoad(tv, "genres") { App.api.arr(if (tv) "/genres/tv" else "/genres/movie").objects() }
    val langList = rememberLoad("languages") { App.api.arr("/languages").objects().sortedBy { it.str("english_name") } }
    val providerList = rememberLoad(tv, region, "providers") {
        App.api.arr(if (tv) "/watchproviders/tv" else "/watchproviders/movies", mapOf("watchRegion" to region)).objects()
    }

    ModalBottomSheet(onDismiss, sheetState = sheet) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("Filters", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            Dropdown("Sort by", (if (tv) tvSorts else movieSorts), sort) { sort = it }

            Label("Release year")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    from, { from = it.filter(Char::isDigit).take(4) }, label = { Text("From") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    to, { to = it.filter(Char::isDigit).take(4) }, label = { Text("To") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f),
                )
            }

            genreList.value?.let { list ->
                Label("Genres")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    list.forEach { g ->
                        val id = g.int("id")
                        FilterChip(id in genres, { if (id in genres) genres.remove(id) else genres.add(id) }, label = { Text(g.str("name") ?: "") })
                    }
                }
            }

            langList.value?.let { list ->
                Dropdown(
                    "Original language",
                    listOf("" to "Any") + list.map { (it.str("iso_639_1") ?: "") to (it.str("english_name") ?: it.str("iso_639_1") ?: "") },
                    lang,
                ) { lang = it }
            }

            Label("Runtime: ${runtime.start.toInt()}–${runtime.endInclusive.toInt()} min")
            RangeSlider(runtime, { runtime = it }, valueRange = 0f..400f, steps = 39)

            Label("TMDB rating: ${"%.1f".format(rating.start)}–${"%.1f".format(rating.endInclusive)}")
            RangeSlider(rating, { rating = it }, valueRange = 0f..10f, steps = 19)

            OutlinedTextField(
                minVotes, { minVotes = it.filter(Char::isDigit) }, label = { Text("Minimum number of votes") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(),
            )

            providerList.value?.takeIf { it.isNotEmpty() }?.let { list ->
                Label("Streaming services ($region)")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    list.take(60).forEach { p ->
                        val id = p.int("id")
                        FilterChip(
                            id in providers, { if (id in providers) providers.remove(id) else providers.add(id) },
                            label = { Text(p.str("name") ?: "") },
                            leadingIcon = p.str("logoPath")?.let { logo -> { AsyncImage(App.session.tmdb(logo, "w92"), null, Modifier.size(18.dp)) } },
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                OutlinedButton(
                    onClick = {
                        onApply(current.filterKeys { it in setOf("keywords", "studio", "network") })
                    },
                    modifier = Modifier.weight(1f),
                ) { Text("Reset") }
                Button(
                    onClick = {
                        val m = current.toMutableMap()
                        fun set(k: String, v: Any?) { if (v == null || v.toString().isEmpty()) m.remove(k) else m[k] = v }
                        set("sortBy", sort.takeIf { it != "popularity.desc" })
                        set("genre", genres.joinToString(","))
                        set(dateFrom, from.takeIf { it.length == 4 }?.let { "$it-01-01" })
                        set(dateTo, to.takeIf { it.length == 4 }?.let { "$it-12-31" })
                        set("language", lang)
                        set("withRuntimeGte", runtime.start.toInt().takeIf { it > 0 })
                        set("withRuntimeLte", runtime.endInclusive.toInt().takeIf { it < 400 })
                        set("voteAverageGte", rating.start.takeIf { it > 0f })
                        set("voteAverageLte", rating.endInclusive.takeIf { it < 10f })
                        set("voteCountGte", minVotes)
                        set("watchProviders", providers.joinToString("|"))
                        set("watchRegion", region.takeIf { providers.isNotEmpty() })
                        onApply(m)
                    },
                    modifier = Modifier.weight(1f),
                ) { Text("Apply") }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun Label(text: String) = Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)

/** A read-only dropdown over (value, label) pairs. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Dropdown(label: String, options: List<Pair<String, String>>, selected: String, modifier: Modifier = Modifier, enabled: Boolean = true, onSelect: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(open, { if (enabled) open = it }, modifier) {
        OutlinedTextField(
            value = options.firstOrNull { it.first == selected }?.second ?: selected,
            onValueChange = {}, readOnly = true, enabled = enabled, label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(open) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        )
        ExposedDropdownMenu(open, { open = false }) {
            options.forEach { (v, l) -> DropdownMenuItem(text = { Text(l) }, onClick = { onSelect(v); open = false }) }
        }
    }
}
