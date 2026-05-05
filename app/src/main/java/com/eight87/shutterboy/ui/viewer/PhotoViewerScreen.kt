package com.eight87.shutterboy.ui.viewer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.eight87.shutterboy.R
import com.eight87.shutterboy.ui.nav.PhotoViewer
import com.eight87.shutterboy.ui.nav.RouteScope

/**
 * Phase C.6 — placeholder body for the [PhotoViewer] route. Phase F replaces
 * this with the real fullscreen pager + chrome + EXIF panel + delete /
 * share / edit-handoff. The route shape (photoIdValue + backingIds) is
 * already locked so the wiring upstream — thumbnail tap → push — doesn't
 * change when F lands.
 *
 * Renders a Scaffold with a back arrow + the tapped photo id + the size of
 * the backing list, so the wiring is visibly correct on the AVD until the
 * pager replaces it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoViewerScreen(
    destination: PhotoViewer,
    scope: RouteScope,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.viewer_placeholder_title)) },
                navigationIcon = {
                    IconButton(onClick = { scope.backStack.pop() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.cd_settings_back),
                        )
                    }
                },
            )
        },
        modifier = modifier.fillMaxSize(),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.viewer_placeholder_id, destination.photoIdValue),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = stringResource(
                    R.string.viewer_placeholder_backing_count,
                    destination.backingIds.size,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.viewer_placeholder_phase_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
