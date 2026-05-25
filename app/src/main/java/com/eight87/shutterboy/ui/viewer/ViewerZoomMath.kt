package com.eight87.shutterboy.ui.viewer

/**
 * Pure math helpers for the fullscreen viewer's pinch/double-tap zoom
 * (viewer-gestures Phase G.3.1 + G.3.2).
 *
 * Compose / Android types are deliberately absent so this stays
 * JVM-testable without Robolectric.
 *
 * - [clampScale] applies the locked `[MIN_SCALE, MAX_SCALE]` range to a
 *   pinch-accumulated scale value.
 * - [toggledScale] flips between rest (1.0) and the locked double-tap
 *   target (2.0) — used by the double-tap gesture in `PhotoViewerScreen`.
 * - [isZoomed] gates pan accumulation and pager swipe enablement: pan
 *   only applies when zoomed in past rest; pager swipe only enabled at
 *   rest.
 */
internal object ViewerZoomMath {

    /** Rest scale; pager swipe + vertical-drag gestures are enabled here. */
    const val MIN_SCALE: Float = 1f

    /** Cap on pinch zoom — beyond 3x detail isn't useful on a phone. */
    const val MAX_SCALE: Float = 3f

    /** Double-tap target — single discrete step between rest and a usable zoom. */
    const val DOUBLE_TAP_ZOOMED_SCALE: Float = 2f

    /** Clamps a pinch-accumulated scale to the locked `[MIN_SCALE, MAX_SCALE]` range. */
    fun clampScale(scale: Float): Float = scale.coerceIn(MIN_SCALE, MAX_SCALE)

    /**
     * Double-tap toggle: at (or below) rest, jump to [DOUBLE_TAP_ZOOMED_SCALE];
     * anywhere zoomed in, collapse back to rest. The "anywhere zoomed in"
     * branch matches Google Photos / most mainstream gallery UIs — a
     * double-tap while zoomed always returns to fit-to-screen.
     */
    fun toggledScale(current: Float): Float =
        if (current > MIN_SCALE) MIN_SCALE else DOUBLE_TAP_ZOOMED_SCALE

    /** True iff the image is zoomed in past rest — gates pan + pager-swipe behaviour. */
    fun isZoomed(scale: Float): Boolean = scale > MIN_SCALE

    /**
     * Clamp pan offsets so the scaled image can't fly off-screen.
     * At scale S the image extends `(S - 1) * viewport / 2` beyond
     * each edge; that's the maximum useful translation.
     */
    fun clampPan(
        panX: Float,
        panY: Float,
        scale: Float,
        viewportWidth: Float,
        viewportHeight: Float,
    ): Pair<Float, Float> {
        val maxX = (scale - 1f) * viewportWidth / 2f
        val maxY = (scale - 1f) * viewportHeight / 2f
        return panX.coerceIn(-maxX, maxX) to panY.coerceIn(-maxY, maxY)
    }
}
