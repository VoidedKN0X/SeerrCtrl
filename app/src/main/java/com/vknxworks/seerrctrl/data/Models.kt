package com.vknxworks.seerrctrl.data

/** Seerr permission bits (server/lib/permissions.ts). */
object Perm {
    const val ADMIN = 2L
    const val MANAGE_SETTINGS = 4L
    const val MANAGE_USERS = 8L
    const val MANAGE_REQUESTS = 16L
    const val REQUEST = 32L
    const val VOTE = 64L
    const val AUTO_APPROVE = 128L
    const val AUTO_APPROVE_MOVIE = 256L
    const val AUTO_APPROVE_TV = 512L
    const val REQUEST_4K = 1024L
    const val REQUEST_4K_MOVIE = 2048L
    const val REQUEST_4K_TV = 4096L
    const val REQUEST_ADVANCED = 8192L
    const val REQUEST_VIEW = 16384L
    const val AUTO_APPROVE_4K = 32768L
    const val AUTO_APPROVE_4K_MOVIE = 65536L
    const val AUTO_APPROVE_4K_TV = 131072L
    const val REQUEST_MOVIE = 262144L
    const val REQUEST_TV = 524288L
    const val MANAGE_ISSUES = 1048576L
    const val VIEW_ISSUES = 2097152L
    const val CREATE_ISSUES = 4194304L
    const val AUTO_REQUEST = 8388608L
    const val AUTO_REQUEST_MOVIE = 16777216L
    const val AUTO_REQUEST_TV = 33554432L
    const val RECENT_VIEW = 67108864L
    const val WATCHLIST_VIEW = 134217728L
    const val MANAGE_BLOCKLIST = 268435456L
    const val VIEW_BLOCKLIST = 1073741824L

    fun has(value: Long, vararg perms: Long, any: Boolean = false): Boolean {
        if (value and ADMIN != 0L) return true
        if (perms.isEmpty()) return true
        return if (any) perms.any { value and it != 0L } else perms.all { value and it != 0L }
    }

    /** One editable permission, with the ones nested under it (same tree as the web UI). */
    data class Node(val bit: Long, val name: String, val desc: String, val children: List<Node> = emptyList())

    val tree: List<Node> = listOf(
        Node(ADMIN, "Admin", "Full administrator access. Bypasses all other permission checks."),
        Node(MANAGE_SETTINGS, "Manage settings", "Modify Seerr's settings. Users need this to grant it to others."),
        Node(MANAGE_USERS, "Manage users", "Manage users. Users with this can't change users with Admin, or grant it."),
        Node(MANAGE_REQUESTS, "Manage requests", "Approve, decline and edit media requests.", listOf(
            Node(REQUEST_ADVANCED, "Advanced requests", "Pick server, quality profile and folder when requesting."),
            Node(REQUEST_VIEW, "View requests", "See requests made by other users."),
            Node(RECENT_VIEW, "View recently added", "See the list of recently added media."),
            Node(WATCHLIST_VIEW, "View watchlists", "See other users' watchlists."),
        )),
        Node(REQUEST, "Request", "Request non-4K media.", listOf(
            Node(REQUEST_MOVIE, "Request movies", "Request non-4K movies."),
            Node(REQUEST_TV, "Request series", "Request non-4K series."),
        )),
        Node(AUTO_APPROVE, "Auto-approve", "Approve all non-4K requests automatically.", listOf(
            Node(AUTO_APPROVE_MOVIE, "Auto-approve movies", "Approve non-4K movie requests automatically."),
            Node(AUTO_APPROVE_TV, "Auto-approve series", "Approve non-4K series requests automatically."),
        )),
        Node(AUTO_REQUEST, "Auto-request", "Request non-4K media on the user's watchlist automatically.", listOf(
            Node(AUTO_REQUEST_MOVIE, "Auto-request movies", "Request watchlisted movies automatically."),
            Node(AUTO_REQUEST_TV, "Auto-request series", "Request watchlisted series automatically."),
        )),
        Node(REQUEST_4K, "Request 4K", "Request 4K media.", listOf(
            Node(REQUEST_4K_MOVIE, "Request 4K movies", "Request 4K movies."),
            Node(REQUEST_4K_TV, "Request 4K series", "Request 4K series."),
        )),
        Node(AUTO_APPROVE_4K, "Auto-approve 4K", "Approve all 4K requests automatically.", listOf(
            Node(AUTO_APPROVE_4K_MOVIE, "Auto-approve 4K movies", "Approve 4K movie requests automatically."),
            Node(AUTO_APPROVE_4K_TV, "Auto-approve 4K series", "Approve 4K series requests automatically."),
        )),
        Node(MANAGE_ISSUES, "Manage issues", "Manage media issues.", listOf(
            Node(CREATE_ISSUES, "Report issues", "Report media issues."),
            Node(VIEW_ISSUES, "View issues", "See issues reported by other users."),
        )),
        Node(MANAGE_BLOCKLIST, "Manage blocklist", "Add and remove blocklisted media.", listOf(
            Node(VIEW_BLOCKLIST, "View blocklist", "See blocklisted media."),
        )),
    )
}

