package com.eight87.shutterboy.ui.photos.grid

import com.eight87.shutterboy.domain.Photo
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * One month-year section of the Photos timeline. The Aves inline-band pattern
 * (locked in C.2): each section is a full-width header band followed by the
 * photos taken in that month. Bands scroll with the content — no overlay-pin.
 *
 * The grouping is pure logic so it's testable without Compose. The
 * [PhotosGrid] composable builds a `LazyVerticalGrid` whose items list is
 * `groupByMonth(photos).flatMap { it.toLazyItems() }` (header + photo cells).
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
