package com.eight87.shutterboy.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.eight87.shutterboy.domain.FolderId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Concrete [CustomOrderPreferences] backed by `DataStore<Preferences>`.
 * Encoding: comma-separated folder ids — `"42,17,3"`. Entries that fail
 * `Long` parse are dropped silently.
 *
 * Empty stored value (or the key absent) decodes to `emptyList()`,
 * which [com.eight87.shutterboy.ui.collections.applyCustomOrder]
 * collapses to the natural ordering.
 */
internal class DataStoreCustomOrderPreferences(
    private val dataStore: DataStore<Preferences>,
) : CustomOrderPreferences {

    override fun observeFolderOrder(): Flow<List<FolderId>> = dataStore.data
        .map { prefs ->
            decodeFolderOrder(prefs[FolderOrderKey])
        }

    override suspend fun setFolderOrder(order: List<FolderId>) {
        dataStore.edit {
            it[FolderOrderKey] = order.joinToString(",") { id -> id.value.toString() }
        }
    }

    companion object {
        private val FolderOrderKey = stringPreferencesKey("folder_order")

        internal fun decodeFolderOrder(stored: String?): List<FolderId> {
            if (stored.isNullOrBlank()) return emptyList()
            return stored.split(",")
                .mapNotNull { token -> token.trim().toLongOrNull()?.let(::FolderId) }
        }
    }
}
