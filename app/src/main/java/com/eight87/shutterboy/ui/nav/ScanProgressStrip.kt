package com.eight87.shutterboy.ui.nav

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eight87.shutterboy.data.repo.LibraryScanner
import com.eight87.shutterboy.domain.ScanProgress

/**
 * Thin indeterminate progress strip that surfaces [LibraryScanner.scanProgress].
 * Renders only while a scan is `Running`; collapses to zero-height when
 * `Idle` / `Done` / `Failed` so the chrome height stays stable. Mounted at
 * the top of the app shell so every surface sees it.
 *
 * Bound to the same Flow `forceRescan()` flips through Running → Done, so
 * the user gets a non-blocking visual signal that work is in flight while
 * the rest of the UI stays interactive.
 */
@Composable
fun ScanProgressStrip(
    scanner: LibraryScanner,
    modifier: Modifier = Modifier,
) {
    val progress by scanner.scanProgress()
        .collectAsStateWithLifecycle(initialValue = ScanProgress.Idle)
    val visible = progress is ScanProgress.Running
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier.semantics { testTag = "scan_progress_strip" },
    ) {
        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        )
    }
}
