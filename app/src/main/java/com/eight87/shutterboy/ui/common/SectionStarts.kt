package com.eight87.shutterboy.ui.common

/**
 * Build the `sectionStarts` parameter expected by [FastScrollbar],
 * assuming the consumer lays each section out as one full-width header
 * cell followed by N item cells (the shape `PhotosGrid` uses at
 * `PhotosZoomLevel.Items`).
 *
 * Given the section labels in display order and a map of label → item
 * count, emits `(timelineIndex, label)` pointing at each section
 * header. The chip the scrollbar renders for that label will then sit
 * at the fractional Y of that index in the flat layout.
 *
 * Pure logic so it's testable without Compose.
 */
fun sectionStartsFromHeaderRuns(
    sectionLabels: List<String>,
    itemCountsBySection: Map<String, Int>,
): List<Pair<Int, String>> {
    if (sectionLabels.isEmpty()) return emptyList()
    val out = ArrayList<Pair<Int, String>>(sectionLabels.size)
    var cursor = 0
    for (label in sectionLabels) {
        out += cursor to label
        cursor += 1 + (itemCountsBySection[label] ?: 0)
    }
    return out
}
