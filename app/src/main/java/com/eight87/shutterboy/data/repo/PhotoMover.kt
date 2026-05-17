package com.eight87.shutterboy.data.repo

import com.eight87.shutterboy.domain.MoveRequest
import com.eight87.shutterboy.domain.PhotoId

/**
 * Phase H.4 — narrow facet covering bulk move-to-album.
 *
 * Move semantics: per-photo `ContentResolver.update(uri, {RELATIVE_PATH=...})`.
 *  - API 30+ (scoped storage) requires user consent for non-owned files; the
 *    impl gathers the per-photo `IntentSender`s into a single
 *    `MediaStore.createWriteRequest` consent dialog and returns
 *    [MoveRequest.Consent].
 *  - API 26-28 attempts a direct update per uri and returns
 *    [MoveRequest.Direct] with the moved count.
 *  - Any failure → [MoveRequest.Failure].
 *
 * The caller routes [MoveRequest.Consent] through an `IntentSenderRequest`
 * launcher (same shape as [PhotoDeleter]). SAF-sourced photos are out of
 * scope for v1 — the impl returns [MoveRequest.Failure] when asked to move
 * a uri the resolver refuses to update.
 */
interface PhotoMover {
    /**
     * @param ids photos to move (must be MediaStore-visible)
     * @param targetRelativePath the MediaStore RELATIVE_PATH the photos
     *   should land at — e.g. `Pictures/Camera/`. Trailing slash required.
     */
    suspend fun moveToFolder(
        ids: List<PhotoId>,
        targetRelativePath: String,
    ): MoveRequest

    /**
     * Apply the pending RELATIVE_PATH update after the user has accepted
     * the system consent dialog launched from a [MoveRequest.Consent]
     * branch. Returns the number of rows actually updated.
     *
     * On API 26-28 this is unused — the direct branch performs the update
     * inline and returns [MoveRequest.Direct] immediately.
     */
    suspend fun applyMoveAfterConsent(): Int
}
