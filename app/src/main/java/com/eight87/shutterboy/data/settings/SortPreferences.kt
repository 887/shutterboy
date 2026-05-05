package com.eight87.shutterboy.data.settings

import com.eight87.shutterboy.domain.FolderId
import com.eight87.shutterboy.domain.sort.PhotoSort
import kotlinx.coroutines.flow.Flow

/**
 * Phase E.2 — narrow facet for sort persistence. Each surface asks for its
 * own sort key (`photos_sort`, `collections_sort`, per-folder override).
 * The folder-detail surface falls back to `collections_sort` when no
 * per-folder key is set — fallback lives inside the impl so the UI sees
 * one Flow regardless of whether the user ever opened the sort sheet for
 * that folder.
 */
interface SortPreferences {
    fun observePhotosSort(): Flow<PhotoSort>
    fun observeCollectionsSort(): Flow<PhotoSort>
    fun observeFolderSort(folderId: FolderId): Flow<PhotoSort>

    suspend fun setPhotosSort(sort: PhotoSort)
    suspend fun setCollectionsSort(sort: PhotoSort)
    suspend fun setFolderSort(folderId: FolderId, sort: PhotoSort)
}
