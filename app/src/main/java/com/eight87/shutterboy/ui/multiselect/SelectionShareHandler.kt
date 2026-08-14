package com.eight87.shutterboy.ui.multiselect

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.eight87.shutterboy.R
import com.eight87.shutterboy.data.repo.PhotoSource
import com.eight87.shutterboy.domain.Photo
import com.eight87.shutterboy.domain.PhotoId
import kotlinx.coroutines.launch

/**
 * Bulk share for selection mode — sibling of [SelectionDeleteHandler] and
 * [SelectionMoveHandler], same one-line wiring shape:
 *
 * ```kotlin
 * val share = rememberSelectionShareHandler(scope.photoSource) { holder.exit() }
 * ...
 * onShare = { share.request(holder.selectedIds()) }
 * ```
 *
 * Resolving ids → content URIs is a suspend read, so `request` hands off to
 * a coroutine rather than blocking the click. One photo uses `ACTION_SEND`
 * (some targets only handle the singular action); two or more use
 * `ACTION_SEND_MULTIPLE`. The MIME is the photos' common type when they
 * agree and the wildcard-subtype form otherwise; a mixed image+video
 * selection falls back to the any-type wildcard.
 *
 * Read-grant only: these are MediaStore URIs we don't own, so a write grant
 * is not ours to give (see `launchEditChooser`).
 */
class SelectionShareHandler internal constructor(
    private val request: (Set<PhotoId>) -> Unit,
) {
    fun request(ids: Set<PhotoId>) = request.invoke(ids)
}

@Composable
fun rememberSelectionShareHandler(
    photoSource: PhotoSource,
    onComplete: () -> Unit,
): SelectionShareHandler {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    return remember(photoSource, context) {
        SelectionShareHandler(
            request = { ids ->
                if (ids.isNotEmpty()) {
                    coroutineScope.launch {
                        val photos = photoSource.photosByIds(ids.map { it.value })
                        if (photos.isNotEmpty() && sharePhotos(context, photos)) {
                            onComplete()
                        }
                    }
                }
            },
        )
    }
}

/** @return true when a chooser was actually launched. */
private fun sharePhotos(context: Context, photos: List<Photo>): Boolean {
    val uris = ArrayList<Uri>(photos.map { it.contentUri })
    val intent = if (uris.size == 1) {
        Intent(Intent.ACTION_SEND).apply {
            type = commonMimeType(photos)
            putExtra(Intent.EXTRA_STREAM, uris.first())
        }
    } else {
        Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = commonMimeType(photos)
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
        }
    }.apply {
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    return try {
        context.startActivity(
            Intent.createChooser(
                intent,
                context.getString(R.string.viewer_share_chooser_title),
            ),
        )
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}

/**
 * Exact MIME when every photo agrees, else the shared top-level type with a
 * wildcard subtype, else the any-type wildcard. Chooser targets filter on
 * this, so being as specific as the selection allows keeps irrelevant apps
 * out of the sheet.
 */
internal fun commonMimeType(photos: List<Photo>): String {
    val types = photos.map { it.mimeType }.filter(String::isNotBlank).toSet()
    if (types.isEmpty()) return "*/*"
    types.singleOrNull()?.let { return it }
    val tops = types.map { it.substringBefore('/') }.toSet()
    return tops.singleOrNull()?.let { "$it/*" } ?: "*/*"
}
