package com.eight87.shutterboy.ui.photos.grid

import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Size as AndroidSize
import coil3.ImageLoader
import coil3.asImage
import coil3.memory.MemoryCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Bounded LIFO prefetch queue feeding a fixed pool of workers off the
 * main thread, bypassing Coil's request queue entirely.
 *
 * Workers call `ContentResolver.loadThumbnail` directly (same API the
 * Aves Android plugin uses) and stuff the resulting Bitmap straight
 * into Coil's memory cache via [MemoryCache.set]. AsyncImage later
 * gets an instant memory-cache hit when it composes — no fade, no
 * decode flash, no queue contention with visible-tile requests.
 *
 *  - LIFO order: newest submission pops first.
 *  - Bounded ([maxPending]): oldest entries drop when over capacity.
 *  - Per-id memory-cache short-circuit on submit so we don't re-queue
 *    items already warm.
 *  - All work on [Dispatchers.IO].
 */
class ThumbnailPrefetcher(
    private val context: Context,
    private val loader: ImageLoader,
    private val targetPx: Int,
    private val maxPending: Int,
    workerCount: Int = 4,
    scope: CoroutineScope,
) {

    data class Task(val id: Long, val uri: Uri, val cacheKey: String)

    private val deque = ArrayDeque<Task>()
    private val signal = Channel<Unit>(Channel.UNLIMITED)
    private val lock = Mutex()

    init {
        repeat(workerCount) {
            scope.launch(Dispatchers.IO) { worker() }
        }
    }

    suspend fun submit(id: Long, uri: Uri) {
        val cacheKey = "thumb-$id-$targetPx"
        if (loader.memoryCache?.get(MemoryCache.Key(cacheKey)) != null) return
        lock.withLock {
            deque.addLast(Task(id, uri, cacheKey))
            while (deque.size > maxPending) deque.removeFirst()
        }
        signal.trySend(Unit)
    }

    suspend fun clear() {
        lock.withLock { deque.clear() }
    }

    private suspend fun worker() {
        while (true) {
            val task = lock.withLock { deque.removeLastOrNull() }
            if (task == null) {
                signal.receive()
                continue
            }
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val bitmap = context.contentResolver.loadThumbnail(
                        task.uri,
                        AndroidSize(targetPx, targetPx),
                        null,
                    )
                    loader.memoryCache?.set(
                        MemoryCache.Key(task.cacheKey),
                        MemoryCache.Value(bitmap.asImage()),
                    )
                }
            }
            // Failures are swallowed — non-fatal per tile; we just
            // don't pre-warm that one and AsyncImage will retry via
            // its normal path.
        }
    }
}
