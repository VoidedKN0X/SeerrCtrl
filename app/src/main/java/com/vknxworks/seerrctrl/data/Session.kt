package com.vknxworks.seerrctrl.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONObject

/** The signed-in user and the server's public settings, observable from Compose. */
class Session(private val api: SeerrApi) {
    var me by mutableStateOf<JSONObject?>(null)
        private set
    var public by mutableStateOf(JSONObject())
        private set

    val userId: Int get() = me.int("id")
    val permissions: Long get() = me.long("permissions")
    val displayName: String get() = me.str("displayName") ?: me.str("username") ?: me.str("email") ?: "You"

    fun can(vararg p: Long, any: Boolean = false): Boolean = Perm.has(permissions, *p, any = any)
    val isAdmin get() = can(Perm.ADMIN)

    val appTitle: String get() = public.str("applicationTitle") ?: "Seerr"
    val mediaServer: MediaServer get() = MediaServer.of(public.intOrNull("mediaServerType"))
    val movie4k: Boolean get() = public.bool("movie4kEnabled")
    val series4k: Boolean get() = public.bool("series4kEnabled")
    val partialRequests: Boolean get() = public.bool("partialRequestsEnabled", true)
    val specialEpisodes: Boolean get() = public.bool("enableSpecialEpisodes")
    val cacheImages: Boolean get() = public.bool("cacheImages")
    val streamingRegion: String get() = me.obj("settings").str("streamingRegion") ?: public.str("streamingRegion") ?: "US"

    suspend fun loadPublic() { public = api.obj("/settings/public") }

    suspend fun refresh() {
        loadPublic()
        me = api.obj("/auth/me")
    }

    fun setMe(user: JSONObject) { me = user }

    fun clear() { me = null }

    // ---------- images ----------

    /** TMDB poster/backdrop/profile path → URL, through Seerr's image cache when it's on. */
    fun tmdb(path: String?, size: String = "w342"): String? {
        if (path.isNullOrBlank()) return null
        if (path.startsWith("http")) return path
        return if (cacheImages) api.abs("/imageproxy/tmdb/t/p/$size$path")
        else "https://image.tmdb.org/t/p/$size$path"
    }

    /** User avatars can be absolute (Plex, Gravatar) or a path on the Seerr server. */
    fun avatar(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        return if (raw.startsWith("http")) raw else api.abs(raw)
    }
}
