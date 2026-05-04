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
}
