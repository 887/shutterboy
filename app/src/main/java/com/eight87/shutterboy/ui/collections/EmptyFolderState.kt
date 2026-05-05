package com.eight87.shutterboy.ui.collections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOff
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.eight87.shutterboy.R
import com.eight87.shutterboy.domain.SourceType

/**
 * Phase D.4 — empty-folder body. Two variants:
 *   - **Generic** ([SourceType.DEVICE] folders, or any folder that's just
 *     genuinely empty) — quiet "this folder is empty" copy.
 *   - **Revoked SAF** ([SourceType.SAF] folders that the user previously
 *     granted access to but whose tree URI is no longer authorised) —
 *     copy + a re-add CTA pointing at Settings → Library → Manage sources.
 *     The CTA copy currently doesn't navigate (Phase I.3 lands the page);
 *     once it lands, this composable will get an `onManageTap` parameter.
 *
 * Used as the [com.eight87.shutterboy.ui.photos.grid.PhotosGrid] empty-state
 * override from [FolderDetailScreen]. Pure visual — no side effects, no
 * state; testable as a snapshot.
 */
@Composable
internal fun EmptyFolderState(
    sourceType: SourceType,
    modifier: Modifier = Modifier,
) {
    val (icon, title, subtitle) = when (sourceType) {
        SourceType.DEVICE -> Triple(
            Icons.Outlined.PhotoLibrary,
            stringResource(R.string.folder_empty_title),
            stringResource(R.string.folder_empty_subtitle),
        )
        SourceType.SAF -> Triple(
            Icons.Outlined.FolderOff,
            stringResource(R.string.folder_revoked_title),
            stringResource(R.string.folder_revoked_subtitle),
        )
    }
    EmptyFolderBody(icon = icon, title = title, subtitle = subtitle, modifier = modifier)
}

@Composable
private fun EmptyFolderBody(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 32.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(64.dp),
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
