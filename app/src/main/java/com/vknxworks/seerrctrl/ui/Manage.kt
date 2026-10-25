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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material.icons.outlined.ReportProblem
import com.vknxworks.seerrctrl.ui.components.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import com.vknxworks.seerrctrl.ui.components.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import com.vknxworks.seerrctrl.ui.components.ModalBottomSheet
import com.vknxworks.seerrctrl.ui.components.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vknxworks.seerrctrl.App
import com.vknxworks.seerrctrl.data.IssueTypes
import com.vknxworks.seerrctrl.data.MediaStatus
import com.vknxworks.seerrctrl.data.Perm
import com.vknxworks.seerrctrl.data.int
import com.vknxworks.seerrctrl.data.intOrNull
import com.vknxworks.seerrctrl.data.jsonOf
import com.vknxworks.seerrctrl.data.obj
import com.vknxworks.seerrctrl.data.objs
import com.vknxworks.seerrctrl.data.str
import kotlinx.coroutines.launch
import org.json.JSONObject
import androidx.compose.ui.res.stringResource
import com.vknxworks.seerrctrl.R

/** Report a playback problem (video, audio, subtitles, other), optionally for a season/episode. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReportIssueDialog(details: JSONObject, tv: Boolean, onDismiss: () -> Unit, onCreated: (Int) -> Unit) {
    val scope = rememberCoroutineScope()
    var type by remember { mutableStateOf(1) }
    var season by remember { mutableStateOf("0") }
    var episode by remember { mutableStateOf("0") }
    var message by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val mediaId = details.obj("mediaInfo").int("id")
    val seasons = details.objs("seasons").filter { it.int("seasonNumber") > 0 || App.session.specialEpisodes }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.ReportProblem, null) },
        title = { Text(stringResource(R.string.report_an_issue)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(details.str("title") ?: details.str("name") ?: "", fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IssueTypes.all.forEach { (id, name) -> FilterChip(type == id, { type = id }, label = { Text(name) }) }
                }
                if (tv) {
                    Dropdown(
                        stringResource(R.string.season_3),
                        listOf("0" to App.str(R.string.all_seasons)) + seasons.map { it.int("seasonNumber").toString() to (it.str("name") ?: App.str(R.string.season_2, it.int("seasonNumber"))) },
                        season,
                    ) { season = it; episode = "0" }
                    val count = seasons.firstOrNull { it.int("seasonNumber").toString() == season }?.int("episodeCount") ?: 0
                    if (season != "0" && count > 0) {
                        Dropdown(stringResource(R.string.episode_2), listOf("0" to App.str(R.string.all_episodes)) + (1..count).map { "$it" to App.str(R.string.episode, it) }, episode) { episode = it }
                    }
                }
                OutlinedTextField(
                    message, { message = it }, label = { Text(stringResource(R.string.what_s_wrong)) }, minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !busy && message.isNotBlank(),
                onClick = {
                    scope.launch {
                        busy = true; error = null
                        try {
                            val r = App.api.post(
                                "/issue",
                                jsonOf(
                                    "issueType" to type, "message" to message.trim(), "mediaId" to mediaId,
                                    "problemSeason" to season.toInt(), "problemEpisode" to episode.toInt(),
                                ),
                            ) as? JSONObject
                            onCreated(r.int("id"))
                        } catch (e: Exception) { error = e.message }
                        busy = false
                    }
                },
            ) { if (busy) CircularProgressIndicator(strokeWidth = 2.dp) else Text(stringResource(R.string.submit)) }
        },
        dismissButton = { TextButton(onDismiss) { Text(stringResource(R.string.lock_cancel)) } },
    )
}

/** Admin tools for a title: mark availability, open in Radarr/Sonarr, remove files, clear data, blocklist. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageMediaSheet(details: JSONObject, type: String, onDismiss: () -> Unit, onChanged: () -> Unit) {
    val s = App.session
    val context = LocalContext.current
    val act = rememberActions()
    val info = details.obj("mediaInfo")
    val mediaId = info.intOrNull("id")
    val tv = type == "tv"
    val has4k = if (tv) s.series4k else s.movie4k
    var confirm by remember { mutableStateOf<Triple<String, String, suspend () -> Unit>?>(null) }
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val title = details.str("title") ?: details.str("name") ?: ""

    fun done(msg: String, block: suspend () -> Unit) = act.run(msg, onOk = {
        MediaCache.invalidate(type, details.int("id")); onChanged(); onDismiss()
    }, block = block)

    ModalBottomSheet(onDismiss, sheetState = sheet) {
        Column(Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding().padding(bottom = 16.dp)) {
            Text(stringResource(R.string.manage_2, title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))

            if (s.can(Perm.MANAGE_REQUESTS) && mediaId != null) {
                for (fourK in if (has4k) listOf(false, true) else listOf(false)) {
                    val label = if (fourK) " (4K)" else ""
                    val st = MediaStatus.of(info.intOrNull(if (fourK) "status4k" else "status"))
                    SheetHeader(stringResource(R.string.availability, label, st.label))
                    (if (fourK) info.str("serviceUrl4k") else info.str("serviceUrl"))?.let { url ->
                        SheetItem(Icons.Outlined.OpenInNew, stringResource(R.string.open_in, if (tv) "Sonarr" else "Radarr", label)) { openUrl(context, url) }
                    }
                    if (st != MediaStatus.Available) {
                        SheetItem(Icons.Outlined.CheckCircle, stringResource(R.string.mark_as_available, label)) {
                            done(App.str(R.string.marked_as_available)) { App.api.post("/media/$mediaId/available", jsonOf("is4k" to fourK)) }
                        }
                    }
                    if (tv && st != MediaStatus.Partial) {
                        SheetItem(Icons.Outlined.CheckCircle, stringResource(R.string.mark_as_partially_available, label)) {
                            done(App.str(R.string.marked_as_partially_available)) { App.api.post("/media/$mediaId/partial", jsonOf("is4k" to fourK)) }
                        }
                    }
                    if (st == MediaStatus.Available || st == MediaStatus.Partial) {
                        SheetItem(Icons.Outlined.HourglassEmpty, stringResource(R.string.mark_as_not_available, label)) {
                            done(App.str(R.string.status_reset)) { App.api.post("/media/$mediaId/unknown", jsonOf("is4k" to fourK)) }
                        }
                    }
                    val sid = info.intOrNull(if (fourK) "serviceId4k" else "serviceId")
                    if (sid != null || info.intOrNull(if (fourK) "externalServiceId4k" else "externalServiceId") != null) {
                        SheetItem(Icons.Outlined.RemoveCircleOutline, stringResource(R.string.remove_from, if (tv) "Sonarr" else "Radarr", label), danger = true) {
                            confirm = Triple(
                                App.str(R.string.remove_from_2, if (tv) "Sonarr" else "Radarr"),
                                App.str(R.string.this_deletes_and_its_files_from, title, if (tv) "Sonarr" else "Radarr", label),
                            ) { App.api.delete("/media/$mediaId/file", query = mapOf("is4k" to fourK)) }
                        }
                    }
                }
                SheetHeader(stringResource(R.string.data))
                SheetItem(Icons.Outlined.DeleteSweep, stringResource(R.string.clear_data), stringResource(R.string.removes_requests_status_and_issues), danger = true) {
                    confirm = Triple(App.str(R.string.clear_data_2), App.str(R.string.all_of_seerr_s_data_for_requests, title)) {
                        App.api.delete("/media/$mediaId")
                    }
                }
            }
            if (s.can(Perm.MANAGE_BLOCKLIST)) {
                SheetHeader(stringResource(R.string.blocklist))
                SheetItem(Icons.Outlined.Block, stringResource(R.string.add_to_blocklist), stringResource(R.string.hides_it_from_discovery_and_blocks), danger = true) {
                    done(App.str(R.string.added_to_blocklist)) {
                        App.api.post("/blocklist", jsonOf("tmdbId" to details.int("id"), "mediaType" to type, "title" to title, "user" to s.userId))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    confirm?.let { (t, body, block) ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            icon = { Icon(Icons.Outlined.DeleteForever, null) },
            title = { Text(t) },
            text = { Text(body) },
            confirmButton = { TextButton({ confirm = null; done(App.str(R.string.lock_done), block) }) { Text(stringResource(R.string.confirm), color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton({ confirm = null }) { Text(stringResource(R.string.lock_cancel)) } },
        )
    }
}

@Composable
fun SheetHeader(text: String) {
    Text(
        text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 20.dp, top = 14.dp, bottom = 4.dp),
    )
}

@Composable
fun SheetItem(icon: ImageVector, title: String, subtitle: String? = null, danger: Boolean = false, onClick: () -> Unit) {
    val color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    ListItem(
        leadingContent = { Icon(icon, null, tint = color) },
        headlineContent = { Text(title, color = color) },
        supportingContent = subtitle?.let { { Text(it) } },
        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
        modifier = Modifier.clickableRow(onClick),
    )
}

