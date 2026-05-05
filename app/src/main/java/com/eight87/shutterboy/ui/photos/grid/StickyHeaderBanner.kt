package com.eight87.shutterboy.ui.photos.grid

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import java.util.Locale

private const val FADE_MS = 200

/**
 * Phase C.5 — translucent month-year banner that fades in for ~200 ms
 * while the grid is scrolling and fades out when the scroll settles.
 * Label format follows the design doc: `MAY 2026` at
 * [PhotosZoomLevel.Items], `2026` at the aggregate densities. Pure
 * derivation lives in [stickyHeaderLabel] so the banner can be tested
 * without Compose.
 *
 * Sits as an overlay child of the grid `Box`, top-centred. Doesn't
 * intercept touch events.
 */
@Composable
internal fun StickyHeaderBanner(
    timeline: List<TimelineDisplayItem>,
    gridState: LazyGridState,
    level: PhotosZoomLevel,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales.get(0) ?: Locale.getDefault()
    val label by remember(timeline, level, locale) {
        derivedStateOf {
            stickyHeaderLabel(timeline, gridState.firstVisibleItemIndex, level, locale)
        }
    }
    val visible = gridState.isScrollInProgress && !label.isNullOrBlank()
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(FADE_MS)),
        exit = fadeOut(animationSpec = tween(FADE_MS)),
        modifier = modifier,
    ) {
        Surface(
            shape = RoundedCornerShape(percent = 50),
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
            tonalElevation = 6.dp,
            modifier = Modifier.padding(top = 12.dp),
        ) {
            Text(
                text = label.orEmpty(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
        }
    }
}
