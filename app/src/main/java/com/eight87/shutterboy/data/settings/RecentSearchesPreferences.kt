package com.eight87.shutterboy.data.settings

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Phase G.5 — persistence for the last 10 distinct search queries.
 *
 * Ordering: most-recent first. Recording an existing entry promotes it to
 * the head rather than appending a duplicate. The list is trimmed to
 * [MAX_RECENT_SEARCHES] on every write. Blank queries are ignored.
 */
interface RecentSearchesPreferences {
    fun observe(): Flow<List<String>>
    suspend fun record(query: String)

    companion object {
        const val MAX_RECENT_SEARCHES: Int = 10
    }
}

/**
 * Default for [com.eight87.shutterboy.data.repo.RoomGalleryRepository] when
 * no real persistence is wired (notably in unit tests that don't touch
 * search history). Emits an empty list and silently swallows writes.
 */
internal object NoOpRecentSearchesPreferences : RecentSearchesPreferences {
    override fun observe(): Flow<List<String>> = flowOf(emptyList())
    override suspend fun record(query: String) { /* no-op */ }
}
