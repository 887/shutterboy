package com.eight87.shutterboy

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.request.crossfade
import coil3.video.VideoFrameDecoder
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
            .components { add(VideoFrameDecoder.Factory()) }
            // 8 worker coroutines stealing from the loader queue for
            // fetch + decode, so a fling-burst of visible-tile requests
            // doesn't stall behind a single in-flight JPEG.
            .fetcherCoroutineContext(Dispatchers.IO.limitedParallelism(8))
            .decoderCoroutineContext(Dispatchers.Default.limitedParallelism(8))
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
            .crossfade(true)
            .build()
}
