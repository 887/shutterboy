package com.eight87.shutterboy.ui.photos.grid

import com.eight87.shutterboy.domain.Photo
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * One month-year section of the [PhotosZoomLevel.Items] timeline. The Aves
 * inline-band pattern (locked in C.2): each section is a full-width header
 * band followed by the photos taken in that month. Bands scroll with the
 * content — no overlay-pin.
 *
 * The grouping is pure logic so it's testable without Compose.
 */
data class PhotoSection(
    val yearMonth: YearMonth,
    val photos: List<Photo>,
)

/**
 * Group a flat photo list (already sorted by the caller via `PhotoSort`) into
 * month-year sections. Order of sections is the order photos appear — so a
 * `ByDateTaken(DESC)` input produces newest-first sections, an ASC input
 * produces oldest-first.
 */
internal fun groupByMonth(
    photos: List<Photo>,
    zone: ZoneId = ZoneId.systemDefault(),
): List<PhotoSection> {
    if (photos.isEmpty()) return emptyList()
    val sections = mutableListOf<PhotoSection>()
    var currentKey: YearMonth? = null
    var bucket = mutableListOf<Photo>()
    for (photo in photos) {
        val key = YearMonth.from(Instant.ofEpochMilli(photo.dateTakenMs).atZone(zone))
        val previous = currentKey
        if (key != previous) {
            if (previous != null && bucket.isNotEmpty()) {
                sections += PhotoSection(previous, bucket.toList())
            }
            currentKey = key
            bucket = mutableListOf()
        }
        bucket += photo
    }
    val finalKey = currentKey
    if (finalKey != null && bucket.isNotEmpty()) {
        sections += PhotoSection(finalKey, bucket.toList())
    }
    return sections
}

/**
 * Locale-aware band label format. `MAY 2026` in en-US, `MAI 2026` in fr-FR,
 * `5月 2026` in ja-JP, etc. — locale follows whatever Compose context it's
 * rendered in (caller passes `Locale.getDefault()`).
 */
internal fun formatMonthBand(yearMonth: YearMonth, locale: Locale = Locale.getDefault()): String =
    DateTimeFormatter.ofPattern("MMMM yyyy", locale)
        .format(yearMonth.atDay(1))
        .uppercase(locale)

/** Year-only band format (used at [PhotosZoomLevel.Days] and `.Months`). */
internal fun formatYearBand(year: Int): String = year.toString()

/** Month-only label (used inside a [PhotosZoomLevel.Months] tile). */
internal fun formatMonthTile(yearMonth: YearMonth, locale: Locale = Locale.getDefault()): String =
    DateTimeFormatter.ofPattern("MMM", locale)
        .format(yearMonth.atDay(1))
        .uppercase(locale)

/** Day-only label (used inside a [PhotosZoomLevel.Days] tile). */
internal fun formatDayTile(date: LocalDate, locale: Locale = Locale.getDefault()): String =
    DateTimeFormatter.ofPattern("EEE d", locale)
        .format(date)

/**
 * Per-density display item. The `LazyVerticalGrid` consumes a flat list of
 * these, mapping each to the appropriate item / span. Sealed dispatch keeps
 * the per-level rendering exhaustive and OCP-friendly: adding a new density
 * level is one new sealed-case + one new render-branch.
 */
sealed interface TimelineDisplayItem {
    /** Full-width inline band with `MAY 2026` style label. */
    data class MonthYearBand(val yearMonth: YearMonth) : TimelineDisplayItem

    /** Full-width inline band with `2026` style label. */
    data class YearBand(val year: Int) : TimelineDisplayItem

    /** A single photo cell — only used at the [PhotosZoomLevel.Items] level. */
    data class PhotoCell(val photo: Photo) : TimelineDisplayItem

    /** A day-aggregate tile — cover photo + day label overlay. */
    data class DayCell(
        val date: LocalDate,
        val cover: Photo,
        val photoCount: Int,
    ) : TimelineDisplayItem

    /** A month-aggregate tile — cover photo + month label overlay. */
    data class MonthCell(
        val yearMonth: YearMonth,
        val cover: Photo,
        val photoCount: Int,
    ) : TimelineDisplayItem

    /** A year-aggregate tile — cover photo + year label overlay. */
    data class YearCell(
        val year: Int,
        val cover: Photo,
        val photoCount: Int,
    ) : TimelineDisplayItem
}

