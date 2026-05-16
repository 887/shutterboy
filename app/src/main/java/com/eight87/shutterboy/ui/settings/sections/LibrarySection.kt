package com.eight87.shutterboy.ui.settings.sections

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.eight87.shutterboy.R
import com.eight87.shutterboy.data.repo.LibraryScanner
import com.eight87.shutterboy.ui.settings.catalog.SettingsCard
import com.eight87.shutterboy.ui.settings.catalog.SettingsRow
import kotlinx.coroutines.launch

/**
 * Phase I.3 — Library section card. Surfaces the "Rescan photos" row
 * that drives [LibraryScanner.forceRescan] (clears the cold-start gate
 * + walks MediaStore + every SAF tree). Tap shows a "Rescanning…"
 * snackbar; completion replaces it with "Rescan complete — N photos".
 *
 * The full sub-page (Manage sources / Clear cache, per main.md I.3)
 * lands later — this ships the rescan entry inline on the Settings
 * root so Library state stops being invisible.
 */
@Composable
fun LibrarySection(
    libraryScanner: LibraryScanner,
    snackbar: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    val inProgressMessage = stringResource(R.string.settings_library_rescan_in_progress)
    val completeMessageFormat = stringResource(R.string.settings_library_rescan_complete)
    LibrarySection(
        modifier = modifier,
        onRescan = {
            // Two independent coroutines so the rescan starts immediately
            // instead of waiting for `showSnackbar` to suspend-return. The
            // ScanProgressStrip in the app shell observes the same
            // LibraryScanner.scanProgress() Flow, so even if the user
            // dismisses the snackbar, the progress bar still surfaces the
            // in-flight scan.
            coroutineScope.launch {
                snackbar.showSnackbar(inProgressMessage)
            }
            coroutineScope.launch {
                val snapshot = libraryScanner.forceRescan()
                snackbar.showSnackbar(completeMessageFormat.format(snapshot.photos.size))
            }
        },
    )
}

/**
 * Test seam — pure composable that takes a tap handler. The
 * Robolectric test mounts this variant with a counter on `onRescan` so
 * it doesn't need to spin up a real `SnackbarHostState` or
 * `LibraryScanner`.
 */
@Composable
internal fun LibrarySection(
    onRescan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsCard(
        modifier = modifier,
        title = stringResource(R.string.settings_section_library),
    ) {
        SettingsRow(
            id = "settings_library_rescan",
            icon = Icons.Filled.Refresh,
            label = stringResource(R.string.settings_library_rescan),
            subtitle = stringResource(R.string.settings_library_rescan_subtitle),
            onClick = onRescan,
        )
    }
}
