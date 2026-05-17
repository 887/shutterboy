package com.eight87.shutterboy.ui.multiselect

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.eight87.shutterboy.R
import com.eight87.shutterboy.domain.Folder
import com.eight87.shutterboy.domain.FolderId

/**
 * Phase H.4 — folder picker dialog for the bulk move-to-album flow.
 *
 * Shows an [AlertDialog] containing a [LazyColumn] of [Folder] rows; each
 * row renders the folder display name + photo count. Tapping a row fires
 * [onPick] with the chosen folder.
 *
 * Filtering note: the caller passes the already-filtered list. The pure
 * helper [filterMoveTargets] strips the source folder when known so the
 * user can't accidentally "move to here". See `MoveTargetSelectionTest`.
 */
object FolderPickerDialogTags {
    const val ROOT = "multiselect_move_picker_root"
    const val CANCEL = "multiselect_move_picker_cancel"
    const val ROW_PREFIX = "multiselect_move_picker_row_"
}

fun folderPickerRowTag(folderId: FolderId): String =
    "${FolderPickerDialogTags.ROW_PREFIX}${folderId.value}"

@Composable
fun FolderPickerDialog(
    folders: List<Folder>,
    onPick: (Folder) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        modifier = Modifier.testTag(FolderPickerDialogTags.ROOT),
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.multiselect_move_picker_title)) },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(folders, key = { it.id.value }) { folder ->
                    FolderRow(folder = folder, onClick = { onPick(folder) })
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                modifier = Modifier.testTag(FolderPickerDialogTags.CANCEL),
                onClick = onDismiss,
            ) {
                Text(stringResource(R.string.multiselect_move_picker_cancel))
            }
        },
    )
}

@Composable
private fun FolderRow(folder: Folder, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(folderPickerRowTag(folder.id))
            .padding(vertical = 8.dp, horizontal = 4.dp),
    ) {
        Text(
            text = folder.displayName,
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = stringResource(
                R.string.multiselect_move_folder_photo_count,
                folder.photoCount,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Pure helper: drop the source folder from the move-target list so the
 * user can't pick "move to here". Stable order preserved otherwise.
 */
fun filterMoveTargets(
    folders: List<Folder>,
    sourceFolderId: FolderId?,
): List<Folder> =
    if (sourceFolderId == null) folders
    else folders.filter { it.id != sourceFolderId }
