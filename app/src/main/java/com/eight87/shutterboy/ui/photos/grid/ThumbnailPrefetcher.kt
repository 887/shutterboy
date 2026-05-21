package com.eight87.shutterboy.ui.photos.grid

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.CancellationSignal
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

    data class Task(val id: Long, val uri: Uri, val cacheKey: String, val px: Int)

    private val deque = ArrayDeque<Task>()

    private val signal = Channel<Unit>(Channel.UNLIMITED)
    private val lock = Mutex()

    private data class InFlightEntry(val id: Long, val signal: CancellationSignal)
    private val inFlight = HashMap<String, InFlightEntry>()

    init {
        repeat(workerCount) {
            scope.launch(Dispatchers.IO) { worker() }
        }
    }

    /**
     * Submit a target-sized prefetch for [id]. Items already warm in
     * Coil's memory cache short-circuit; otherwise the task is pushed
     * to the LIFO front so the worker pool picks it up next.
     */
    suspend fun submit(id: Long, uri: Uri) {
        val key = "thumb-$id-$targetPx"
        if (loader.memoryCache?.get(MemoryCache.Key(key)) != null) return
        lock.withLock {
            deque.addLast(Task(id, uri, key, targetPx))
            while (deque.size > maxPending) deque.removeFirst()
        }
        signal.trySend(Unit)
    }

    suspend fun clear() {
        lock.withLock { deque.clear() }
    }

    /**
     * Keep only tasks (pending + in-flight) whose id is in [keepIds].
     * Pending tasks outside the set are dropped; in-flight ones get
     * their `CancellationSignal` fired so `loadThumbnail` aborts
     * mid-flight. Called by the grid scheduler when the prefetch window
     * shifts (typical trigger: scroll direction reverses, items that
     * were "ahead" become "way behind" and aren't worth completing).
     */
    suspend fun retain(keepIds: Set<Long>) {
        val toCancel: List<CancellationSignal> = lock.withLock {
            deque.removeAll { it.id !in keepIds }
            inFlight.values
                .filter { it.id !in keepIds }
                .map { it.signal }
        }
        toCancel.forEach { runCatching { it.cancel() } }
    }

    private suspend fun worker() {
        while (true) {
            val task = lock.withLock { deque.removeLastOrNull() }
            if (task == null) {
                signal.receive()
                continue
            }
            val cancelSignal = CancellationSignal()
            lock.withLock { inFlight[task.cacheKey] = InFlightEntry(task.id, cancelSignal) }
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val raw = context.contentResolver.loadThumbnail(
                        task.uri,
                        AndroidSize(task.px, task.px),
                        cancelSignal,
                    )
                    // OS may return larger than requested (MINI_KIND
                    // 512x384 for camera 4K sources). Resize on the IO
                    // thread before HARDWARE-copying — preserves aspect
                    // ratio so cells aren't stretched into squares.
                    val resized = com.eight87.shutterboy.data.coil.aspectFit(
                        raw, task.px, task.px,
                    )
                    if (resized !== raw) raw.recycle()
                    // Copy to HARDWARE config so the bitmap lives in
                    // GPU memory and draws are zero-copy. Software
                    // bitmaps would re-upload to the GPU every frame
                    // they're drawn, which is exactly the micro-stutter
                    // signature when many tiles are visible at once
                    // (worse in portrait where the visible row count is
                    // higher). Falls back to the original ARGB_8888 if
                    // hardware copy fails (rare — only on degraded GPUs).
                    val hw = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        runCatching { resized.copy(Bitmap.Config.HARDWARE, false) }
                            .getOrNull() ?: resized
                    } else {
                        resized
                    }
                    loader.memoryCache?.set(
                        MemoryCache.Key(task.cacheKey),
                        MemoryCache.Value(hw.asImage()),
                    )
                }
            }
            lock.withLock { inFlight.remove(task.cacheKey) }
            // Failures are swallowed — non-fatal per tile; we just
            // don't pre-warm that one and AsyncImage will retry via
            // its normal path. `OperationCanceledException` from a
            // cancelled signal lands here too and is correctly ignored.
        }
    }
}
