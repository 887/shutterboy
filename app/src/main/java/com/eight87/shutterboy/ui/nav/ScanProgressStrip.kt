package com.eight87.shutterboy.ui.nav

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eight87.shutterboy.R
import com.eight87.shutterboy.data.repo.LibraryScanner
import com.eight87.shutterboy.domain.ScanProgress

/**
 * Tonearmboy-style scan progress bar, mounted at the top of the app
 * shell. Surfaces [LibraryScanner.scanProgress] as a richer status
 * strip — `<scanned>/<total>` + percent on the right, a determinate
 * (or indeterminate) [LinearProgressIndicator], and an optional
 * one-line "current title" caption (the display name of the photo or
 * video the scanner is processing).
 *
 * Renders only while a scan is `Running`; collapses to zero-height
 * otherwise so the chrome height stays stable. The producer side
 * throttles emissions to ~5 Hz so this composable never recomposes
 * faster than a human can read it.
 */
@Composable
fun ScanProgressStrip(
    scanner: LibraryScanner,
    modifier: Modifier = Modifier,
) {
    val progress by scanner.scanProgress()
        .collectAsStateWithLifecycle(initialValue = ScanProgress.Idle)
    val running = progress as? ScanProgress.Running
    AnimatedVisibility(
        visible = running != null,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier.semantics { testTag = "scan_progress_strip" },
    ) {
        val p = running ?: return@AnimatedVisibility
        val total = p.total ?: 0
        Surface(
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (total > 0) {
                            stringResource(
                                R.string.library_scan_progress_with_total,
                                p.processed,
                                total,
                            )
                        } else {
                            stringResource(R.string.library_scan_progress_indeterminate)
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (total > 0) {
                        Text(
                            text = stringResource(
                                R.string.library_scan_progress_percent,
                                (p.fraction * 100).toInt(),
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (total > 0) {
                    LinearProgressIndicator(
                        progress = { p.fraction.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                val title = p.currentTitle
                if (!title.isNullOrBlank()) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
