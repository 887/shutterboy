package com.eight87.shutterboy.data.repo

import com.eight87.shutterboy.domain.PhotoTag
import kotlinx.coroutines.flow.Flow

/**
 * Narrow read facet (ISP) for a photo's Smart Tags. The viewer's info
 * panel depends only on this, not the whole repository.
 */
interface PhotoTagSource {
    /** Reactive tags for a photo, highest-confidence first. */
    fun observeTagsForPhoto(photoId: Long): Flow<List<PhotoTag>>
}
