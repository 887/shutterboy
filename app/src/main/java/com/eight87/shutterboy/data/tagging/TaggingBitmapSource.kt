package com.eight87.shutterboy.data.tagging

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Decodes a small bitmap for a photo to feed the tagger. A seam (DIP) so
 * the service can be unit-tested with a fake decoder instead of a real
 * ContentResolver.
 */
interface TaggingBitmapSource {
    /** Decode a downscaled bitmap, or null if the photo can't be read. */
    suspend fun decode(contentUri: String): Bitmap?
}

/**
 * Production [TaggingBitmapSource]. Prefers `ContentResolver.loadThumbnail`
 * (API 29+) — the OS already cached a thumbnail when it indexed the media,
 * so this is cheap and never holds a full-resolution bitmap in memory. On
 * API 26-28 it falls back to a sampled `BitmapFactory` decode.
 *
 * [TARGET_PX] (512) comfortably clears ML Kit's minimum useful input
 * (~480×360 for labeling) without paying for full-size decodes.
 */
class ContentResolverBitmapSource(
    private val context: Context,
) : TaggingBitmapSource {

    override suspend fun decode(contentUri: String): Bitmap? = withContext(Dispatchers.IO) {
        val uri = runCatching { Uri.parse(contentUri) }.getOrNull() ?: return@withContext null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching {
                context.contentResolver.loadThumbnail(uri, Size(TARGET_PX, TARGET_PX), null)
            }.getOrNull()
        } else {
            decodeSampled(uri)
        }
    }

    private fun decodeSampled(uri: Uri): Bitmap? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        var sample = 1
        var w = bounds.outWidth
        var h = bounds.outHeight
        while (w / 2 >= TARGET_PX && h / 2 >= TARGET_PX) {
            w /= 2; h /= 2; sample *= 2
        }
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opts)
        }
    }.getOrNull()

    private companion object {
        const val TARGET_PX = 512
    }
}
