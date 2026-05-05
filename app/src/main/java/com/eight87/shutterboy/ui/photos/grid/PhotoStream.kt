package com.eight87.shutterboy.ui.photos.grid

import com.eight87.shutterboy.domain.Photo
import com.eight87.shutterboy.domain.sort.PhotoSort
import kotlinx.coroutines.flow.Flow

/**
 * Phase D.3.5 — strategy for the photo-timeline engine. Three surfaces
 * (Photos / FolderDetail / SmartAlbumDetail) all share the density-zoomed
 * grid + scrubber + sticky-banner stack; each one only differs in *which*
 * photos to show. [com.eight87.shutterboy.ui.photos.grid.PhotosGrid] now
 * takes a `PhotoStream` instead of the wholesale [com.eight87.shutterboy.data.repo.PhotoSource]
 * facet — R.D engine-and-strategy locked, OCP open-for-new-surfaces.
 *
 * Concrete strategies built at the call-site:
 *   - Photos tab → `PhotoStream { sort -> scope.photoSource.observePhotos(sort) }`
 *   - FolderDetail → `PhotoStream { sort -> scope.photoSource.observePhotosInFolder(folderId, sort) }`
 *   - SmartAlbumDetail → `PhotoStream { _ -> scope.smartAlbumSource.observeSmartAlbum(id) }`
 *     (smart albums don't accept a sort in v1; the lambda parameter is
 *     ignored).
 *
 * `fun interface` so SAM-conversion lets each call-site stay a one-liner.
 */
fun interface PhotoStream {
    fun observe(sort: PhotoSort): Flow<List<Photo>>
}
