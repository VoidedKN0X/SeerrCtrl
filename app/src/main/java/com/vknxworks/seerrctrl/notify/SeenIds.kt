package com.vknxworks.seerrctrl.notify

/**
 * Keeps the "already notified" lists of the background check small without forgetting anything that still
 * matters: ids in the list Seerr returned this time are always kept (dropping one would notify it again as
 * new), and the rest is filled up with the newest (highest) ids. Seerr's ids only grow, so an old id that is
 * dropped belongs to a request or issue that left the list long ago.
 */
object SeenIds {
    const val MAX = 400

    private fun num(id: String): Long = id.toLongOrNull() ?: Long.MIN_VALUE

    /** [seen] trimmed to [max] entries, always keeping every id in [current]. */
    fun trim(seen: Set<String>, current: Set<String>, max: Int = MAX): Set<String> {
        if (seen.size <= max) return seen
        val keep = LinkedHashSet<String>(current.filter { it in seen })
        for (id in seen.sortedByDescending(::num)) {
            if (keep.size >= max) break
            keep += id
        }
        return keep
    }

    /** The keys of a map to keep: all of [current], then the highest other keys of [all], up to [max] in total. */
    fun keysToKeep(all: Set<String>, current: Set<String>, max: Int = MAX): Set<String> = trim(all + current, current, max)
}
