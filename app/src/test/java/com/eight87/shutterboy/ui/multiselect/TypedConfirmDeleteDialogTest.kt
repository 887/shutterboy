package com.eight87.shutterboy.ui.multiselect

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Phase H.3 — Robolectric coverage for the typed-confirm bulk-delete dialog.
 *
 * Asserts:
 *  - Confirm is disabled by default (empty field).
 *  - Typing the count string enables Confirm.
 *  - Typing a non-matching count disables Confirm again.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class TypedConfirmDeleteDialogTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun confirm_button_only_enabled_when_typed_count_matches() {
        var confirmed = false

        composeRule.setContent {
            TypedConfirmDeleteDialog(
                count = 15,
                onConfirm = { confirmed = true },
                onDismiss = {},
            )
        }

        // Default state: Confirm disabled.
        composeRule.onNodeWithTag(TypedConfirmDeleteDialogTags.CONFIRM).assertIsNotEnabled()

        // Type the matching count → Confirm enabled.
        composeRule.onNodeWithTag(TypedConfirmDeleteDialogTags.INPUT).performTextInput("15")
        composeRule.onNodeWithTag(TypedConfirmDeleteDialogTags.CONFIRM).assertIsEnabled()

        // Replace with a non-matching count → Confirm disabled again.
        composeRule.onNodeWithTag(TypedConfirmDeleteDialogTags.INPUT).performTextClearance()
        composeRule.onNodeWithTag(TypedConfirmDeleteDialogTags.INPUT).performTextInput("14")
        composeRule.onNodeWithTag(TypedConfirmDeleteDialogTags.CONFIRM).assertIsNotEnabled()

        // Sanity: onConfirm was never invoked (we never tapped Confirm).
        assert(!confirmed)
    }
}
