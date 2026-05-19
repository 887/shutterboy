package com.eight87.shutterboy.data.coil

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.Size as AndroidSize
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DataSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.request.Options
import coil3.size.pxOrElse

/**
 * Coil [Fetcher] backed by [android.content.ContentResolver.loadThumbnail]
 * — the system's MediaStore thumbnail cache (API 29+). Returns in
 * ~1-2 ms for a thumbnail-sized request because the OS already
 * generated and persisted the bitmap when the media file was indexed,
 * instead of the ~50-100 ms it takes Coil's default ContentUriFetcher
 * to open the full JPEG, decode it, and downsample to the target size.
 *
 * Matches the technique used by Aves (`ThumbnailFetcher.kt`): prefer
 * the resolver thumbnail for small sizes, fall through to the default
 * Coil pipeline for large (viewer-resolution) sizes.
 *
 * Registered before [coil3.fetch.ContentUriFetcher.Factory] so it wins
 * the dispatch for `content://media/...` URIs requested at
 * thumbnail size; viewer-resolution requests skip past it via the
 * [maxPx] cutoff.
 */
class MediaStoreThumbnailFetcher(
    private val context: Context,
    private val uri: Uri,
    private val options: Options,
) : Fetcher {
    override suspend fun fetch(): FetchResult {
        val width = options.size.width.pxOrElse { DEFAULT_SIZE }
        val height = options.size.height.pxOrElse { DEFAULT_SIZE }
        val raw = context.contentResolver.loadThumbnail(
            uri,
            AndroidSize(width, height),
            null,
        )
        // HARDWARE config → GPU-resident bitmap → zero-copy draws. The
        // micro-stutter while scrolling a tile grid is the GPU
        // re-uploading software bitmaps every frame; HARDWARE bitmaps
        // upload once and are bound directly.
        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            runCatching { raw.copy(Bitmap.Config.HARDWARE, false) }
                .getOrNull() ?: raw
        } else {
            raw
        }
        // Kick off async GPU texture upload on RenderThread NOW (between
        // frames), not lazily inside the first DrawFrame that paints the
        // tile. Without this the upload lands on the critical path of
        // the first frame after Photos becomes visible → exactly the
        // "lag when the surface switches over" stutter.
        bitmap.prepareToDraw()
        return ImageFetchResult(
            image = bitmap.asImage(),
            isSampled = true,
            dataSource = DataSource.DISK,
        )
    }

    class Factory(private val maxPx: Int = MAX_THUMBNAIL_PX) :
        Fetcher.Factory<Uri> {
        override fun create(
            data: Uri,
            options: Options,
            imageLoader: ImageLoader,
        ): Fetcher? {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
            if (data.scheme != "content") return null
            val authority = data.authority ?: return null
            // Only fire for MediaStore-backed URIs; other content://
            // providers may not support loadThumbnail.
            if (!authority.endsWith("media")) return null
            // Skip viewer-resolution requests — they need the full
            // image, not the OS thumbnail.
            val w = options.size.width.pxOrElse { Int.MAX_VALUE }
            val h = options.size.height.pxOrElse { Int.MAX_VALUE }
            if (w > maxPx || h > maxPx) return null
            return MediaStoreThumbnailFetcher(options.context, data, options)
        }
    }

    companion object {
        private const val DEFAULT_SIZE = 512
        private const val MAX_THUMBNAIL_PX = 1024
    }
}
