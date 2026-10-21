package com.vknxworks.seerrctrl.data

import com.vknxworks.seerrctrl.App
import com.vknxworks.seerrctrl.R

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
        Node(ADMIN, App.str(R.string.admin), App.str(R.string.full_administrator_access_bypasses)),
        Node(MANAGE_SETTINGS, App.str(R.string.manage_settings), App.str(R.string.modify_seerr_s_settings_users_need)),
        Node(MANAGE_USERS, App.str(R.string.manage_users), App.str(R.string.manage_users_users_with_this_can_t)),
        Node(MANAGE_REQUESTS, App.str(R.string.manage_requests), App.str(R.string.approve_decline_and_edit_media), listOf(
            Node(REQUEST_ADVANCED, App.str(R.string.advanced_requests), App.str(R.string.pick_server_quality_profile_and)),
            Node(REQUEST_VIEW, App.str(R.string.view_requests), App.str(R.string.see_requests_made_by_other_users)),
            Node(RECENT_VIEW, App.str(R.string.view_recently_added), App.str(R.string.see_the_list_of_recently_added_media)),
            Node(WATCHLIST_VIEW, App.str(R.string.view_watchlists), App.str(R.string.see_other_users_watchlists)),
        )),
        Node(REQUEST, App.str(R.string.request), App.str(R.string.request_non_4k_media), listOf(
            Node(REQUEST_MOVIE, App.str(R.string.request_movies), App.str(R.string.request_non_4k_movies)),
            Node(REQUEST_TV, App.str(R.string.request_series), App.str(R.string.request_non_4k_series)),
        )),
        Node(AUTO_APPROVE, App.str(R.string.auto_approve), App.str(R.string.approve_all_non_4k_requests), listOf(
            Node(AUTO_APPROVE_MOVIE, App.str(R.string.auto_approve_movies), App.str(R.string.approve_non_4k_movie_requests)),
            Node(AUTO_APPROVE_TV, App.str(R.string.auto_approve_series), App.str(R.string.approve_non_4k_series_requests)),
        )),
        Node(AUTO_REQUEST, App.str(R.string.auto_request), App.str(R.string.request_non_4k_media_on_the_user_s), listOf(
            Node(AUTO_REQUEST_MOVIE, App.str(R.string.auto_request_movies), App.str(R.string.request_watchlisted_movies)),
            Node(AUTO_REQUEST_TV, App.str(R.string.auto_request_series), App.str(R.string.request_watchlisted_series)),
        )),
        Node(REQUEST_4K, App.str(R.string.request_4k), App.str(R.string.request_4k_media), listOf(
            Node(REQUEST_4K_MOVIE, App.str(R.string.request_4k_movies_2), App.str(R.string.request_4k_movies)),
            Node(REQUEST_4K_TV, App.str(R.string.request_4k_series_2), App.str(R.string.request_4k_series)),
        )),
        Node(AUTO_APPROVE_4K, App.str(R.string.auto_approve_4k), App.str(R.string.approve_all_4k_requests), listOf(
            Node(AUTO_APPROVE_4K_MOVIE, App.str(R.string.auto_approve_4k_movies), App.str(R.string.approve_4k_movie_requests)),
            Node(AUTO_APPROVE_4K_TV, App.str(R.string.auto_approve_4k_series), App.str(R.string.approve_4k_series_requests)),
        )),
        Node(MANAGE_ISSUES, App.str(R.string.manage_issues), App.str(R.string.manage_media_issues), listOf(
            Node(CREATE_ISSUES, App.str(R.string.report_issues), App.str(R.string.report_media_issues)),
            Node(VIEW_ISSUES, App.str(R.string.view_issues), App.str(R.string.see_issues_reported_by_other_users)),
        )),
        Node(MANAGE_BLOCKLIST, App.str(R.string.manage_blocklist), App.str(R.string.add_and_remove_blocklisted_media), listOf(
            Node(VIEW_BLOCKLIST, App.str(R.string.view_blocklist), App.str(R.string.see_blocklisted_media)),
        )),
    )
}

/** Media availability (MediaStatus). */
enum class MediaStatus(val code: Int, val label: String) {
    Unknown(1, App.str(R.string.not_requested)),
    Pending(2, App.str(R.string.pending)),
    Processing(3, App.str(R.string.requested)),
    Partial(4, App.str(R.string.partially_available)),
    Available(5, App.str(R.string.available)),
    Blocklisted(6, App.str(R.string.blocklisted)),
    Deleted(7, App.str(R.string.deleted));

    companion object {
        fun of(code: Int?): MediaStatus = entries.firstOrNull { it.code == code } ?: Unknown
    }
}

/** Request state (MediaRequestStatus). */
enum class RequestStatus(val code: Int, val label: String) {
    Pending(1, App.str(R.string.pending_approval)),
    Approved(2, App.str(R.string.approved)),
    Declined(3, App.str(R.string.declined)),
    Failed(4, App.str(R.string.failed)),
    Completed(5, App.str(R.string.completed));

    companion object {
        fun of(code: Int?): RequestStatus = entries.firstOrNull { it.code == code } ?: Pending
    }
}

object IssueTypes {
    val all = listOf(1 to App.str(R.string.video), 2 to App.str(R.string.audio), 3 to App.str(R.string.subtitles), 4 to App.str(R.string.other))
    fun name(t: Int) = all.firstOrNull { it.first == t }?.second ?: App.str(R.string.other)
}

object UserTypes {
    fun name(t: Int) = when (t) { 1 -> App.str(R.string.plex_user); 2 -> App.str(R.string.local_user); 3 -> App.str(R.string.jellyfin_user); 4 -> App.str(R.string.emby_user); else -> App.str(R.string.user) }
}

enum class MediaServer(val code: Int, val label: String) {
    Plex(1, App.str(R.string.plex)), Jellyfin(2, App.str(R.string.jellyfin)), Emby(3, App.str(R.string.emby)), None(4, App.str(R.string.not_configured));

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
        title = str("title") ?: str("name") ?: str("originalTitle") ?: str("originalName") ?: App.str(R.string.untitled),
        image = str("posterPath") ?: str("profilePath"),
        year = year(str("releaseDate") ?: str("firstAirDate")),
        status = MediaStatus.of(info.intOrNull("status")),
        status4k = MediaStatus.of(info.intOrNull("status4k")),
        subtitle = str("character") ?: str("job"),
    )
}
