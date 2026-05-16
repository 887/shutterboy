package com.eight87.shutterboy.ui.photos.grid

import com.eight87.shutterboy.domain.Photo
import com.eight87.shutterboy.domain.sort.PhotoSort
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.util.Locale

/**
 * Phase B.1 — per-axis section-label derivation for the floaty
 * scrollbar chips.
 *
 * Date axes collapse to `MMM yyyy` (re-uses [formatMonthBand]). Name
 * axis returns the first letter of `displayName` (locale-uppercased);
 * the leading-article strip lands when the multilingual sort
 * comparator extension does (post-T.E). Size axis returns a bucket
 * label resolved by [SizeBucketLabels] — caller passes resolved
 * strings so this helper stays pure / resource-free.
 *
 * Pure logic; testable without Compose or Android resources.
 */
fun sectionLabelFor(
    sort: PhotoSort,
    photo: Photo,
    locale: Locale = Locale.getDefault(),
    zone: ZoneId = ZoneId.systemDefault(),
    sizeBucketLabels: SizeBucketLabels = SizeBucketLabels.EnglishDefaults,
): String = when (sort) {
    is PhotoSort.ByDateTaken ->
        formatMonthBand(YearMonth.from(Instant.ofEpochMilli(photo.dateTakenMs).atZone(zone)), locale)
    is PhotoSort.ByDateAdded ->
        formatMonthBand(YearMonth.from(Instant.ofEpochMilli(photo.dateAddedMs).atZone(zone)), locale)
    is PhotoSort.ByName ->
        firstLetterLabel(photo.displayName, locale)
    is PhotoSort.BySize ->
        sizeBucketLabel(photo.sizeBytes, sizeBucketLabels)
}

/**
 * Localised size-bucket labels. Caller resolves these from
 * `R.string.photos_size_bucket_*` and passes the struct; defaults are
 * English fallbacks for tests + early development.
 */
data class SizeBucketLabels(
    val underOneMb: String,
    val oneToFiveMb: String,
    val fiveToTwentyMb: String,
    val overTwentyMb: String,
) {
    companion object {
        val EnglishDefaults = SizeBucketLabels(
            underOneMb = "< 1 MB",
            oneToFiveMb = "1-5 MB",
            fiveToTwentyMb = "5-20 MB",
            overTwentyMb = "> 20 MB",
        )
    }
}

private const val ONE_MB = 1L * 1024 * 1024
private const val FIVE_MB = 5L * ONE_MB
private const val TWENTY_MB = 20L * ONE_MB

private fun sizeBucketLabel(sizeBytes: Long, labels: SizeBucketLabels): String = when {
    sizeBytes < ONE_MB -> labels.underOneMb
    sizeBytes < FIVE_MB -> labels.oneToFiveMb
    sizeBytes < TWENTY_MB -> labels.fiveToTwentyMb
    else -> labels.overTwentyMb
}

private fun firstLetterLabel(displayName: String, locale: Locale): String {
    val trimmed = displayName.trim()
    if (trimmed.isEmpty()) return "#"
    val first = trimmed.codePointAt(0)
    val ch = String(Character.toChars(first))
    return if (Character.isLetter(first)) ch.uppercase(locale) else "#"
}
