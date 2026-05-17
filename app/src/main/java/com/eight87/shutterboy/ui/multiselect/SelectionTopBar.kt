package com.eight87.shutterboy.ui.multiselect

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DriveFileMove
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.eight87.shutterboy.R

/**
 * Phase H.2 + H.4 + m3-expressive F.3 pattern 3 — selection-mode top app bar.
 *
 * Renders the count, a close (X) affordance, a "Select all" affordance, a
 * Move-to-album affordance (H.4), and a Delete affordance. Container colour
 * is `surfaceContainerHigh` (M3E surface-tier ladder rung 4) so the bar sits
 * visibly above the page surface without leaning on shadow elevation.
 *
 * Stable test tags expose each affordance to Robolectric / mobile-mcp.
 */
object SelectionTopBarTags {
    const val ROOT = "multiselect_top_bar_root"
    const val CLOSE = "multiselect_top_bar_close"
    const val SELECT_ALL = "multiselect_top_bar_select_all"
    const val MOVE = "multiselect_top_bar_move"
    const val DELETE = "multiselect_top_bar_delete"
    const val COUNT = "multiselect_top_bar_count"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionTopBar(
    count: Int,
    onClose: () -> Unit,
    onSelectAll: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        modifier = modifier.testTag(SelectionTopBarTags.ROOT),
        title = {
            Text(
                modifier = Modifier.testTag(SelectionTopBarTags.COUNT),
                text = stringResource(R.string.multiselect_selected_count, count),
            )
        },
        navigationIcon = {
            IconButton(
                modifier = Modifier.testTag(SelectionTopBarTags.CLOSE),
                onClick = onClose,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = stringResource(R.string.cd_multiselect_close),
                )
            }
        },
        actions = {
            IconButton(
                modifier = Modifier.testTag(SelectionTopBarTags.SELECT_ALL),
                onClick = onSelectAll,
            ) {
                Icon(
                    imageVector = Icons.Filled.DoneAll,
                    contentDescription = stringResource(R.string.multiselect_select_all),
                )
            }
            IconButton(
                modifier = Modifier.testTag(SelectionTopBarTags.MOVE),
                onClick = onMove,
                enabled = count > 0,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.DriveFileMove,
                    contentDescription = stringResource(R.string.cd_multiselect_move),
                )
            }
            IconButton(
                modifier = Modifier.testTag(SelectionTopBarTags.DELETE),
                onClick = onDelete,
                enabled = count > 0,
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.multiselect_delete),
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    )
}
