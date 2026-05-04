package com.eight87.shutterboy.data.repo

import com.eight87.shutterboy.domain.MediaChange
import kotlinx.coroutines.flow.Flow

/**
 * Reactive pulse from the underlying media surfaces. Wired to a
 * `ContentResolver.registerContentObserver` on `MediaStore.Images.Media.EXTERNAL_CONTENT_URI`
 * for DEVICE; SAF tree changes are emitted when a `DocumentFile` walk diff
 * detects new/changed entries (the SAF API doesn't expose a real ContentObserver).
 *
 * The repository's [LibraryScanner] subscribes to this and debounces / coalesces
 * scan re-runs. UI never subscribes directly — UI Flows already update when
 * Room rows change.
 */
interface MediaChangeSource {
    fun observeChanges(): Flow<MediaChange>
}
