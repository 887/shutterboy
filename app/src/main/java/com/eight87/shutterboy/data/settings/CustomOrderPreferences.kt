package com.eight87.shutterboy.data.settings

import com.eight87.shutterboy.domain.FolderId
import com.eight87.shutterboy.domain.SmartAlbumId
import kotlinx.coroutines.flow.Flow

/**
 * Phase E.4 — narrow facet for user-pinned ordering of the smart-album
 * chip row + folders grid. Separated from [SortPreferences] because
 * "what order do these items render in" is a different concern from
 * "how do we sort photos *inside* one of those items" — both are
 * persisted, neither is the other.
 *
 * Empty / unset list = no custom order; the UI applies the natural
 * ordering coming out of the data layer (see
 * [com.eight87.shutterboy.ui.collections.applyCustomOrder]). Per-folder
 * sort + per-album sort live on [SortPreferences]; this facet only
 * tracks the *chrome* ordering.
 */
interface CustomOrderPreferences {
    fun observeSmartAlbumOrder(): Flow<List<SmartAlbumId>>
    fun observeFolderOrder(): Flow<List<FolderId>>

    suspend fun setSmartAlbumOrder(order: List<SmartAlbumId>)
    suspend fun setFolderOrder(order: List<FolderId>)
}
