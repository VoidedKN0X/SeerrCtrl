package com.vknxworks.seerrctrl.data

import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

// Small null-safe helpers over org.json, so screens read like `movie.str("title")`.

fun JSONObject?.str(key: String): String? =
    if (this == null || !has(key) || isNull(key)) null else optString(key).takeIf { it.isNotEmpty() }

fun JSONObject?.int(key: String, def: Int = 0): Int =
    if (this == null || !has(key) || isNull(key)) def else optInt(key, def)

fun JSONObject?.intOrNull(key: String): Int? =
    if (this == null || !has(key) || isNull(key)) null else (opt(key) as? Number)?.toInt() ?: optString(key).toIntOrNull()

fun JSONObject?.long(key: String, def: Long = 0): Long =
    if (this == null || !has(key) || isNull(key)) def else optLong(key, def)

fun JSONObject?.dbl(key: String): Double? =
    if (this == null || !has(key) || isNull(key)) null else optDouble(key).takeUnless { it.isNaN() }

fun JSONObject?.bool(key: String, def: Boolean = false): Boolean =
    if (this == null || !has(key) || isNull(key)) def else optBoolean(key, def)

fun JSONObject?.obj(key: String): JSONObject? =
    if (this == null || !has(key) || isNull(key)) null else optJSONObject(key)

fun JSONObject?.arr(key: String): JSONArray? =
    if (this == null || !has(key) || isNull(key)) null else optJSONArray(key)

fun JSONObject?.objs(key: String): List<JSONObject> = arr(key).objects()

fun JSONArray?.objects(): List<JSONObject> {
    if (this == null) return emptyList()
    return (0 until length()).mapNotNull { optJSONObject(it) }
}

fun JSONArray?.strings(): List<String> {
    if (this == null) return emptyList()
    return (0 until length()).mapNotNull { if (isNull(it)) null else opt(it)?.toString() }
}

fun JSONArray?.ints(): List<Int> {
    if (this == null) return emptyList()
    return (0 until length()).mapNotNull { (opt(it) as? Number)?.toInt() }
}

fun jsonOf(vararg pairs: Pair<String, Any?>): JSONObject = JSONObject().apply {
    pairs.forEach { (k, v) -> put(k, v ?: JSONObject.NULL) }
}

fun JSONObject.deepCopy(): JSONObject = JSONObject(toString())

// ---------- dates ----------

private val dayFmt = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
private val dayTimeFmt = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)

/** "2024-05-01" or an ISO timestamp → "1 May 2024". */
fun prettyDate(raw: String?): String? {
    if (raw.isNullOrBlank()) return null
    return try {
        if (raw.length <= 10) LocalDate.parse(raw).format(dayFmt)
        else Instant.parse(raw).atZone(ZoneId.systemDefault()).format(dayFmt)
    } catch (_: Exception) { raw.take(10) }
}

fun prettyDateTime(raw: String?): String? {
    if (raw.isNullOrBlank()) return null
    return try { Instant.parse(raw).atZone(ZoneId.systemDefault()).format(dayTimeFmt) } catch (_: Exception) { raw }
}

/** "3 hours ago" style text for an ISO timestamp. */
fun timeAgo(raw: String?): String? {
    if (raw.isNullOrBlank()) return null
    val t = try { Instant.parse(raw) } catch (_: Exception) { return raw }
    val s = (System.currentTimeMillis() - t.toEpochMilli()) / 1000
    return when {
        s < 60 -> "just now"
        s < 3600 -> "${s / 60} min ago"
        s < 86400 -> "${s / 3600} h ago"
        s < 86400 * 30 -> "${s / 86400} d ago"
        else -> prettyDate(raw)
    }
}

fun year(raw: String?): String? = raw?.take(4)?.takeIf { it.length == 4 && it.all(Char::isDigit) }

fun humanBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val units = listOf("KB", "MB", "GB", "TB", "PB")
    var v = bytes / 1024.0
    var i = 0
    while (v >= 1024 && i < units.lastIndex) { v /= 1024; i++ }
    return String.format("%.1f %s", v, units[i])
}

fun runtime(minutes: Int): String? = when {
    minutes <= 0 -> null
    minutes < 60 -> "${minutes}m"
    else -> "${minutes / 60}h ${minutes % 60}m"
}

/** "applicationTitle" → "Application title". */
fun humanize(key: String): String {
    val spaced = key.replace(Regex("([a-z0-9])([A-Z])"), "$1 $2").replace('_', ' ')
    val fixed = spaced.split(' ').joinToString(" ") { w ->
        when (w.lowercase()) {
            "url" -> "URL"; "api" -> "API"; "ssl" -> "SSL"; "ip" -> "IP"; "id" -> "ID"; "dns" -> "DNS"
            "tmdb" -> "TMDB"; "imdb" -> "IMDb"; "tvdb" -> "TVDB"; "csrf" -> "CSRF"; "smtp" -> "SMTP"
            "pgp" -> "PGP"; "4k" -> "4K"; "ipv4" -> "IPv4"; "dnscache" -> "DNS cache"
            else -> w.lowercase()
        }
    }
    return fixed.replaceFirstChar { it.uppercase() }
}
