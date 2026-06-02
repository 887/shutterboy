package com.eight87.shutterboy.data.tagging

import android.graphics.Bitmap
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.eight87.shutterboy.data.db.FolderEntity
import com.eight87.shutterboy.data.db.PhotoEntity
import com.eight87.shutterboy.data.db.ShutterboyDatabase
import com.eight87.shutterboy.data.settings.TaggingPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Smart Tags E.4 — drives [AiTaggingService] over an in-memory Room DB with a
 * fake tagger + fake bitmap source. Covers: tagging untagged photos + marking
 * status, the disabled no-op, the unsupported-device no-op, wipe, re-tag, and
 * caption-flag propagation.
 *
 * Uses `runBlocking` + real dispatchers (NOT `runTest`'s virtual clock): the
 * service drives Room Flows/queries on Room's own executor, which a virtual
 * scheduler can't advance, so we wait on real time with generous timeouts.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AiTaggingServiceTest {

    private lateinit var db: ShutterboyDatabase
    private lateinit var scope: CoroutineScope

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ShutterboyDatabase::class.java,
        ).allowMainThreadQueries().build()
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    }

    @After
    fun tearDown() {
        scope.cancel()
        db.close()
    }

    private suspend fun seedPhotos(count: Int) {
        db.folders().upsertAll(listOf(FolderEntity(id = 1, displayName = "f", sourceType = "DEVICE")))
        db.photos().upsertAll(
            (1..count).map { i ->
                PhotoEntity(
                    id = i.toLong(),
                    contentUri = "content://media/external/images/media/$i",
                    displayName = "p$i.jpg",
                    dateTakenMs = i.toLong(),
                    dateAddedMs = i.toLong(),
                    width = 100, height = 100, sizeBytes = 1000,
                    mimeType = "image/jpeg", folderId = 1,
                )
            },
        )
    }

    private suspend fun await(timeoutMs: Long = 10_000, cond: suspend () -> Boolean) {
        withTimeout(timeoutMs) { while (!cond()) delay(15) }
    }

    private fun service(tagger: ImageTagger, prefs: TaggingPreferences) = AiTaggingService(
        tagger, FakeBitmaps(), db.photos(), db.photoTags(), prefs, scope,
    )

    private class FakeTagger(
        private val availability: TaggerAvailability,
        private val perImage: List<TagCandidate> = listOf(TagCandidate("Dog", 0.9f, TagSource.LABEL)),
    ) : ImageTagger {
        @Volatile var analyzeCalls = 0
        @Volatile var lastWantCaption: Boolean? = null
        override suspend fun availability() = availability
        override suspend fun analyze(bitmap: Bitmap, wantCaption: Boolean): List<TagCandidate> {
            analyzeCalls++
            lastWantCaption = wantCaption
            return perImage
        }
        override fun close() = Unit
    }

    private class FakeBitmaps : TaggingBitmapSource {
        override suspend fun decode(contentUri: String): Bitmap =
            Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
    }

    private class FakePrefs(
        enabled: Boolean,
        captions: Boolean = false,
    ) : TaggingPreferences {
        val enabledFlow = MutableStateFlow(enabled)
        val captionsFlow = MutableStateFlow(captions)
        override fun observeEnabled(): Flow<Boolean> = enabledFlow
        override suspend fun setEnabled(value: Boolean) { enabledFlow.value = value }
        override fun observeCaptionsEnabled(): Flow<Boolean> = captionsFlow
        override suspend fun setCaptionsEnabled(value: Boolean) { captionsFlow.value = value }
    }

    @Test
    fun `tags every untagged photo and marks status`() = runBlocking {
        seedPhotos(3)
        val svc = service(FakeTagger(TaggerAvailability(true, false)), FakePrefs(enabled = true))
        svc.start()
        await { db.photoTags().observeProcessedCount().first() == 3 }

        assertEquals(3, db.photoTags().observeTagCount().first())
        assertTrue(db.photoTags().untaggedPhotoIds(10).isEmpty())
    }

    @Test
    fun `disabled master toggle does nothing`() = runBlocking {
        seedPhotos(3)
        val tagger = FakeTagger(TaggerAvailability(true, false))
        val svc = service(tagger, FakePrefs(enabled = false))
        svc.start()
        await { svc.runState.value == TaggingRunState.Disabled }
        // Give any erroneously-launched pass a moment to (not) run.
        delay(100)
        assertEquals(0, db.photoTags().observeProcessedCount().first())
        assertEquals(0, tagger.analyzeCalls)
    }

    @Test
    fun `unsupported device marks state and tags nothing`() = runBlocking {
        seedPhotos(2)
        val svc = service(FakeTagger(TaggerAvailability.NONE), FakePrefs(enabled = true))
        svc.start()
        await { svc.runState.value == TaggingRunState.Unsupported }
        assertEquals(0, db.photoTags().observeProcessedCount().first())
    }

    @Test
    fun `wipe clears tags and status`() = runBlocking {
        seedPhotos(2)
        val svc = service(FakeTagger(TaggerAvailability(true, false)), FakePrefs(enabled = true))
        svc.start()
        await { db.photoTags().observeProcessedCount().first() == 2 }

        svc.wipe()
        await { db.photoTags().observeProcessedCount().first() == 0 }
        assertEquals(0, db.photoTags().observeTagCount().first())
    }

    @Test
    fun `retagAll re-analyses already-processed photos`() = runBlocking {
        seedPhotos(2)
        val tagger = FakeTagger(TaggerAvailability(true, false))
        val svc = service(tagger, FakePrefs(enabled = true))
        svc.start()
        await { tagger.analyzeCalls == 2 && db.photoTags().observeProcessedCount().first() == 2 }

        svc.retagAll()
        await { tagger.analyzeCalls == 4 }
        assertEquals(2, db.photoTags().observeProcessedCount().first())
    }

    @Test
    fun `caption flag is set when captions enabled and available`() = runBlocking {
        seedPhotos(1)
        val tagger = FakeTagger(TaggerAvailability(true, true))
        val svc = service(tagger, FakePrefs(enabled = true, captions = true))
        svc.start()
        await { tagger.lastWantCaption == true }
        assertEquals(true, tagger.lastWantCaption)
    }

    @Test
    fun `caption flag stays off when captions unavailable`() = runBlocking {
        seedPhotos(1)
        val tagger = FakeTagger(TaggerAvailability(true, false))
        val svc = service(tagger, FakePrefs(enabled = true, captions = true))
        svc.start()
        await { db.photoTags().observeProcessedCount().first() == 1 }
        assertFalse(tagger.lastWantCaption ?: true)
    }
}
