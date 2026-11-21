package com.vknxworks.seerrctrl.data

/**
 * A title (or person or collection) named by a link someone shared to SeerrCtrl: a themoviedb.org page,
 * which carries the TMDB id Seerr uses, or an imdb.com page, whose id Seerr looks up (search "imdb:tt…").
 */
sealed interface SharedLink {
    /** A page in SeerrCtrl: "movie/603", "tv/1399", "person/31" or "collection/2344". */
    data class Tmdb(val route: String) : SharedLink

    /** An IMDb id: tt… for a title, nm… for a person. */
    data class Imdb(val id: String) : SharedLink

    companion object {
        private val TMDB = Regex("""(?:^|[\s(<"'])(?:https?://)?(?:www\.|m\.)?themoviedb\.org/(movie|tv|person|collection)/(\d{1,9})(?![\d.])""", RegexOption.IGNORE_CASE)
        private val IMDB = Regex("""(?:^|[\s(<"'])(?:https?://)?(?:www\.|m\.)?imdb\.com/(?:[a-z]{2}(?:-[a-z]{2})?/)?(?:title/(tt\d{5,10})|name/(nm\d{5,10}))(?!\d)""", RegexOption.IGNORE_CASE)

        /** The first TMDB or IMDb link in [text] (what a share sheet sends: often a title, then the link), or null. */
        fun parse(text: String?): SharedLink? {
            if (text.isNullOrBlank()) return null
            // Shared text is short; anything huge isn't a link someone shared.
            val t = text.take(4000)
            val tmdb = TMDB.find(t)
            val imdb = IMDB.find(t)
            return when {
                tmdb != null && (imdb == null || tmdb.range.first < imdb.range.first) -> {
                    val id = tmdb.groupValues[2].toIntOrNull()?.takeIf { it > 0 } ?: return null
                    Tmdb("${tmdb.groupValues[1].lowercase()}/$id")
                }
                imdb != null -> Imdb((imdb.groupValues[1].ifEmpty { imdb.groupValues[2] }).lowercase())
                else -> null
            }
        }

        /** The page for the first usable result of Seerr's search for "imdb:<id>", or null when it found nothing. */
        fun routeFromSearch(results: List<org.json.JSONObject>): String? = results.firstNotNullOfOrNull { r ->
            val id = r.intOrNull("id")?.takeIf { it > 0 } ?: return@firstNotNullOfOrNull null
            when (r.str("mediaType")) {
                "movie" -> "movie/$id"
                "tv" -> "tv/$id"
                "person" -> "person/$id"
                else -> null
            }
        }
    }
}
