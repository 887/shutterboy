package com.eight87.shutterboy.ui.viewer

import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
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
 * Phase F.1 smoke test — mount the viewer over a 3-photo fake source,
 * assert the first photo's TopAppBar title shows, swipe left, assert
 * the second photo becomes current. Robolectric @SDK 33 to match the
 * rest of the suite (compileSdk 36 outruns the bundled Robolectric jar).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class PhotoViewerScreenSmokeTest {

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
        override fun observePhotos(sort: PhotoSort): Flow<List<Photo>> = flowOf(byId.values.toList())
        override fun observePhotosInFolder(
            folderId: FolderId,
            sort: PhotoSort,
        ): Flow<List<Photo>> = flowOf(emptyList())

        override suspend fun photosByIds(ids: List<Long>): List<Photo> =
            ids.mapNotNull { byId[it] }

        override fun observePhotoById(id: Long): Flow<Photo?> = flowOf(byId[id])
    }

    @Test
    fun pager_renders_first_photo_then_swipe_advances_to_second() {
        val photos = listOf(
            makePhoto(1L, "first.jpg"),
            makePhoto(2L, "second.jpg"),
            makePhoto(3L, "third.jpg"),
        )
        val fake = FakePhotoSource(photos.associateBy { it.id.value })

        composeRule.setContent {
            PhotoViewerContent(
                backingIds = photos.map { it.id.value },
                initialPhotoId = 1L,
                photoSource = fake,
                onBack = {},
            )
        }

        composeRule.waitForIdle()

        // TopAppBar title shows the first photo's name. The chrome
        // auto-hides after 3s and the Compose test clock auto-advances
        // past that during waitForIdle, so the node may be detached
        // visually — assertExists is the right contract for "the
        // pager populated page 1".
        composeRule.onNodeWithText("first.jpg").assertExists()

        // Swipe the pager left → page 2.
        composeRule.onNodeWithTag(VIEWER_PAGER_TAG)
            .performTouchInput { swipeLeft() }

        composeRule.waitForIdle()

        composeRule.onNodeWithText("second.jpg").assertExists()
    }
}
