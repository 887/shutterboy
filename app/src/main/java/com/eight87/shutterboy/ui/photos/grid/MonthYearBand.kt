package com.eight87.shutterboy.ui.photos.grid

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import java.time.YearMonth

/**
 * Aves-pattern inline section header. Full-width band that scrolls with
 * the photos beneath it (NOT pinned). Lives inside the LazyVerticalGrid as
 * `item(span = { GridItemSpan(maxLineSpan) }) { MonthYearBand(...) }`.
 *
 * Locale follows the system; the band label uses [formatMonthBand].
 */
@Composable
internal fun MonthYearBand(
    yearMonth: YearMonth,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales.get(0) ?: java.util.Locale.getDefault()
    Text(
        text = formatMonthBand(yearMonth, locale),
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    )
}
