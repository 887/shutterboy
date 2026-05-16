package com.eight87.shutterboy.ui.viewer

import kotlin.math.abs

/**
 * Pure math helpers for the fullscreen viewer's gesture vocabulary.
 *
 * Compose / Android types are deliberately absent so this stays
 * JVM-testable without Robolectric.
 *
 * - [dismissScrimAlpha] maps the running swipe-down distance to the
 *   container alpha used by the (future) "photo slides down with the
 *   finger" polish. The plan's locked decision punts the visual polish
 *   to a follow-up — the math is wired here so the polish lands in a
 *   second pass without touching the gesture handler again.
 * - [exceedsDismissThreshold] and [exceedsInfoOpenThreshold] are the
 *   release-time predicates: have we accumulated enough vertical travel
 *   to count as a deliberate dismiss / info-open?
 */
internal object ViewerGestureMath {

    /** Locked at 128 dp by the viewer-gestures plan (Phase G.1). */
    const val DISMISS_THRESHOLD_DP: Float = 128f

    /** Locked at 64 dp by the viewer-gestures plan (Phase G.2). */
    const val INFO_OPEN_THRESHOLD_DP: Float = 64f

    /**
     * Container-alpha schedule for the swipe-down dismiss gesture.
     *
     *   alpha = 1 - (drag / threshold).coerceIn(0, 1)
     *
     * At rest the photo is fully opaque (α=1); at the threshold the
     * photo has faded fully out (α=0), revealing the surface beneath.
     * Past the threshold we clamp at 0 — no negative alpha, no
     * undershoot.
     *
     * Only downward drag accumulates dismiss travel; upward drag clamps
     * the input to 0 (used by the info-open gesture instead).
     */
    fun dismissScrimAlpha(dragPx: Float, thresholdPx: Float): Float {
        if (thresholdPx <= 0f) return 1f
        val downward = dragPx.coerceAtLeast(0f)
        val progress = (downward / thresholdPx).coerceIn(0f, 1f)
        return 1f - progress
    }

    /** Has the user dragged down past the dismiss threshold? */
    fun exceedsDismissThreshold(dragPx: Float, thresholdPx: Float): Boolean =
        dragPx >= thresholdPx

    /** Has the user dragged up past the info-open threshold? */
    fun exceedsInfoOpenThreshold(dragPx: Float, thresholdPx: Float): Boolean =
        -dragPx >= thresholdPx

    /**
     * Classifies a release event into one of three terminal states for
     * a vertical drag accumulator. Up-vs-down disambiguation uses
     * [abs] of the accumulator so a balanced (≈0) drag goes nowhere.
     */
    fun classifyRelease(
        dragPx: Float,
        dismissThresholdPx: Float,
        infoOpenThresholdPx: Float,
    ): ReleaseAction {
        if (abs(dragPx) < 0.5f) return ReleaseAction.None
        return when {
            dragPx >= dismissThresholdPx -> ReleaseAction.Dismiss
            -dragPx >= infoOpenThresholdPx -> ReleaseAction.OpenInfo
            else -> ReleaseAction.None
        }
    }

    enum class ReleaseAction { None, Dismiss, OpenInfo }
}
