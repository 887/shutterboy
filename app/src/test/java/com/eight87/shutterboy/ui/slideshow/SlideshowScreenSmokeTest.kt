@file:Suppress("DEPRECATION")

package com.eight87.shutterboy.ui.slideshow

import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.eight87.shutterboy.data.repo.PhotoSource
import com.eight87.shutterboy.domain.FolderId
import com.eight87.shutterboy.domain.Photo
import com.eight87.shutterboy.domain.PhotoId
import com.eight87.shutterboy.domain.sort.PhotoSort
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Phase J.1 smoke test — mount the slideshow over a 3-photo fake source,
 * assert the first photo's contentUri is loaded (its page tag appears),
 * tap → assert the "Paused" badge becomes visible (playing toggled off).
 * Robolectric @SDK 33 to match the rest of the suite.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SlideshowScreenSmokeTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun makePhoto(id: Long, name: String): Photo = Photo(
        id = PhotoId(id),
        contentUri = Uri.parse("content://media/external/images/media/$id"),
        displayName = name,
        dateTakenMs = 1_700_000_000_000L + id,
        dateAddedMs = 1_700_000_000_000L + id,
        width = 4032,
        height = 3024,
        sizeBytes = 1024L * 1024L * 4L,
        mimeType = "image/jpeg",
        folderId = FolderId(1L),
        exifLensModel = "Test Lens",
        exifFocalLength = 35f,
        exifIso = 200,
        exifAperture = 2.8f,
        exifShutterSpeedSec = 1f / 125f,
    )

    private class FakePhotoSource(private val byId: Map<Long, Photo>) : PhotoSource {
        override fun observePhotos(sort: PhotoSort): Flow<List<Photo>> =
            flowOf(byId.values.toList())

        override fun observePhotosInFolder(
            folderId: FolderId,
            sort: PhotoSort,
        ): Flow<List<Photo>> = flowOf(emptyList())

        override suspend fun photosByIds(ids: List<Long>): List<Photo> =
            ids.mapNotNull { byId[it] }

        override fun observePhotoById(id: Long): Flow<Photo?> = flowOf(byId[id])
    }

    @Test
    fun pager_renders_first_page_then_tap_pauses() {
        val photos = listOf(
            makePhoto(1L, "first.jpg"),
            makePhoto(2L, "second.jpg"),
            makePhoto(3L, "third.jpg"),
        )
        val fake = FakePhotoSource(photos.associateBy { it.id.value })

        composeRule.setContent {
            SlideshowContent(
                backingIds = photos.map { it.id.value },
                photoSource = fake,
                onBack = {},
                // Very long dwell so the timer never fires inside the test —
                // we only want to assert the initial render + tap-toggle.
                dwellMs = 60_000L,
            )
        }

        composeRule.waitForIdle()

        // First photo's page is mounted (contentUri resolved via fake).
        composeRule.onNodeWithTag("${SLIDESHOW_PAGE_TAG_PREFIX}1").assertIsDisplayed()

        // Initially playing → no paused badge.
        composeRule.onNodeWithTag(SLIDESHOW_PAUSED_BADGE_TAG).assertDoesNotExist()

        // Tap the pager → playing flips to false → paused badge appears.
        composeRule.onNodeWithTag(SLIDESHOW_PAGER_TAG).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(SLIDESHOW_PAUSED_BADGE_TAG).assertIsDisplayed()
    }
}
