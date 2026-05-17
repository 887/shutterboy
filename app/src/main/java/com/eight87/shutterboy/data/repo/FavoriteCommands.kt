package com.eight87.shutterboy.data.repo

import com.eight87.shutterboy.domain.Photo
import com.eight87.shutterboy.domain.PhotoId
import kotlinx.coroutines.flow.Flow

/**
 * Phase G.3 — favorites facet. Narrow interface so the viewer's heart
 * toggle, the Favorites destination, and the Search "Favorites only"
 * filter all depend on this rather than the wholesale repository.
 *
 * `observeFavoritePhotos()` and `observeFavoriteIds()` back the
 * Favorites surface + the Search-time intersection filter respectively;
 * `observeIsFavorite()` drives the per-photo heart icon state in the
 * viewer chrome.
 */
interface FavoriteCommands {
    suspend fun toggleFavorite(photoId: PhotoId): Boolean
    suspend fun isFavorite(photoId: PhotoId): Boolean
    fun observeIsFavorite(photoId: PhotoId): Flow<Boolean>
    fun observeFavoritePhotos(): Flow<List<Photo>>
    fun observeFavoriteIds(): Flow<Set<Long>>
}
