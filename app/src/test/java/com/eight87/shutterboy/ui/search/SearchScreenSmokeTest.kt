package com.eight87.shutterboy.ui.search

import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import com.eight87.shutterboy.data.repo.PhotoSearch
import com.eight87.shutterboy.domain.FolderId
import com.eight87.shutterboy.domain.Photo
import com.eight87.shutterboy.domain.PhotoId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Phase G smoke — mount [SearchScreenContent] over a fake [PhotoSearch] that
 * returns a stub list for any non-empty query. Type a character, wait for
 * recomposition, assert the result thumbnail is rendered.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SearchScreenSmokeTest {

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
    )

    private class FakePhotoSearch(private val results: List<Photo>) : PhotoSearch {
        override fun searchPhotos(query: String): Flow<List<Photo>> =
            if (query.isBlank()) flowOf(emptyList()) else flowOf(results)

        override fun recentSearches(): Flow<List<String>> = flowOf(emptyList())
        override suspend fun recordSearch(query: String) { /* no-op */ }
    }

    @Test
    fun typing_a_query_renders_a_result_thumbnail() {
        val fake = FakePhotoSearch(
            results = listOf(makePhoto(42L, "vacation.jpg")),
        )

        composeRule.setContent {
            SearchScreenContent(
                photoSearch = fake,
                onBack = {},
                onResultTap = { _, _ -> },
            )
        }

        composeRule.waitForIdle()

        // The pill field hint is up front.
        composeRule.onNodeWithText("Search photos").assertIsDisplayed()

        // Type into the field — fake returns the stub list.
        composeRule.onNodeWithTag(SEARCH_FIELD_TAG).performTextInput("vacation")
        composeRule.waitForIdle()

        // Results grid is mounted and the thumbnail for id=42 rendered.
        composeRule.onNodeWithTag(SEARCH_RESULTS_GRID_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag("search_thumb_42").assertIsDisplayed()
    }

    @Test
    fun empty_results_show_the_empty_state_message() {
        val fake = FakePhotoSearch(results = emptyList())

        composeRule.setContent {
            SearchScreenContent(
                photoSearch = fake,
                onBack = {},
                onResultTap = { _, _ -> },
            )
        }

        composeRule.onNodeWithTag(SEARCH_FIELD_TAG).performTextInput("nope")
        composeRule.waitForIdle()

        composeRule.onNodeWithText("No photos match this search.").assertIsDisplayed()
    }
}
