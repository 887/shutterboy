package com.eight87.shutterboy.ui.sort

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.eight87.shutterboy.R
import com.eight87.shutterboy.domain.sort.PhotoSort

/**
 * Phase E.2 — top-bar overflow action that reveals the sort sheet. Hosts
 * the dropdown-menu state (`Sort…` item) + the bottom-sheet visibility
 * state, then delegates the apply callback to the caller's persistence
 * write. Surface-agnostic — Photos / FolderDetail / Collections all use
 * the same composable, differing only in which `setXxxSort` they wire
 * into [onSortChanged].
 *
 * Future overflow items (e.g. `Select all`, `Slideshow…`) drop in as
 * additional [DropdownMenuItem]s here; the structure is OCP-shaped.
 */
@Composable
internal fun SortOverflowAction(
    sort: PhotoSort,
    onSortChanged: (PhotoSort) -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }
    var sheetOpen by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        IconButton(onClick = { menuOpen = true }) {
            Icon(
                imageVector = Icons.Outlined.MoreVert,
                contentDescription = stringResource(R.string.cd_more_options),
            )
        }
        DropdownMenu(
            expanded = menuOpen,
            onDismissRequest = { menuOpen = false },
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.sort_overflow_label)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Sort,
                        contentDescription = stringResource(R.string.cd_sort_overflow),
                    )
                },
                onClick = {
                    menuOpen = false
                    sheetOpen = true
                },
            )
        }
    }

    if (sheetOpen) {
        SortSheet(
            current = sort,
            onApply = { newSort ->
                onSortChanged(newSort)
                sheetOpen = false
            },
            onDismiss = { sheetOpen = false },
        )
    }
}
