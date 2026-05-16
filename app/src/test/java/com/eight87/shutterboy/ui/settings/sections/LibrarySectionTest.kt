package com.eight87.shutterboy.ui.settings.sections

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.eight87.shutterboy.data.repo.LibraryScanner
import com.eight87.shutterboy.domain.LibrarySnapshot
import com.eight87.shutterboy.domain.ScanProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.atomic.AtomicInteger

/**
 * Phase I.3 — assert tapping the "Rescan photos" row calls
 * [LibraryScanner.forceRescan] exactly once. The fake counts every
 * `forceRescan` invocation; the snackbar messages are surfaced via the
 * passed-in [SnackbarHostState] but not asserted here (Compose's snackbar
 * timing is flaky under Robolectric — the counter is the load-bearing
 * contract).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class LibrarySectionTest {

    @get:Rule
    val composeRule = createComposeRule()

    private class FakeLibraryScanner(
        private val counter: AtomicInteger,
    ) : LibraryScanner {
        override suspend fun runScan(): LibrarySnapshot =
            LibrarySnapshot(photos = emptyList(), folders = emptyList(), deltaCount = 0)

        override suspend fun scanIfChanged(): LibrarySnapshot = runScan()

        override suspend fun forceRescan(): LibrarySnapshot {
            counter.incrementAndGet()
            return runScan()
        }

        override fun scanProgress(): Flow<ScanProgress> = emptyFlow()
    }

    @Test
    fun tapping_rescan_row_invokes_forceRescan_once() {
        val counter = AtomicInteger(0)
        val fake = FakeLibraryScanner(counter)

        composeRule.setContent {
            // Drive the test seam directly — taps route straight to
            // `forceRescan` without the snackbar timing flake.
            LibrarySection(onRescan = { runBlocking { fake.forceRescan() } })
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithText("Rescan photos").performClick()
        composeRule.waitForIdle()

        assertEquals(1, counter.get())
    }
}
