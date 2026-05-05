package com.eight87.shutterboy.ui.sort

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.eight87.shutterboy.R
import com.eight87.shutterboy.domain.sort.Direction
import com.eight87.shutterboy.domain.sort.PhotoSort

/**
 * Phase E.2 — sort sheet. `ModalBottomSheet` with a radio list of axes
 * (Date taken / Date added / File name / File size) and an Ascending /
 * Descending segmented toggle. The sheet holds [pendingAxis] +
 * [pendingDirection] state; Apply emits the resolved [PhotoSort] to
 * [onApply] and dismisses; back-press / scrim-tap dismiss without
 * applying.
 *
 * Pure composable — caller owns the show/hide state and the persisted
 * sort that backs `current`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SortSheet(
    current: PhotoSort,
    onApply: (PhotoSort) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState()
    var pendingAxis by remember(current) { mutableStateOf(SortAxis.fromSort(current)) }
    var pendingDirection by remember(current) { mutableStateOf(current.direction) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.sort_sheet_title),
                style = MaterialTheme.typography.titleLarge,
            )

            Column(modifier = Modifier.selectableGroup()) {
                SortAxis.entries.forEach { axis ->
                    val selected = axis == pendingAxis
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selected,
                                role = Role.RadioButton,
                                onClick = { pendingAxis = axis },
                            )
                            .padding(vertical = 6.dp),
                    ) {
                        RadioButton(selected = selected, onClick = null)
                        Text(
                            text = stringResource(axis.labelRes),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 12.dp),
                        )
                    }
                }
            }

            DirectionSegmented(
                direction = pendingDirection,
                onDirection = { pendingDirection = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            )

            Button(
                onClick = { onApply(pendingAxis.applyTo(pendingDirection)) },
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 8.dp, bottom = 16.dp),
            ) {
                Text(text = stringResource(R.string.sort_apply))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DirectionSegmented(
    direction: Direction,
    onDirection: (Direction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val entries = Direction.entries
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        entries.forEachIndexed { index, candidate ->
            SegmentedButton(
                selected = candidate == direction,
                onClick = { onDirection(candidate) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = entries.size),
                label = {
                    Text(
                        text = stringResource(
                            when (candidate) {
                                Direction.ASC -> R.string.sort_direction_asc
                                Direction.DESC -> R.string.sort_direction_desc
                            },
                        ),
                    )
                },
            )
        }
    }
}
