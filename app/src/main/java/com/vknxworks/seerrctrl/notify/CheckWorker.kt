package com.vknxworks.seerrctrl.notify

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.toBitmap
import com.vknxworks.seerrctrl.App
import com.vknxworks.seerrctrl.R
import com.vknxworks.seerrctrl.data.ApiException
import com.vknxworks.seerrctrl.data.IssueTypes
import com.vknxworks.seerrctrl.data.MediaStatus
import com.vknxworks.seerrctrl.data.Perm
import com.vknxworks.seerrctrl.data.RequestStatus
import com.vknxworks.seerrctrl.data.bool
import com.vknxworks.seerrctrl.data.int
import com.vknxworks.seerrctrl.data.intOrNull
import com.vknxworks.seerrctrl.data.long
import com.vknxworks.seerrctrl.data.obj
import com.vknxworks.seerrctrl.data.objs
import com.vknxworks.seerrctrl.data.str
import com.vknxworks.seerrctrl.ui.MediaCache
import org.json.JSONObject

/**
 * Runs every 15+ minutes. Compares what the server says now with what it said last time and notifies about
 * the differences. The first run only records the current state, so you don't get a flood of old news.
 */
class CheckWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    private val store = ctx.getSharedPreferences("notify_state", Context.MODE_PRIVATE)

    override suspend fun doWork(): Result {
        App.init(applicationContext)
        if (!App.prefs.isConfigured || !App.settings.notifyEnabled) return Result.success()
        val me = try { App.api.obj("/auth/me") } catch (e: ApiException) {
            return if (e.code == 401 || e.code == 403) Result.success() else Result.retry()
        } catch (_: Exception) { return Result.retry() }

        val myId = me.int("id")
        val perms = me.long("permissions")
        // A different account (or server) than last time: start fresh.
        val who = App.api.baseUrl + "#" + myId
        if (store.getString("who", null) != who) store.edit().clear().putString("who", who).apply()
        val first = !store.getBoolean("primed", false)

        try {
            if (App.settings.notifyMine) myRequests(myId, first)
            if (App.settings.notifyPending && Perm.has(perms, Perm.MANAGE_REQUESTS)) pendingRequests(myId, first)
            if (App.settings.notifyIssues) issues(myId, Perm.has(perms, Perm.MANAGE_ISSUES), first)
        } catch (_: Exception) {
            return Result.retry()
        }
        store.edit().putBoolean("primed", true).apply()
        Notifier.bump()
        return Result.success()
    }

    // ---------- state helpers ----------

    private fun getMap(key: String): JSONObject = try { JSONObject(store.getString(key, "{}")!!) } catch (_: Exception) { JSONObject() }
    private fun putMap(key: String, m: JSONObject) = store.edit().putString(key, m.toString()).apply()
    private fun getSet(key: String): MutableSet<String> = store.getStringSet(key, emptySet())!!.toMutableSet()
    private fun putSet(key: String, s: Set<String>) = store.edit().putStringSet(key, s.toList().takeLast(400).toSet()).apply()

    private suspend fun titleAndPoster(type: String, tmdbId: Int): Pair<String, Bitmap?> {
        val d = try { MediaCache.get(type, tmdbId) } catch (_: Exception) { null }
        val title = d.str("title") ?: d.str("name") ?: if (type == "tv") "A series" else "A movie"
        val poster = try {
            val url = App.session.tmdb(d.str("posterPath"), "w185") ?: d.str("posterPath")?.let { "https://image.tmdb.org/t/p/w185$it" }
            if (url == null) null else {
                val res = SingletonImageLoader.get(applicationContext).execute(ImageRequest.Builder(applicationContext).data(url).build())
                (res as? SuccessResult)?.image?.toBitmap()
            }
        } catch (_: Exception) { null }
        return title to poster
    }

    private fun mediaRoute(type: String, tmdbId: Int) = (if (type == "tv") "tv/" else "movie/") + tmdbId

    // ---------- your requests ----------

    private suspend fun myRequests(myId: Int, first: Boolean) {
        val list = App.api.obj("/request", mapOf("take" to 40, "sort" to "modified", "requestedBy" to myId, "filter" to "all")).objs("results")
        val old = getMap("mine")
        val now = JSONObject()
        for (r in list) {
            val id = r.int("id").toString()
            val media = r.obj("media")
            val is4k = r.bool("is4k")
            val st = RequestStatus.of(r.int("status"))
            val avail = MediaStatus.of(media.intOrNull(if (is4k) "status4k" else "status"))
            val sig = "${st.code}:${avail.code}"
            now.put(id, sig)
            val prev = old.optString(id, "")
            if (first || prev.isEmpty() || prev == sig) continue
            val (pSt, pAv) = prev.split(':').let { (it.getOrNull(0)?.toIntOrNull() ?: 0) to (it.getOrNull(1)?.toIntOrNull() ?: 0) }
            val type = media.str("mediaType") ?: "movie"
            val tmdbId = media.int("tmdbId")
            val (title, poster) = titleAndPoster(type, tmdbId)
            val k = if (is4k) " in 4K" else ""
            val text = when {
                avail == MediaStatus.Available && pAv != MediaStatus.Available.code -> "$title is now available$k"
                avail == MediaStatus.Partial && pAv != MediaStatus.Partial.code -> "$title is partially available$k"
                st != RequestStatus.of(pSt) -> when (st) {
                    RequestStatus.Approved -> "Your request for $title$k was approved"
                    RequestStatus.Declined -> "Your request for $title$k was declined"
                    RequestStatus.Failed -> "Your request for $title$k failed to process"
                    RequestStatus.Completed -> "Your request for $title$k is complete"
                    RequestStatus.Pending -> null
                }
                else -> null
            } ?: continue
            Notifier.post(applicationContext, 10_000 + r.int("id"), Notifier.CH_MINE, title, text, mediaRoute(type, tmdbId), poster)
        }
        // Keep entries for requests that dropped off the first page, so they don't look new later.
        old.keys().forEach { k -> if (!now.has(k)) now.put(k, old.get(k)) }
        putMap("mine", now)
    }

    // ---------- requests waiting for approval ----------

    private suspend fun pendingRequests(myId: Int, first: Boolean) {
        val list = App.api.obj("/request", mapOf("take" to 30, "sort" to "added", "filter" to "pending")).objs("results")
        val seen = getSet("pending")
        for (r in list) {
            val id = r.int("id")
            if (!seen.add(id.toString()) || first) continue
            val by = r.obj("requestedBy")
            if (by.int("id") == myId) continue
            val media = r.obj("media")
            val type = media.str("mediaType") ?: "movie"
            val tmdbId = media.int("tmdbId")
            val (title, poster) = titleAndPoster(type, tmdbId)
            val seasons = r.objs("seasons").map { it.int("seasonNumber") }.sorted()
            val text = "${by.str("displayName") ?: "Someone"} requested ${if (r.bool("is4k")) "4K " else ""}" +
                (if (type == "tv" && seasons.isNotEmpty()) "season${if (seasons.size > 1) "s" else ""} ${seasons.joinToString(", ")}" else if (type == "tv") "this series" else "this movie")
            val nid = 20_000 + id
            Notifier.post(
                applicationContext, nid, Notifier.CH_PENDING, title, text, mediaRoute(type, tmdbId), poster,
                actions = listOf(
                    action(nid, id, ActionReceiver.APPROVE, "Approve", title),
                    action(nid, id, ActionReceiver.DECLINE, "Decline", title),
                ),
            )
        }
        putSet("pending", seen)
    }

    private fun action(nid: Int, requestId: Int, verb: String, label: String, title: String): NotificationCompat.Action {
        val i = Intent(applicationContext, ActionReceiver::class.java)
            .setAction(verb)
            .putExtra(ActionReceiver.EXTRA_REQUEST, requestId)
            .putExtra(ActionReceiver.EXTRA_NOTIFICATION, nid)
            .putExtra(ActionReceiver.EXTRA_TITLE, title)
        val pi = PendingIntent.getBroadcast(
            applicationContext, nid * 2 + if (verb == ActionReceiver.APPROVE) 0 else 1, i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        // Changing a request needs the phone unlocked first (the device lock, not SeerrCtrl's own PIN).
        return NotificationCompat.Action.Builder(if (verb == ActionReceiver.APPROVE) R.drawable.ic_stat_check else R.drawable.ic_stat_close, label, pi)
            .setAuthenticationRequired(true)
            .build()
    }

    // ---------- issues ----------

    private suspend fun issues(myId: Int, manager: Boolean, first: Boolean) {
        if (manager) {
            val open = App.api.obj("/issue", mapOf("take" to 30, "sort" to "added", "filter" to "open")).objs("results")
            val seen = getSet("issues")
            for (i in open) {
                val id = i.int("id")
                if (!seen.add(id.toString()) || first) continue
                if (i.obj("createdBy").int("id") == myId) continue
                val media = i.obj("media")
                val (title, poster) = titleAndPoster(media.str("mediaType") ?: "movie", media.int("tmdbId"))
                val msg = i.objs("comments").firstOrNull()?.str("message")
                Notifier.post(
                    applicationContext, 30_000 + id, Notifier.CH_ISSUES,
                    "${IssueTypes.name(i.int("issueType"))} issue: $title",
                    "${i.obj("createdBy").str("displayName") ?: "Someone"}: ${msg ?: "reported a problem"}",
                    "issue/$id", poster,
                )
            }
            putSet("issues", seen)
        }

        // Issues you reported: replies and status changes.
        val mine = App.api.obj("/issue", mapOf("take" to 30, "sort" to "modified", "filter" to "all", "createdBy" to myId)).objs("results")
        val old = getMap("myissues")
        val now = JSONObject()
        for (i in mine) {
            val id = i.int("id")
            val sig = "${i.int("status")}:${i.objs("comments").size}"
            now.put(id.toString(), sig)
            val prev = old.optString(id.toString(), "")
            if (first || prev.isEmpty() || prev == sig) continue
            val (pSt, pCount) = prev.split(':').let { (it.getOrNull(0)?.toIntOrNull() ?: 0) to (it.getOrNull(1)?.toIntOrNull() ?: 0) }
            val media = i.obj("media")
            val (title, poster) = titleAndPoster(media.str("mediaType") ?: "movie", media.int("tmdbId"))
            val text = when {
                i.int("status") == 2 && pSt != 2 -> "Your issue was marked as resolved"
                i.int("status") == 1 && pSt == 2 -> "Your issue was reopened"
                i.objs("comments").size > pCount -> {
                    // The list doesn't say who wrote a comment; the issue itself does.
                    val full = try { App.api.obj("/issue/$id") } catch (_: Exception) { null }
                    val last = full.objs("comments").lastOrNull()
                    if (last != null && last.obj("user").int("id") == myId) null
                    else "${last.obj("user").str("displayName") ?: "Someone"} replied: ${last.str("message") ?: ""}"
                }
                else -> null
            } ?: continue
            Notifier.post(applicationContext, 40_000 + id, Notifier.CH_ISSUES, title, text, "issue/$id", poster)
        }
        old.keys().forEach { k -> if (!now.has(k)) now.put(k, old.get(k)) }
        putMap("myissues", now)
    }
}
