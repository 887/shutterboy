package com.eight87.shutterboy.ui.photos.grid

import android.util.Log
import coil3.ImageLoader
import coil3.memory.MemoryCache
import coil3.request.ImageRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Bounded LIFO prefetch queue feeding a fixed pool of workers off the
 * main thread.
 *
 *  - Newest submission goes to the top of the stack; workers pop from
 *    the top so the FRESHEST scroll position is processed first
 *    (Aves-style fast-scrub priority). Older entries get serviced
 *    only after the freshly-added ones drain.
 *  - The stack has a hard ceiling of [maxPending] (default ~3
 *    viewport-worths). When a new submission would overflow, the
 *    OLDEST (bottom) entries are dropped first — they're the stalest
 *    scroll positions and the user has moved past them.
 *  - [clear] flushes the entire queue. Call it when the scroll
 *    direction changes, when the user grabs the thumb to scrub, or any
 *    other "the world just moved" moment so workers don't waste cycles
 *    on now-irrelevant positions.
 *  - Submissions whose memory-cache key is already present in
 *    [loader]'s memory cache short-circuit — no double-decode.
 *
 * The whole thing lives on [Dispatchers.IO]: 4 worker coroutines steal
 * from a single `ArrayDeque` guarded by a [Mutex]. No work hits the
 * main thread.
 */
class ThumbnailPrefetcher(
    private val loader: ImageLoader,
    private val maxPending: Int,
    workerCount: Int = 4,
    scope: CoroutineScope,
) {
    private val deque = ArrayDeque<ImageRequest>()
    private val signal = Channel<Unit>(Channel.UNLIMITED)
    private val lock = Mutex()

    init {
        repeat(workerCount) {
            scope.launch(Dispatchers.IO) { worker() }
        }
    }

    suspend fun submit(req: ImageRequest) {
        val key = req.memoryCacheKey?.let { MemoryCache.Key(it) }
        if (key != null && loader.memoryCache?.get(key) != null) {
            Log.d(TAG, "skip ${req.memoryCacheKey} (cache hit)")
            return
        }
        val size = lock.withLock {
            deque.addLast(req)
            while (deque.size > maxPending) {
                deque.removeFirst()
            }
            deque.size
        }
        Log.d(TAG, "submit ${req.memoryCacheKey} (queue size: $size)")
        signal.trySend(Unit)
    }

    suspend fun clear() {
        val cleared = lock.withLock {
            val n = deque.size
            deque.clear()
            n
        }
        Log.d(TAG, "clear (dropped $cleared)")
    }

    private suspend fun worker() {
        Log.d(TAG, "worker started")
        while (true) {
            val req = lock.withLock { deque.removeLastOrNull() }
            if (req == null) {
                signal.receive()
            } else {
                Log.d(TAG, "decode ${req.memoryCacheKey}")
                runCatching { loader.execute(req) }
                    .onFailure { Log.w(TAG, "decode failed: ${req.memoryCacheKey}", it) }
            }
        }
    }

    companion object {
        private const val TAG = "ThumbnailPrefetcher"
    }
}
