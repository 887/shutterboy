package com.eight87.shutterboy.ui.photos.grid

/**
 * **Grouping** mode for the Photos timeline — independent of column
 * count. Pinch-in / pinch-out cycle through more aggregation:
 * Items → Days → Months → Years.
 *
 *   - [Items]: one cell per photo, month-year section bands.
 *   - [Days]: one cover-tile per day, year section bands.
 *   - [Months]: one cover-tile per month, year section bands.
 *   - [Years]: one hero cover per year, no section bands.
 *
 * Column count is a separate, independent preference — see
 * [com.eight87.shutterboy.data.settings.DisplayPreferences.observeColumnCount].
 * Grouping and columns compose: a user can pick "Months" grouping
 * with 3 columns, or "Items" grouping with 5 columns, etc.
 */
enum class PhotosZoomLevel {
    Items, Days, Months, Years;

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
 * Suggested default column count for a given grouping — used when no
 * persisted column-count preference is set. Items grouping is the
 * densest, so it gets the most columns; Years grouping shows hero
 * cards so it defaults to one full-width per row.
 */
fun PhotosZoomLevel.defaultColumns(): Int = when (this) {
    PhotosZoomLevel.Items -> 4
    PhotosZoomLevel.Days -> 3
    PhotosZoomLevel.Months -> 2
    PhotosZoomLevel.Years -> 1
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
