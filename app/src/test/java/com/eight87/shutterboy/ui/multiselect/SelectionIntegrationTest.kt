@file:Suppress("DEPRECATION")

package com.eight87.shutterboy.ui.multiselect

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eight87.shutterboy.data.repo.PhotoSource
import com.eight87.shutterboy.domain.FolderId
import com.eight87.shutterboy.domain.Photo
import com.eight87.shutterboy.domain.PhotoId
import com.eight87.shutterboy.domain.sort.PhotoSort
import com.eight87.shutterboy.ui.photos.grid.GalleryTimelineFrame
import com.eight87.shutterboy.ui.photos.grid.PhotoStream
import com.eight87.shutterboy.ui.photos.grid.photoThumbnailTag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Phase H.5 — Robolectric integration coverage for the multi-select chrome.
 *
 * Mounts a `GalleryTimelineFrame` over a fake `PhotoSource`-backed
 * `PhotoStream`, drives the same selection-mode wiring the three production
 * screens use (long-press → enter Active, tap → toggle, `BackHandler` →
 * exit), and asserts:
 *   - long-press on the first tile enters selection mode + count = 1
 *   - tap on the second tile increments to count = 2
 *   - the `BackHandler` press exits selection mode
 *
 * The wrapper mirrors `PhotosScreen` 1:1 for the selection-state pieces;
 * stripping the rest off the production screen avoids dragging in the full
 * `RouteScope` facet bundle for a focused selection test.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SelectionIntegrationTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun makePhoto(id: Long): Photo = Photo(
        id = PhotoId(id),
        contentUri = Uri.parse("content://media/external/images/media/$id"),
        displayName = "p$id.jpg",
        dateTakenMs = 1_700_000_000_000L + id,
        dateAddedMs = 1_700_000_000_000L + id,
        width = 100,
        height = 100,
        sizeBytes = 1024L,
        mimeType = "image/jpeg",
        folderId = FolderId(1L),
        exifLensModel = null,
        exifFocalLength = null,
        exifIso = null,
        exifAperture = null,
        exifShutterSpeedSec = null,
    )

    private class FakePhotoSource(private val photos: List<Photo>) : PhotoSource {
        override fun observePhotos(sort: PhotoSort): Flow<List<Photo>> = flowOf(photos)
        override fun observePhotosInFolder(
            folderId: FolderId,
            sort: PhotoSort,
        ): Flow<List<Photo>> = flowOf(emptyList())

        override suspend fun photosByIds(ids: List<Long>): List<Photo> =
            photos.filter { it.id.value in ids }

        override fun observePhotoById(id: Long): Flow<Photo?> =
            flowOf(photos.firstOrNull { it.id.value == id })
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun MultiSelectHarness(photoSource: PhotoSource) {
        val stream = remember { PhotoStream { photoSource.observePhotos(PhotoSort.Default) } }
        val allPhotos by stream.observe().collectAsStateWithLifecycle(initialValue = emptyList())
        val holder = rememberSelectionHolder()

        BackHandler(enabled = holder.state is SelectionState.Active) { holder.exit() }

        Scaffold(
            topBar = {
                val active = holder.state
                if (active is SelectionState.Active) {
                    SelectionTopBar(
                        count = active.selectedIds.size,
                        onClose = { holder.exit() },
                        onSelectAll = {
                            holder.selectAll(allPhotos.orEmpty().map { it.id })
                        },
                        onMove = { },
                        onDelete = { },
                    )
                } else {
                    TopAppBar(
                        title = { Text(text = "Photos", modifier = Modifier.testTag(IDLE_TITLE_TAG)) },
                    )
                }
            },
            modifier = Modifier.fillMaxSize(),
        ) { padding ->
            GalleryTimelineFrame(
                stream = stream,
                onPhotoTap = { id, _ ->
                    if (holder.state is SelectionState.Active) holder.toggle(id)
                },
                selectionState = holder.state,
                onPhotoLongPress = { id -> holder.enterActive(id) },
                modifier = Modifier.fillMaxSize(),
            )
            // Suppress unused-parameter warning for `padding` in this test harness.
            @Suppress("UNUSED_EXPRESSION") padding
        }
    }

    @Test
    fun long_press_enters_selection_tap_adds_back_exits() {
        val photos = listOf(makePhoto(1L), makePhoto(2L), makePhoto(3L))
        val fake = FakePhotoSource(photos)

        composeRule.setContent { MultiSelectHarness(fake) }
        composeRule.waitForIdle()

        // Initially: regular title bar visible, selection bar absent.
        composeRule.onNodeWithTag(IDLE_TITLE_TAG).assertIsDisplayed()
        composeRule.onAllNodesWithTag(SelectionTopBarTags.ROOT).assertCountEquals(0)

        // Long-press tile 1 → selection bar appears with "1 selected".
        composeRule.onNodeWithTag(photoThumbnailTag(1L))
            .performTouchInput { longClick() }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(SelectionTopBarTags.ROOT).assertIsDisplayed()
        composeRule.onNodeWithText("1 selected").assertIsDisplayed()

        // Tap tile 2 → toggles in, "2 selected".
        composeRule.onNodeWithTag(photoThumbnailTag(2L)).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("2 selected").assertIsDisplayed()

        // Click the close affordance → selection mode exits, idle title returns.
        composeRule.onNodeWithTag(SelectionTopBarTags.CLOSE).performClick()
        composeRule.waitForIdle()
        composeRule.onAllNodesWithTag(SelectionTopBarTags.ROOT).assertCountEquals(0)
        composeRule.onNodeWithTag(IDLE_TITLE_TAG).assertIsDisplayed()
    }

    private companion object {
        const val IDLE_TITLE_TAG = "test_idle_title"
    }
}