/**
 * Fold a flat photo list into the per-level display item list. Caller is
 * responsible for sorting input — this function preserves input order
 * section-by-section, so a newest-first input produces a newest-first
 * timeline.
 *
 * Pure logic for unit-testability.
 */
internal fun buildTimeline(
    photos: List<Photo>,
    level: PhotosZoomLevel,
    zone: ZoneId = ZoneId.systemDefault(),
): List<TimelineDisplayItem> {
    if (photos.isEmpty()) return emptyList()
    // Grouping (Items / Days / Months / Years) drives what each cell
    // represents and which section bands appear. Column count is a
    // separate independent control — see PhotosScreen.columnCount.
    return when (level) {
        PhotosZoomLevel.Items -> buildItemsTimeline(photos, zone)
        PhotosZoomLevel.Days -> buildDaysTimeline(photos, zone)
        PhotosZoomLevel.Months -> buildMonthsTimeline(photos, zone)
        PhotosZoomLevel.Years -> buildYearsTimeline(photos, zone)
    }
}

private fun buildItemsTimeline(photos: List<Photo>, zone: ZoneId): List<TimelineDisplayItem> {
    val out = mutableListOf<TimelineDisplayItem>()
    val sections = groupByMonth(photos, zone)
    for (section in sections) {
        out += TimelineDisplayItem.MonthYearBand(section.yearMonth)
        for (photo in section.photos) out += TimelineDisplayItem.PhotoCell(photo)
    }
    return out
}

private fun buildDaysTimeline(photos: List<Photo>, zone: ZoneId): List<TimelineDisplayItem> {
    val out = mutableListOf<TimelineDisplayItem>()
    var currentYear: Int? = null
    var currentDate: LocalDate? = null
    var dayCover: Photo? = null
    var dayCount = 0
    fun flushDay() {
        val d = currentDate
        val c = dayCover
        if (d != null && c != null && dayCount > 0) {
            out += TimelineDisplayItem.DayCell(date = d, cover = c, photoCount = dayCount)
        }
        currentDate = null
        dayCover = null
        dayCount = 0
    }
    for (photo in photos) {
        val zdt = Instant.ofEpochMilli(photo.dateTakenMs).atZone(zone)
        val date = zdt.toLocalDate()
        val year = date.year
        if (year != currentYear) {
            flushDay()
            out += TimelineDisplayItem.YearBand(year)
            currentYear = year
        }
        if (date != currentDate) {
            flushDay()
            currentDate = date
            dayCover = photo
        }
        dayCount += 1
    }
    flushDay()
    return out
}

private fun buildMonthsTimeline(photos: List<Photo>, zone: ZoneId): List<TimelineDisplayItem> {
    val out = mutableListOf<TimelineDisplayItem>()
    var currentYear: Int? = null
    var currentMonth: YearMonth? = null
    var monthCover: Photo? = null
    var monthCount = 0
    fun flushMonth() {
        val m = currentMonth
        val c = monthCover
        if (m != null && c != null && monthCount > 0) {
            out += TimelineDisplayItem.MonthCell(yearMonth = m, cover = c, photoCount = monthCount)
        }
        currentMonth = null
        monthCover = null
        monthCount = 0
    }
    for (photo in photos) {
        val ym = YearMonth.from(Instant.ofEpochMilli(photo.dateTakenMs).atZone(zone))
        if (ym.year != currentYear) {
            flushMonth()
            out += TimelineDisplayItem.YearBand(ym.year)
            currentYear = ym.year
        }
        if (ym != currentMonth) {
            flushMonth()
            currentMonth = ym
            monthCover = photo
        }
        monthCount += 1
    }
    flushMonth()
    return out
}

private fun buildYearsTimeline(photos: List<Photo>, zone: ZoneId): List<TimelineDisplayItem> {
    val out = mutableListOf<TimelineDisplayItem>()
    var currentYear: Int? = null
    var yearCover: Photo? = null
    var yearCount = 0
    fun flushYear() {
        val y = currentYear
        val c = yearCover
        if (y != null && c != null && yearCount > 0) {
            out += TimelineDisplayItem.YearCell(year = y, cover = c, photoCount = yearCount)
        }
        currentYear = null
        yearCover = null
        yearCount = 0
    }
    for (photo in photos) {
        val year = Instant.ofEpochMilli(photo.dateTakenMs).atZone(zone).year
        if (year != currentYear) {
            flushYear()
            currentYear = year
            yearCover = photo
        }
        yearCount += 1
    }
    flushYear()
    return out
}

