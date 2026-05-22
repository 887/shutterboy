@file:Suppress("DEPRECATION")

package com.eight87.shutterboy.ui.collections

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.eight87.shutterboy.data.repo.FavoriteCommands
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
 * Phase G.3 smoke — mount [FavoritesScreenContent] with an empty fake
 * favorites flow and assert the empty-state body renders.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class FavoritesScreenSmokeTest {

    @get:Rule
    val composeRule = createComposeRule()

    private class FakeFavorites : FavoriteCommands {
        override suspend fun toggleFavorite(photoId: PhotoId): Boolean = false
        override suspend fun isFavorite(photoId: PhotoId): Boolean = false
        override fun observeIsFavorite(photoId: PhotoId): Flow<Boolean> = flowOf(false)
        override fun observeFavoritePhotos(): Flow<List<Photo>> = flowOf(emptyList())
        override fun observeFavoriteIds(): Flow<Set<Long>> = flowOf(emptySet())
    }

    @Test
    fun empty_favorites_show_the_empty_state_message() {
        composeRule.setContent {
            FavoritesScreenContent(
                favoriteCommands = FakeFavorites(),
                onBack = {},
                onPhotoTap = { _, _ -> },
            )
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText("No favorites yet").assertIsDisplayed()
        composeRule.onNodeWithText("Favorites").assertIsDisplayed()
    }
}
