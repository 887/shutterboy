package com.eight87.shutterboy.ui.nav

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.compositionLocalOf

/**
 * Phase F.7 — `SharedTransitionScope` for grid → viewer shared-element
 * transition. The scope is provided by [ShutterboyApp] (which wraps the
 * `NavDisplay` in a `SharedTransitionLayout`); leaf composables (the
 * Photos timeline tile + the viewer's [androidx.compose.foundation.AsyncImage]
 * page) read it via this composition local.
 *
 * Null when no `SharedTransitionLayout` is in scope (e.g. unit tests that
 * mount the viewer / tile directly via Robolectric). Callers must
 * null-check and fall back to a plain modifier in that case — F.7 ships
 * the shared element as a progressive enhancement on top of the existing
 * crossfade, never a hard requirement.
 *
 * Paired with `androidx.navigation3.ui.LocalNavAnimatedContentScope`,
 * which gives us the `AnimatedContentScope` the `sharedElement` modifier
 * needs as its second argument. Both must be non-null for the shared
 * element to wire up.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }

/** Stable shared-element key for the grid tile ↔ viewer page transition. */
fun photoSharedElementKey(photoId: Long): String = "photo-$photoId"

/**
 * Nullable mirror of `androidx.navigation3.ui.LocalNavAnimatedContentScope`.
 *
 * Navigation3's `LocalNavAnimatedContentScope` is declared non-nullable
 * with a default lambda that throws `IllegalStateException` outside a
 * `NavEntry`. Kotlin's Compose compiler also forbids `try/catch` around
 * a composable read, so we can't just defensively read the upstream
 * local. Instead we mirror it into our own nullable local — the
 * `Register` extension for each route reads the upstream local (where
 * it's safe to do so) and publishes the value through this local.
 *
 * Leaf composables (the timeline tile, the viewer's AsyncImage) read
 * this local. Null means "no nav animated content scope" — i.e. we're
 * inside a Robolectric unit-test mount that didn't go through the nav
 * graph, so the shared-element wiring falls back to a no-op modifier.
 */
val LocalAnimatedContentScopeOrNull = compositionLocalOf<AnimatedContentScope?> { null }

/**
 * Publishes the upstream `LocalNavAnimatedContentScope` into our nullable
 * mirror [LocalAnimatedContentScopeOrNull] for the lifetime of [content].
 * Call this from inside a `NavDisplay` entry composable — that's where
 * the upstream local is safe to read.
 */
@androidx.compose.runtime.Composable
fun WithNavAnimatedContentScope(content: @androidx.compose.runtime.Composable () -> Unit) {
    val scope = androidx.navigation3.ui.LocalNavAnimatedContentScope.current
    androidx.compose.runtime.CompositionLocalProvider(
        LocalAnimatedContentScopeOrNull provides scope,
        content = content,
    )
}
