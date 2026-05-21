package com.eight87.shutterboy.data.coil

import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import android.util.Size as AndroidSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/**
 * Walks new photos after a scan and calls
 * [android.content.ContentResolver.loadThumbnail] on each so the OS
 * materialises the MINI_KIND thumbnail on disk **before** the user
 * scrolls to it. The first call to `loadThumbnail` for an
 * un-thumbnailed photo is slow (~200-500 ms) because MediaStore reads
 * the full image, decodes it, generates the thumb, persists it.
 * Subsequent calls hit that on-disk cache in ~1-10 ms.
 *
 * Without this prewarm, the slow path runs at scroll time and the grid
 * stutters / shows spinners on tiles the user has never opened before.
 * Aves has the same trick (its own disk thumbnail cache populated at
 * scan time); this is our equivalent against the OS-owned cache.
 *
 * Lifecycle: long-lived (constructed once at AppGraph init, runs on
 * its own scope). Items are queued through a bounded channel and
 * drained by a small worker pool on `Dispatchers.IO`. Limited
 * parallelism so the prewarm doesn't fight visible-cell decodes that
 * hit the same `loadThumbnail` IPC.
 *
 * Discarded bitmap result is the entire point — we want the OS-side
 * persistence, not an in-memory cache entry.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ThumbnailPrewarmer(
    private val context: Context,
    scope: CoroutineScope,
    workerCount: Int = 2,
) {
    data class Item(val id: Long, val uri: Uri)

    // Unlimited buffer — scans may enqueue tens of thousands at once.
    // Memory cost is two longs + an Uri reference per pending item;
    // a 30k-photo library is ~1.5 MB queued, well under budget.
    private val queue = Channel<Item>(Channel.UNLIMITED)

    init {
        val ioDispatcher = Dispatchers.IO.limitedParallelism(workerCount)
        repeat(workerCount) {
            scope.launch(ioDispatcher) {
                for (item in queue) {
                    runCatching {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            context.contentResolver.loadThumbnail(
                                item.uri,
                                THUMB_SIZE,
                                null,
                            )
                        }
                    }
                    // Discard the returned bitmap — letting the GC
                    // reclaim it immediately. The OS-side persistence
                    // is what we wanted.
                }
            }
        }
    }

    fun enqueue(items: Collection<Item>) {
        if (items.isEmpty()) return
        Log.i("shutterboy", "thumbnail-prewarm: enqueueing ${items.size} items")
        for (item in items) {
            queue.trySend(item)
        }
    }

    companion object {
        // Match `ThumbnailPrefetcher.TINY_PX` — same tier that the
        // FAR ring submits to the in-process memory cache. Asking for
        // a smaller size persuades MediaStore to generate the small
        // MINI_KIND variant instead of a needlessly large one.
        private val THUMB_SIZE = AndroidSize(96, 96)
    }
}
