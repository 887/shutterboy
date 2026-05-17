package com.eight87.shutterboy.ui.viewer

import android.net.Uri
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

/**
 * Inline video player for the photo viewer. Used by [PhotoViewerScreen]'s
 * `PhotoPage` when the row's MIME type starts with `video/`.
 *
 * Lifecycle contract:
 *  - One [ExoPlayer] instance per visible page (the pager may briefly
 *    keep two pages composed during a swipe, so each `PhotoPage` builds
 *    + owns its own player via `remember`).
 *  - `isActive = false` (i.e. the page is not the pager's current page)
 *    pauses playback so off-screen pages don't keep audio running.
 *  - `DisposableEffect` releases the player when the composable leaves
 *    the composition, and a `LifecycleEventObserver` pauses on
 *    `ON_PAUSE` so the host activity going to background doesn't leak
 *    audio output (Media3 quirk — the player keeps playing without it).
 *  - `useController = true` exposes Media3's stock seek bar / play-pause
 *    controls; tap on the PlayerView toggles them via Media3's own
 *    controller-show logic, so the parent's chrome-toggle tap callback
 *    is not wired in here (would fight Media3's controller).
 */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun VideoPlayerSurface(
    contentUri: Uri,
    isActive: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val exoPlayer = remember(contentUri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(contentUri))
            prepare()
            playWhenReady = true
        }
    }

    // Pause off-screen pages — the pager may have us briefly composed
    // alongside a sibling page during a swipe.
    LaunchedEffect(isActive, exoPlayer) {
        exoPlayer.playWhenReady = isActive
    }

    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> exoPlayer.playWhenReady = false
                Lifecycle.Event.ON_RESUME -> {
                    if (isActive) exoPlayer.playWhenReady = true
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.release()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = true
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
    }
}
