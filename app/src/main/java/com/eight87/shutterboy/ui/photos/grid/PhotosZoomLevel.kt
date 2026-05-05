package com.eight87.shutterboy.ui.photos.grid

/**
 * Phase C.3 — four-level density zoom for the Photos timeline. Pinch-out
 * (positive zoom) takes the grid toward [Items] (denser, 4 columns of
 * thumbnails); pinch-in (negative zoom) takes it toward [Years] (sparser,
 * one hero cover per year).
 *
 * Pure enum + pure transitions so the gesture-handler can be unit-tested
 * without spinning up Compose.
 */
enum class PhotosZoomLevel(val columns: Int) {
    /** Default. One cell per photo, 4 columns; month-year section bands. */
    Items(columns = 4),

    /** One cover-tile per day, 3 columns; year section bands. */
    Days(columns = 3),

    /** One cover-tile per month, 2 columns; year section bands. */
    Months(columns = 2),

    /** One hero cover per year, 1 column; no section bands. */
    Years(columns = 1);

    /** Step toward more density. Clamps at [Items]. */
    fun zoomIn(): PhotosZoomLevel = when (this) {
        Years -> Months
        Months -> Days
        Days -> Items
        Items -> Items
    }

    /** Step toward more aggregation. Clamps at [Years]. */
    fun zoomOut(): PhotosZoomLevel = when (this) {
        Items -> Days
        Days -> Months
        Months -> Years
        Years -> Years
    }
}

/**
 * Pinch-zoom accumulator. Compose's `TransformableState.onTransformation`
 * delivers continuous `zoomChange` floats (1.0 = no change, > 1 = zoom in,
 * < 1 = zoom out). We multiply them into a running accumulator; on crossing a
 * threshold the accumulator resets and the level transitions. Pure data so
 * tests can drive it without `Modifier.transformable`.
 *
 * Default thresholds: 1.5× in (50 % zoom-in to advance), 0.66× out (33 %
 * zoom-out to retreat). Asymmetric so a user can hold a steady pinch and
 * advance multiple levels without snapping back when the gesture wobbles.
 */
data class ZoomAccumulator(
    val level: PhotosZoomLevel = PhotosZoomLevel.Items,
    val pendingZoom: Float = 1.0f,
) {
    fun apply(
        zoomChange: Float,
        zoomInThreshold: Float = 1.5f,
        zoomOutThreshold: Float = 0.66f,
    ): ZoomAccumulator {
        val next = pendingZoom * zoomChange
        return when {
            next >= zoomInThreshold -> ZoomAccumulator(level = level.zoomIn(), pendingZoom = 1.0f)
            next <= zoomOutThreshold -> ZoomAccumulator(level = level.zoomOut(), pendingZoom = 1.0f)
            else -> copy(pendingZoom = next)
        }
    }
}
