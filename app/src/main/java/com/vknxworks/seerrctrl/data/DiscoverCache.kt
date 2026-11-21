package com.vknxworks.seerrctrl.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * The Discover page as it was last loaded, kept on the phone so the tab has something to show the moment
 * it opens; the fresh page replaces it a moment later.
 *
 * Only the public part is kept: rows of TMDB titles (trending, popular, upcoming, genres, studios,
 * networks, the server's custom sliders) with their titles and poster paths. Nothing about the account or
 * the library is written: no requests, no watchlist, no "recently added", and no availability or request
 * status on the tiles. The file is in the app's private no-backup storage and is deleted on sign-out and
 * when the session ends, so it always belongs to the account that is signed in.
 */
object DiscoverCache {
    private const val VERSION = 1
    private const val MAX_BYTES = 2L * 1024 * 1024

    /** One saved row. [kind]: "tiles", "genres" or "logos". */
    class Row(
        val key: String,
        val kind: String,
        val title: String,
        val tv: Boolean = false,
        val tiles: List<Tile> = emptyList(),
        val morePath: String? = null,
        val moreParams: Map<String, String> = emptyMap(),
        val genres: List<JSONObject> = emptyList(),
    )

    private fun file(ctx: Context) = File(ctx.noBackupFilesDir, "discover.json")

    fun encode(server: String, rows: List<Row>): String = JSONObject().apply {
        put("v", VERSION)
        put("server", server)
        put("rows", JSONArray().apply {
            rows.forEach { r ->
                put(JSONObject().apply {
                    put("key", r.key); put("kind", r.kind); put("title", r.title); put("tv", r.tv)
                    if (r.morePath != null) {
                        put("morePath", r.morePath)
                        put("moreParams", JSONObject().apply { r.moreParams.forEach { (k, v) -> put(k, v) } })
                    }
                    put("tiles", JSONArray().apply {
                        r.tiles.forEach { t ->
                            put(JSONObject().apply {
                                put("id", t.id); put("type", t.type); put("title", t.title)
                                t.image?.let { put("image", it) }
                                t.year?.let { put("year", it) }
                            })
                        }
                    })
                    put("genres", JSONArray().apply { r.genres.forEach { put(it) } })
                })
            }
        })
    }.toString()

    /** The rows in [text] when it was saved for this [server]; otherwise nothing. */
    fun decode(text: String, server: String): List<Row> {
        val j = try { JSONObject(text) } catch (_: Exception) { return emptyList() }
        if (j.int("v") != VERSION || server.isEmpty() || j.str("server") != server) return emptyList()
        return j.objs("rows").mapNotNull { r ->
            val key = r.str("key") ?: return@mapNotNull null
            val kind = r.str("kind") ?: return@mapNotNull null
            val params = r.obj("moreParams")
            Row(
                key = key,
                kind = kind,
                title = r.str("title").orEmpty(),
                tv = r.bool("tv"),
                tiles = r.objs("tiles").mapNotNull { t ->
                    val id = t.intOrNull("id") ?: return@mapNotNull null
                    Tile(id, t.str("type") ?: "movie", t.str("title").orEmpty(), t.str("image"), t.str("year"), MediaStatus.Unknown, MediaStatus.Unknown)
                },
                morePath = r.str("morePath"),
                moreParams = params?.keys()?.asSequence()?.associateWith { params.optString(it) }.orEmpty(),
                genres = r.objs("genres"),
            )
        }
    }

    fun read(ctx: Context, server: String): List<Row> = try {
        val f = file(ctx)
        if (!f.isFile || f.length() > MAX_BYTES) emptyList() else decode(f.readText(), server)
    } catch (_: Exception) { emptyList() }

    fun write(ctx: Context, server: String, rows: List<Row>) {
        try {
            val text = encode(server, rows)
            if (text.length > MAX_BYTES) return
            val tmp = File(ctx.noBackupFilesDir, "discover.json.tmp")
            tmp.writeText(text)
            if (!tmp.renameTo(file(ctx))) tmp.delete()
        } catch (_: Exception) {}
    }

    fun clear(ctx: Context) {
        runCatching { file(ctx).delete() }
        runCatching { File(ctx.noBackupFilesDir, "discover.json.tmp").delete() }
    }
}
