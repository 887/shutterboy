package com.eight87.shutterboy.ui.photos.grid

import com.eight87.shutterboy.domain.sort.PhotoSort
import java.time.ZoneId
import java.util.Locale

/**
 * Phase B.2 — walk a flat [TimelineDisplayItem] list and emit one
 * `(timelineIndex, label)` per section start, where "section" is
 * defined by the current [sort] axis:
 *
 *  - `ByDateTaken` / `ByDateAdded` at Items density: section boundary
 *    is each [TimelineDisplayItem.MonthYearBand]; label is `MMM yyyy`.
 *  - `ByDateTaken` / `ByDateAdded` at Days+ densities: section
 *    boundary is each [TimelineDisplayItem.YearBand]; label is the
 *    year.
 *  - `ByName`: walk `PhotoCell` (and tile cover photos) emitting at
 *    each first-letter transition.
 *  - `BySize`: walk `PhotoCell` (and tile cover photos) emitting at
 *    each size-bucket transition.
 *
 * Pure logic so the call site can `remember(timeline, sort)` over it
 * cheaply and tests don't need Compose.
 */
fun sectionStartsFrom(
    timeline: List<TimelineDisplayItem>,
    sort: PhotoSort,
    locale: Locale = Locale.getDefault(),
    zone: ZoneId = ZoneId.systemDefault(),
    sizeBucketLabels: SizeBucketLabels = SizeBucketLabels.EnglishDefaults,
): List<Pair<Int, String>> {
    if (timeline.isEmpty()) return emptyList()

    return when (sort) {
        is PhotoSort.ByDateTaken, is PhotoSort.ByDateAdded ->
            dateBandStarts(timeline, locale)
        is PhotoSort.ByName, is PhotoSort.BySize ->
            walkPhotoTransitions(timeline, sort, locale, zone, sizeBucketLabels)
    }
}

private fun dateBandStarts(
    timeline: List<TimelineDisplayItem>,
    locale: Locale,
): List<Pair<Int, String>> {
    val out = mutableListOf<Pair<Int, String>>()
    timeline.forEachIndexed { idx, item ->
        when (item) {
            is TimelineDisplayItem.MonthYearBand ->
                out += idx to formatMonthBand(item.yearMonth, locale)
            is TimelineDisplayItem.YearBand ->
                out += idx to formatYearBand(item.year)
            else -> Unit
        }
    }
    return out
}

private fun walkPhotoTransitions(
    timeline: List<TimelineDisplayItem>,
    sort: PhotoSort,
    locale: Locale,
    zone: ZoneId,
    sizeBucketLabels: SizeBucketLabels,
): List<Pair<Int, String>> {
    val out = mutableListOf<Pair<Int, String>>()
    var lastLabel: String? = null
    timeline.forEachIndexed { idx, item ->
        val photo = when (item) {
            is TimelineDisplayItem.PhotoCell -> item.photo
            is TimelineDisplayItem.DayCell -> item.cover
            is TimelineDisplayItem.MonthCell -> item.cover
            is TimelineDisplayItem.YearCell -> item.cover
            is TimelineDisplayItem.MonthYearBand,
            is TimelineDisplayItem.YearBand,
            -> null
        } ?: return@forEachIndexed
        val label = sectionLabelFor(sort, photo, locale, zone, sizeBucketLabels)
        if (label != lastLabel) {
            out += idx to label
            lastLabel = label
        }
    }
    return out
}
