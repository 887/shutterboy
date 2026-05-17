package com.eight87.shutterboy

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.video.VideoFrameDecoder

/**
 * Process-scoped composition root holder. The single [AppGraph] is constructed
 * here on application start and lives for the process lifetime; activities,
 * view models, and (later) services obtain their narrow facets through
 * [graph].
 *
 * Implements [SingletonImageLoader.Factory] so every `AsyncImage` uses the
 * same Coil 3 [ImageLoader] — registered with the [VideoFrameDecoder] so
 * MediaStore video URIs render as extracted-frame thumbnails across the
 * gallery + viewer surfaces.
 */
class ShutterboyApplication : Application(), SingletonImageLoader.Factory {

    lateinit var graph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        graph = AppGraph(applicationContext = this)
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components { add(VideoFrameDecoder.Factory()) }
            .build()
}
