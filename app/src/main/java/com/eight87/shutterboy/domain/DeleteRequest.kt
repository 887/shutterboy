package com.eight87.shutterboy.domain

import android.app.PendingIntent

/**
 * Three-branch SDK split for MediaStore deletes, mirroring tonearmboy's
 * `TrackDeleter`:
 *  - API 30+: `MediaStore.createDeleteRequest` returns a [PendingIntent] the UI
 *    routes through `IntentSenderRequest` for the system consent dialog →
 *    [Consent].
 *  - API 29: `RecoverableSecurityException.userAction` intent → also [Consent].
 *  - API 26-28: direct `contentResolver.delete` → [Immediate].
 *
 * The repository's [com.eight87.shutterboy.data.repo.PhotoDeleter] surface
 * returns the sealed type so the UI can `when`-dispatch on it without leaking
 * SDK-version branches into the Compose layer.
 */
sealed interface DeleteRequest {
    data class Immediate(val deletedCount: Int) : DeleteRequest
    data class Consent(val intentSender: PendingIntent) : DeleteRequest
    data class Failure(val reason: String) : DeleteRequest
}
