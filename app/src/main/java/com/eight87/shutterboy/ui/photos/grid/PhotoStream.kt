package com.eight87.shutterboy.ui.photos.grid

import com.eight87.shutterboy.domain.Photo
import kotlinx.coroutines.flow.Flow

/**
 * Phase D.3.5 — strategy for the photo-timeline engine. Three surfaces
 * (Photos / FolderDetail / SmartAlbumDetail) all share the density-zoomed
 * grid + scrubber + sticky-banner stack; each one only differs in *which*
 * photos to show. [com.eight87.shutterboy.ui.photos.grid.PhotosGrid] now
 * takes a `PhotoStream` instead of the wholesale [com.eight87.shutterboy.data.repo.PhotoSource]
 * facet — R.D engine-and-strategy locked, OCP open-for-new-surfaces.
 *
 * **Sort discipline (Phase E.2 locked):** the strategy bakes the sort
 * choice into the closure, not the engine. The renderer doesn't know
 * about sort — the surface picks `observePhotos(sort)` /
 * `observePhotosInFolder(folderId, sort)` /
 * `observeSmartAlbum(id)` (no sort) per its own persisted preference and
 * hands the resulting Flow to [PhotosGrid] via this interface. Sort
 * changes thread through the surface re-creating its `PhotoStream` keyed
 * against the new sort.
 *
 * `fun interface` so SAM-conversion lets each call-site stay a one-liner.
 */
fun interface PhotoStream {
    fun observe(): Flow<List<Photo>>
}
