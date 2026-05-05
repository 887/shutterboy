package com.eight87.shutterboy.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.eight87.shutterboy.domain.FolderId
import com.eight87.shutterboy.domain.SmartAlbumId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Phase E.4 — concrete [CustomOrderPreferences] backed by
 * `DataStore<Preferences>`. Encoding: comma-separated single-key strings
 * for both lists.
 *   - smart_album_order: `"camera,screenshots,favorites,recents"`
 *     (entries that don't decode via [SmartAlbumId.fromStorageKey] are
 *     silently dropped — handles a downgrade from a future build that
 *     introduced a new album).
 *   - folder_order: `"42,17,3"` — entries that fail `Long` parse are
 *     dropped likewise.
 *
 * Empty stored value (or the key absent) decodes to `emptyList()`, which
 * the UI helper [com.eight87.shutterboy.ui.collections.applyCustomOrder]
 * collapses to the natural ordering.
 */
internal class DataStoreCustomOrderPreferences(
    private val dataStore: DataStore<Preferences>,
) : CustomOrderPreferences {

    override fun observeSmartAlbumOrder(): Flow<List<SmartAlbumId>> = dataStore.data
        .map { prefs ->
            decodeSmartAlbumOrder(prefs[SmartAlbumOrderKey])
        }

    override fun observeFolderOrder(): Flow<List<FolderId>> = dataStore.data
        .map { prefs ->
            decodeFolderOrder(prefs[FolderOrderKey])
        }

    override suspend fun setSmartAlbumOrder(order: List<SmartAlbumId>) {
        dataStore.edit {
            it[SmartAlbumOrderKey] = order.joinToString(",") { id -> id.storageKey }
        }
    }

    override suspend fun setFolderOrder(order: List<FolderId>) {
        dataStore.edit {
            it[FolderOrderKey] = order.joinToString(",") { id -> id.value.toString() }
        }
    }

    companion object {
        private val SmartAlbumOrderKey = stringPreferencesKey("smart_album_order")
        private val FolderOrderKey = stringPreferencesKey("folder_order")

        internal fun decodeSmartAlbumOrder(stored: String?): List<SmartAlbumId> {
            if (stored.isNullOrBlank()) return emptyList()
            return stored.split(",")
                .mapNotNull { token -> SmartAlbumId.fromStorageKey(token.trim()) }
        }

        internal fun decodeFolderOrder(stored: String?): List<FolderId> {
            if (stored.isNullOrBlank()) return emptyList()
            return stored.split(",")
                .mapNotNull { token -> token.trim().toLongOrNull()?.let(::FolderId) }
        }
    }
}
