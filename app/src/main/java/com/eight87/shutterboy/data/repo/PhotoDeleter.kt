package com.eight87.shutterboy.data.repo

import com.eight87.shutterboy.domain.DeleteRequest
import com.eight87.shutterboy.domain.PhotoId

interface PhotoDeleter {
    /**
     * Three-branch SDK split:
     *  - API 30+: returns `Consent(intentSender)`; UI routes through
     *    `IntentSenderRequest` to show the system consent dialog.
     *  - API 29: same `Consent` shape — `RecoverableSecurityException.userAction`.
     *  - API 26-28: returns `Immediate(deletedCount)` — direct delete.
     *  - Failure: returns `Failure(reason)` with a short reason string.
     */
    suspend fun deletePhotos(ids: List<PhotoId>): DeleteRequest

    /**
     * Drop these photo rows from the Room cache immediately. Called by
     * the UI after a successful consent-dialog confirm (or an
     * Immediate-branch success) so the grid updates instantly — Aves-
     * style — without waiting for the MediaStore change observer to
     * re-trigger a full library scan. The eventual rescan is still
     * harmless (it's a no-op for already-removed rows), but the user
     * sees the deletion land in one frame instead of after a multi-
     * second walk.
     */
    suspend fun eagerlyRemoveFromCache(ids: List<PhotoId>)
}
