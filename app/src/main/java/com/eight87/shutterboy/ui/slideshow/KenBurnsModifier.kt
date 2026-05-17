package com.eight87.shutterboy.ui.slideshow

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Phase J.2 — Ken Burns slow-zoom-and-drift effect for slideshow pages.
 *
 * Returns a `Modifier` that drives a `graphicsLayer` over the slide's
 * dwell duration:
 *   - `scaleX` / `scaleY` grow from 1.0 → [MAX_SCALE] (1.08).
 *   - `transformOrigin` is offset toward one of four corners, chosen
 *     deterministically from [photoId] (id % 4), so consecutive slides
 *     pan in visibly different directions instead of all drifting the
 *     same way.
 *
 * The animation resets per slide by keying the `LaunchedEffect` on
 * [photoId] — when the pager swaps pages, a fresh `Animatable` runs.
 * Driver: `LinearEasing` so the motion reads as continuous Ken-Burns
 * drift rather than an ease-in zoom.
 *
 * The slide's `AsyncImage` should sit inside a `Box` that clips so the
 * 8 % overscan doesn't bleed letterboxing on the edges.
 */
@Composable
internal fun rememberKenBurnsModifier(
    photoId: Long,
    dwellMs: Long,
): Modifier {
    val progress = remember(photoId) { Animatable(0f) }
    LaunchedEffect(photoId, dwellMs) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = dwellMs.toInt().coerceAtLeast(0),
                easing = LinearEasing,
            ),
        )
    }
    val origin = remember(photoId) { kenBurnsOriginFor(photoId) }
    return Modifier.graphicsLayer {
        val t = progress.value
        val scale = MIN_SCALE + (MAX_SCALE - MIN_SCALE) * t
        scaleX = scale
        scaleY = scale
        transformOrigin = origin
    }
}

/**
 * Deterministic origin for [photoId] — four corners cycle so the slide's
 * pan direction varies. Exposed `internal` for unit-test coverage.
 */
internal fun kenBurnsOriginFor(photoId: Long): TransformOrigin {
    // (id mod 4 + 4) mod 4 keeps negative ids safe — Long photo ids
    // should always be non-negative in practice, but cheap to be defensive.
    val bucket = ((photoId % 4L) + 4L) % 4L
    return when (bucket.toInt()) {
        0 -> TransformOrigin(0f, 0f)       // top-left
        1 -> TransformOrigin(1f, 0f)       // top-right
        2 -> TransformOrigin(0f, 1f)       // bottom-left
        else -> TransformOrigin(1f, 1f)    // bottom-right
    }
}

internal const val MIN_SCALE: Float = 1.0f
internal const val MAX_SCALE: Float = 1.08f
