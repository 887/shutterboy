package com.eight87.shutterboy.domain

/**
 * Smart-album catalogue locked for v1 in the design doc — Camera, Screenshots,
 * Favorites, Recents. Selfies / Videos / Recently-Deleted deferred to v2.
 *
 * Resolution rules are documented per case below; each maps to a Room query at
 * the repo layer. Default order is the catalogue order; user reordering is
 * persisted by [SmartAlbumId.storageKey] in DataStore (Phase E.4).
 */
sealed interface SmartAlbumId {
    /** Stable string for DataStore-persisted custom order + per-album sort keys. */
    val storageKey: String

    /** `bucket_display_name == "Camera"` (case-insensitive). */
    data object Camera : SmartAlbumId {
        override val storageKey = "camera"
    }

    /** `bucket_display_name == "Screenshots"` (case-insensitive). */
    data object Screenshots : SmartAlbumId {
        override val storageKey = "screenshots"
    }

    /** Joined against `photo_favorites`. */
    data object Favorites : SmartAlbumId {
        override val storageKey = "favorites"
    }

    /** Last 30 days by `dateTakenMs`. */
    data object Recents : SmartAlbumId {
        override val storageKey = "recents"

        /** Window in milliseconds — exposed for testability. */
        const val WINDOW_MS: Long = 30L * 24 * 60 * 60 * 1000
    }

    companion object {
        val defaultOrder: List<SmartAlbumId> =
            listOf(Camera, Screenshots, Favorites, Recents)

        fun fromStorageKey(key: String): SmartAlbumId? =
            defaultOrder.firstOrNull { it.storageKey == key }
    }
}
