package com.eight87.shutterboy.domain

import android.app.PendingIntent

/**
 * Phase H.4 — three-branch SDK split for bulk MediaStore moves.
 *
 *  - API 30+ (scoped storage): non-owned files need user consent via
 *    `MediaStore.createWriteRequest(resolver, uris, true)` →
 *    [Consent] carrying the `PendingIntent`. After consent the actual
 *    `ContentResolver.update` calls run; for the v1 scope we surface the
 *    consent flow and let the caller refresh the library after the user
 *    accepts (a follow-up scan picks up the new RELATIVE_PATH rows).
 *  - API 26-28 (legacy storage): we attempt direct
 *    `ContentResolver.update(uri, ContentValues with RELATIVE_PATH)` per
 *    photo and report [Direct] with the moved count.
 *  - Failure: [Failure] with a short reason.
 *
 * Mirrors [DeleteRequest]'s three-branch shape so the Compose layer can
 * `when`-dispatch identically.
 */
sealed interface MoveRequest {
    data class Direct(val movedCount: Int) : MoveRequest
    data class Consent(val intentSender: PendingIntent) : MoveRequest
    data class Failure(val reason: String) : MoveRequest
}
