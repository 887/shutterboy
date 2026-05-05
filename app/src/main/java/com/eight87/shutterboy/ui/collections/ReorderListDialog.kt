package com.eight87.shutterboy.ui.collections

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.eight87.shutterboy.R
import com.eight87.shutterboy.ui.common.DragReorderColumn

private const val ROW_HEIGHT_DP = 56

/**
 * Phase E.4 — generic reorder dialog. Used by Collections root for both
 * smart-album reordering + folder reordering. Holds the working copy
 * inside [working]; Apply emits to [onApply], Cancel discards.
 *
 * Drag-handle icon at the row's leading edge is the lift target; the
 * caller-supplied [labelOf] gets the rest of the row (a single line of
 * text). For more elaborate row content, consume [DragReorderColumn]
 * directly.
 */
@Composable
internal fun <T : Any> ReorderListDialog(
    title: String,
    items: List<T>,
    keyOf: (T) -> String,
    labelOf: @Composable (T) -> String,
    onApply: (List<T>) -> Unit,
    onDismiss: () -> Unit,
) {
    var working by remember(items) { mutableStateOf(items) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                DragReorderColumn(
                    items = working,
                    itemKey = keyOf,
                    rowHeightDp = ROW_HEIGHT_DP,
                    onReordered = { working = it },
                ) { item, handleModifier ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DragHandle,
                            contentDescription = stringResource(R.string.cd_drag_handle),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = handleModifier.padding(end = 16.dp),
                        )
                        Text(
                            text = labelOf(item),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onApply(working)
                onDismiss()
            }) {
                Text(text = stringResource(R.string.collections_reorder_apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.collections_reorder_cancel))
            }
        },
    )
}
