package com.eight87.shutterboy.ui.photos

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.eight87.shutterboy.R
import com.eight87.shutterboy.ui.nav.RouteScope

/**
 * Phase C.1 — placeholder body for the Photos tab. Phase C.2 replaces this
 * with the real timeline scaffold (`LazyVerticalGrid` + inline month-year
 * bands + density zoom + year scrubber).
 *
 * Per refactor-solid R.D, when the real screen lands `PhotosScreen.kt`
 * stays at ~200 LOC ceiling — scaffold + top-bar + dispatch only — and
 * sub-pieces (grid / band / scrubber / empty-state / multiselect) live in
 * separate files under `ui/photos/grid/` and `ui/photos/multiselect/`.
 */
@Composable
fun PhotosScreen(
    @Suppress("UNUSED_PARAMETER") scope: RouteScope,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(id = R.string.photos_placeholder_title),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