/** Media availability (MediaStatus). */
enum class MediaStatus(val code: Int, val label: String) {
    Unknown(1, "Not requested"),
    Pending(2, "Pending"),
    Processing(3, "Requested"),
    Partial(4, "Partially available"),
    Available(5, "Available"),
    Blocklisted(6, "Blocklisted"),
    Deleted(7, "Deleted");

    companion object {
        fun of(code: Int?): MediaStatus = entries.firstOrNull { it.code == code } ?: Unknown
    }
}

/** Request state (MediaRequestStatus). */
enum class RequestStatus(val code: Int, val label: String) {
    Pending(1, "Pending approval"),
    Approved(2, "Approved"),
    Declined(3, "Declined"),
    Failed(4, "Failed"),
    Completed(5, "Completed");

    companion object {
        fun of(code: Int?): RequestStatus = entries.firstOrNull { it.code == code } ?: Pending
    }
}

object IssueTypes {
    val all = listOf(1 to "Video", 2 to "Audio", 3 to "Subtitles", 4 to "Other")
    fun name(t: Int) = all.firstOrNull { it.first == t }?.second ?: "Other"
}

object UserTypes {
    fun name(t: Int) = when (t) { 1 -> "Plex user"; 2 -> "Local user"; 3 -> "Jellyfin user"; 4 -> "Emby user"; else -> "User" }
}

enum class MediaServer(val code: Int, val label: String) {
    Plex(1, "Plex"), Jellyfin(2, "Jellyfin"), Emby(3, "Emby"), None(4, "Not configured");

    companion object {
        fun of(code: Int?): MediaServer = entries.firstOrNull { it.code == code } ?: None
    }
}

/** A tile in a discover row / grid: movie, series, person or collection. */
data class Tile(
    val id: Int,
    val type: String, // movie | tv | person | collection
    val title: String,
    val image: String?,
    val year: String?,
    val status: MediaStatus,
    val status4k: MediaStatus,
    val subtitle: String? = null,
)

fun org.json.JSONObject.toTile(): Tile? {
    val type = str("mediaType") ?: when {
        has("profilePath") -> "person"
        has("firstAirDate") || has("name") && !has("title") -> "tv"
        else -> "movie"
    }
    val id = intOrNull("id") ?: return null
    val info = obj("mediaInfo")
    return Tile(
        id = id,
        type = type,
        title = str("title") ?: str("name") ?: str("originalTitle") ?: str("originalName") ?: "Untitled",
        image = str("posterPath") ?: str("profilePath"),
        year = year(str("releaseDate") ?: str("firstAirDate")),
        status = MediaStatus.of(info.intOrNull("status")),
        status4k = MediaStatus.of(info.intOrNull("status4k")),
        subtitle = str("character") ?: str("job"),
    )
}