/**
 * Phase C.4 — pointer used by [YearScrubber]. Pairs a year with the
 * timeline index of the first item belonging to that year. Markers are
 * emitted in timeline order: a `ByDateTaken(DESC)` input produces
 * newest-year-first markers, top of the strip = top of the timeline.
 */
data class YearMarker(val year: Int, val timelineIndex: Int)

/**
 * Walk a flat [TimelineDisplayItem] list once, emitting one [YearMarker]
 * per distinct year at the index of that year's first occurrence. Pure
 * logic so the scrubber can be tested without Compose. Items that don't
 * carry a year ([TimelineDisplayItem.PhotoCell] at Items density does, via
 * the preceding [TimelineDisplayItem.MonthYearBand]) are skipped here —
 * the band that opened the year already produced the marker.
 */
internal fun extractYearMarkers(timeline: List<TimelineDisplayItem>): List<YearMarker> {
    val markers = mutableListOf<YearMarker>()
    var lastYear: Int? = null
    timeline.forEachIndexed { idx, item ->
        val y = item.yearOrNull() ?: return@forEachIndexed
        if (y != lastYear) {
            markers += YearMarker(y, idx)
            lastYear = y
        }
    }
    return markers
}

private fun TimelineDisplayItem.yearOrNull(): Int? = when (this) {
    is TimelineDisplayItem.MonthYearBand -> yearMonth.year
    is TimelineDisplayItem.YearBand -> year
    // Flat-grid mode: derive year directly from the photo's capture
    // date so the scrubber's marker extraction still produces year pills
    // without the (now-removed) band items walking the timeline.
    is TimelineDisplayItem.PhotoCell -> java.time.Instant.ofEpochMilli(photo.dateTakenMs)
        .atZone(java.time.ZoneId.systemDefault())
        .year
    is TimelineDisplayItem.DayCell -> date.year
    is TimelineDisplayItem.MonthCell -> yearMonth.year
    is TimelineDisplayItem.YearCell -> year
}

/**
 * Map a [0..1] drag fraction to the marker at that fraction of the strip.
 * Empty markers → null. Fractions are clamped, so an out-of-range gesture
 * jumps to the nearest end.
 */
internal fun yearAtFraction(markers: List<YearMarker>, fraction: Float): YearMarker? {
    if (markers.isEmpty()) return null
    val clamped = fraction.coerceIn(0f, 1f)
    val idx = kotlin.math.round(clamped * (markers.size - 1)).toInt()
    return markers[idx.coerceIn(0, markers.size - 1)]
}

/**
 * Phase C.5 — derive the sticky-header label for the visible range's
 * first item. Walks back from [visibleIndex] to the most recent band /
 * tile that carries a date and formats it according to [level]:
 * `MAY 2026` at Items, `2026` at Days/Months/Years. Pure logic so the
 * banner can be tested without Compose.
 */
internal fun stickyHeaderLabel(
    timeline: List<TimelineDisplayItem>,
    visibleIndex: Int,
    level: PhotosZoomLevel,
    locale: Locale = Locale.getDefault(),
): String? {
    if (timeline.isEmpty() || visibleIndex < 0) return null
    val clamped = visibleIndex.coerceAtMost(timeline.size - 1)
    for (i in clamped downTo 0) {
        when (val item = timeline[i]) {
            is TimelineDisplayItem.MonthYearBand ->
                return formatMonthBand(item.yearMonth, locale)
            is TimelineDisplayItem.YearBand -> return formatYearBand(item.year)
            is TimelineDisplayItem.DayCell -> return formatYearBand(item.date.year)
            is TimelineDisplayItem.MonthCell -> return formatYearBand(item.yearMonth.year)
            is TimelineDisplayItem.YearCell -> return formatYearBand(item.year)
            // Flat-grid mode: derive `MAY 2026` directly from the
            // photo's capture date so the scrubber's floating pill
            // shows the month even without band items in the timeline.
            is TimelineDisplayItem.PhotoCell -> {
                val ym = java.time.Instant.ofEpochMilli(item.photo.dateTakenMs)
                    .atZone(java.time.ZoneId.systemDefault())
                    .let { java.time.YearMonth.of(it.year, it.month) }
                return formatMonthBand(ym, locale)
            }
        }
    }
    return null
}
