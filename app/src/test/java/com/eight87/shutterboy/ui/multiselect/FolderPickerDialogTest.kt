package com.eight87.shutterboy.ui.multiselect

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.eight87.shutterboy.domain.Folder
import com.eight87.shutterboy.domain.FolderId
import com.eight87.shutterboy.domain.SourceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Phase H.4 — Robolectric coverage for the move-to-album folder picker.
 *
 * Mounts the dialog with a fake list of 3 folders, asserts each row is
 * rendered, and asserts that tapping a row fires `onPick` with the
 * matching folder.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class FolderPickerDialogTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val folders = listOf(
        Folder(id = FolderId(1), displayName = "Camera", sourceType = SourceType.DEVICE, photoCount = 42),
        Folder(id = FolderId(2), displayName = "Screenshots", sourceType = SourceType.DEVICE, photoCount = 7),
        Folder(id = FolderId(3), displayName = "Whatsapp", sourceType = SourceType.DEVICE, photoCount = 99),
    )

    @Test
    fun renders_every_folder_and_fires_onPick() {
        var picked: Folder? = null
        composeRule.setContent {
            FolderPickerDialog(
                folders = folders,
                onPick = { picked = it },
                onDismiss = {},
            )
        }

        // All three folder names render.
        composeRule.onNodeWithText("Camera").assertIsDisplayed()
        composeRule.onNodeWithText("Screenshots").assertIsDisplayed()
        composeRule.onNodeWithText("Whatsapp").assertIsDisplayed()

        // Tap the Screenshots row → onPick fires with the matching folder.
        composeRule.onNodeWithTag(folderPickerRowTag(FolderId(2))).performClick()
        composeRule.waitForIdle()
        assertNotNull(picked)
        assertEquals(FolderId(2), picked!!.id)
    }
}
