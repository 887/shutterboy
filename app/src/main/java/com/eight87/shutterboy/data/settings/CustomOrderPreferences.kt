package com.eight87.shutterboy.data.settings

import com.eight87.shutterboy.domain.FolderId
import kotlinx.coroutines.flow.Flow

/**
 * User-pinned ordering of the Collections folder grid. Empty / unset
 * list = no custom order; the UI helper
 * [com.eight87.shutterboy.ui.collections.applyCustomOrder] collapses
 * that to the natural ordering coming out of the data layer.
 */
interface CustomOrderPreferences {
    fun observeFolderOrder(): Flow<List<FolderId>>
    suspend fun setFolderOrder(order: List<FolderId>)
}
