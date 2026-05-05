package com.eight87.shutterboy.ui.sort

import androidx.annotation.StringRes
import com.eight87.shutterboy.R
import com.eight87.shutterboy.domain.sort.Direction
import com.eight87.shutterboy.domain.sort.PhotoSort

/**
 * Phase E.2 — UI-side projection of the [PhotoSort] sealed type. Splits
 * the sort into its two orthogonal dimensions (axis + direction) so the
 * sort sheet can render an axis radio list + an ASC/DESC toggle without
 * exhaustively listing the eight sort × direction combinations.
 *
 * `applyTo(direction)` reconstructs the sealed `PhotoSort` from the
 * (axis, direction) pair. `fromSort(sort)` projects in the other
 * direction. Both round-trips are total — there's no `null` shape.
 */
internal enum class SortAxis(@StringRes val labelRes: Int) {
    DateTaken(R.string.sort_axis_date_taken),
    DateAdded(R.string.sort_axis_date_added),
    Name(R.string.sort_axis_name),
    Size(R.string.sort_axis_size);

    fun applyTo(direction: Direction): PhotoSort = when (this) {
        DateTaken -> PhotoSort.ByDateTaken(direction)
        DateAdded -> PhotoSort.ByDateAdded(direction)
        Name -> PhotoSort.ByName(direction)
        Size -> PhotoSort.BySize(direction)
    }

    companion object {
        fun fromSort(sort: PhotoSort): SortAxis = when (sort) {
            is PhotoSort.ByDateTaken -> DateTaken
            is PhotoSort.ByDateAdded -> DateAdded
            is PhotoSort.ByName -> Name
            is PhotoSort.BySize -> Size
        }
    }
}
