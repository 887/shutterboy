package com.eight87.shutterboy.data.repo

import com.eight87.shutterboy.domain.Photo
import com.eight87.shutterboy.domain.SmartAlbumId
import kotlinx.coroutines.flow.Flow

/**
 * Phase B.6 — smart-album resolution. Sealed dispatch on [SmartAlbumId];
 * each variant maps to a Room query under the hood. UI consumes a single
 * `Flow<List<Photo>>` per album id.
 */
interface SmartAlbumSource {
    fun observeSmartAlbum(id: SmartAlbumId): Flow<List<Photo>>

    /** Cover photo per smart album (most recent matching photo, or null). */
    fun observeSmartAlbumCovers(): Flow<Map<SmartAlbumId, Photo?>>
}
