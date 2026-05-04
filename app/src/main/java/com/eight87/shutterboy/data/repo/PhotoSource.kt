package com.eight87.shutterboy.data.repo

import com.eight87.shutterboy.domain.FolderId
import com.eight87.shutterboy.domain.Photo
import com.eight87.shutterboy.domain.sort.PhotoSort
import kotlinx.coroutines.flow.Flow

/**
 * Phase B.5 narrow facet — read-only photo Flows. Tonearmboy R.A locked.
 * Composables that need a list of photos depend on this, never on
 * [com.eight87.shutterboy.data.repo.RoomGalleryRepository].
 */
interface PhotoSource {
    fun observePhotos(sort: PhotoSort = PhotoSort.Default): Flow<List<Photo>>
    fun observePhotosInFolder(folderId: FolderId, sort: PhotoSort = PhotoSort.Default): Flow<List<Photo>>
    suspend fun photosByIds(ids: List<Long>): List<Photo>
}
