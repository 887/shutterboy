package com.eight87.shutterboy

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.video.VideoFrameDecoder
import com.eight87.shutterboy.data.coil.MediaStoreThumbnailFetcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi

/**
 * Process-scoped composition root holder. The single [AppGraph] is constructed
 * here on application start and lives for the process lifetime; activities,
 * view models, and (later) services obtain their narrow facets through
 * [graph].
 *
 * Implements [SingletonImageLoader.Factory] so every `AsyncImage` uses the
 * same Coil 3 [ImageLoader] — registered with the [VideoFrameDecoder] so
 * MediaStore video URIs render as extracted-frame thumbnails across the
 * gallery + viewer surfaces. Cache budgets are bumped above Coil's default
 * ~25% memory / ~250 MB disk so a 10k+ photo library keeps tiles warm
 * during fast scroll (R.F.29).
 */
class ShutterboyApplication : Application(), SingletonImageLoader.Factory {

    lateinit var graph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        graph = AppGraph(applicationContext = this)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components {
                // MediaStore thumbnail fetcher — same trick Aves uses.
                // ContentResolver.loadThumbnail (API 29+) returns the
                // OS-cached thumbnail in ~1-2 ms instead of Coil's
                // default 50-100 ms full-JPEG decode + downsample.
                // Registered FIRST so it wins for thumbnail-sized
                // requests to media URIs; viewer-resolution requests
                // skip past it and hit the default ContentUriFetcher.
                add(MediaStoreThumbnailFetcher.Factory())
                add(VideoFrameDecoder.Factory())
            }
            // 32 workers on fetch + decode. Now that each work-item is
            // a ~1-3 ms MediaStore loadThumbnail call (not a 50 ms full
            // JPEG decode), it's cheap to fan out widely — more in-
            // flight requests = a deeper prefetch window stays warm
            // even during sustained scroll.
            .fetcherCoroutineContext(Dispatchers.IO.limitedParallelism(32))
            .decoderCoroutineContext(Dispatchers.Default.limitedParallelism(32))
            // R.F.29 — Aves-class gallery libraries (10k–50k photos) blow
            // through Coil's default ~25% maxMemory budget during fast
            // scrolling; tiles fall out of cache and re-decode the
            // moment they re-enter the viewport, which is the dominant
            // scroll-jank cause. Bump to 50% of maxMemory.
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.50)
                    .build()
            }
            // Disk cache covers viewer / re-open cases; bump from
            // ~250 MB default to 512 MB so day-to-day re-views are
            // decode-free.
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache"))
                    .maxSizeBytes(512L * 1024 * 1024)
                    .build()
            }
            // Crossfade OFF — the fade animation on top of fast scroll
            // is itself main-thread work that visibly stutters during
            // a fling. Aves snaps thumbnails in instantly; we match.
            .build()
}
