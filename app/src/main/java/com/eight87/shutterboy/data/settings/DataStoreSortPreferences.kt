package com.eight87.shutterboy.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.eight87.shutterboy.domain.FolderId
import com.eight87.shutterboy.domain.sort.PhotoSort
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Phase E.2 — concrete [SortPreferences] backed by `DataStore<Preferences>`.
 * Per-folder sort keys are namespaced as `folder_sort__<id>`; the read
 * path falls back to `collections_sort` when the per-folder key is unset
 * (so the UI sees a populated Flow without needing a `null` ladder
 * upstream).
 *
 * Writes are suspend functions; the UI calls them inside a coroutine
 * scope when the user hits Apply on the sort sheet.
 */
internal class DataStoreSortPreferences(
    private val dataStore: DataStore<Preferences>,
) : SortPreferences {

    override fun observePhotosSort(): Flow<PhotoSort> = dataStore.data
        .map { prefs -> PhotoSortCodec.decode(prefs[PhotosSortKey]) }

    override fun observeCollectionsSort(): Flow<PhotoSort> = dataStore.data
        .map { prefs -> PhotoSortCodec.decode(prefs[CollectionsSortKey]) }

    override fun observeFolderSort(folderId: FolderId): Flow<PhotoSort> = dataStore.data
        .map { prefs ->
            val perFolder = prefs[folderSortKey(folderId)]
            if (perFolder != null) PhotoSortCodec.decode(perFolder)
            else PhotoSortCodec.decode(prefs[CollectionsSortKey])
        }

    override suspend fun setPhotosSort(sort: PhotoSort) {
        dataStore.edit { it[PhotosSortKey] = PhotoSortCodec.encode(sort) }
    }

    override suspend fun setCollectionsSort(sort: PhotoSort) {
        dataStore.edit { it[CollectionsSortKey] = PhotoSortCodec.encode(sort) }
    }

    override suspend fun setFolderSort(folderId: FolderId, sort: PhotoSort) {
        dataStore.edit { it[folderSortKey(folderId)] = PhotoSortCodec.encode(sort) }
    }

    private fun folderSortKey(folderId: FolderId) =
        stringPreferencesKey("folder_sort__${folderId.value}")

    companion object {
        private val PhotosSortKey = stringPreferencesKey("photos_sort")
        private val CollectionsSortKey = stringPreferencesKey("collections_sort")
    }
}
