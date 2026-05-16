package com.eight87.shutterboy.ui.multiselect

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.eight87.shutterboy.R

/**
 * Phase H.3 — typed-confirm bulk delete dialog. Pocket-tap-proof: the Confirm button is
 * disabled until the user types the integer [count] exactly. Mirrors tonearmboy d9d9bc6.
 *
 * Test tags are stable so Robolectric tests can assert state transitions without UI-text
 * coupling.
 */
object TypedConfirmDeleteDialogTags {
    const val ROOT = "multiselect_delete_dialog_root"
    const val INPUT = "multiselect_delete_dialog_input"
    const val CONFIRM = "multiselect_delete_dialog_confirm"
    const val CANCEL = "multiselect_delete_dialog_cancel"
}

@Composable
fun TypedConfirmDeleteDialog(
    count: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    var typed by remember { mutableStateOf("") }
    val expected = count.toString()
    val matches = typed == expected

    AlertDialog(
        modifier = Modifier.testTag(TypedConfirmDeleteDialogTags.ROOT),
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.multiselect_delete_dialog_title, count))
        },
        text = {
            Column {
                Text(stringResource(R.string.multiselect_delete_dialog_body))
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    modifier = Modifier.testTag(TypedConfirmDeleteDialogTags.INPUT),
                    value = typed,
                    onValueChange = { input ->
                        // Numeric-only — strip anything else so paste/IME oddities can't
                        // wedge the field.
                        typed = input.filter(Char::isDigit)
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = { Text(expected) },
                )
            }
        },
        confirmButton = {
            TextButton(
                modifier = Modifier.testTag(TypedConfirmDeleteDialogTags.CONFIRM),
                enabled = matches,
                onClick = onConfirm,
            ) {
                Text(stringResource(R.string.multiselect_delete_confirm_label))
            }
        },
        dismissButton = {
            TextButton(
                modifier = Modifier.testTag(TypedConfirmDeleteDialogTags.CANCEL),
                onClick = onDismiss,
            ) {
                Text(stringResource(R.string.multiselect_delete_cancel_label))
            }
        },
    )
}
