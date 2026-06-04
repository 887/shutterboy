package com.eight87.shutterboy.data.repo

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.eight87.shutterboy.data.db.FolderEntity
import com.eight87.shutterboy.data.db.PhotoEntity
import com.eight87.shutterboy.data.db.PhotoTagEntity
import com.eight87.shutterboy.data.db.ShutterboyDatabase
import com.eight87.shutterboy.data.saf.SafSourceManager
import com.eight87.shutterboy.data.scan.ExifEnricher
import com.eight87.shutterboy.data.scan.MediaStoreScanner
import com.eight87.shutterboy.data.settings.ScanConfigSource
import com.eight87.shutterboy.data.settings.ScanGatePreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Smart Tags — verifies [RoomGalleryRepository.searchPhotos] matches photos by
 * their tags (label / caption), merges those hits with the filename FTS hits
 * de-duplicated, and treats a query containing LIKE wildcards literally.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [33])
class TagSearchTest {

    private lateinit var db: ShutterboyDatabase
    private lateinit var repo: RoomGalleryRepository

    @Before
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<android.app.Application>()
        db = Room.inMemoryDatabaseBuilder(ctx, ShutterboyDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val scanConfig = object : ScanConfigSource {
            override val safSourceUris: Flow<Set<String>> = flowOf(emptySet())
        }
        repo = RoomGalleryRepository(
            context = ctx,
            photoDao = db.photos(),
            folderDao = db.folders(),
            favoriteDao = db.favorites(),
            searchDao = db.search(),
            photoTagDao = db.photoTags(),
            mediaStoreScanner = MediaStoreScanner(ctx),
            exifEnricher = ExifEnricher(ctx),
            safSourceManager = SafSourceManager(ctx),
            scanConfig = scanConfig,
            scanGate = NoopScanGate,
        )
        kotlinx.coroutines.runBlocking {
            db.folders().upsertAll(listOf(FolderEntity(id = 1, displayName = "cam", sourceType = "DEVICE")))
        }
    }

    @After
    fun tearDown() = db.close()

    private object NoopScanGate : ScanGatePreferences {
        override fun observeMediaStoreGeneration(volume: String) = flowOf(null as Long?)
        override suspend fun setMediaStoreGeneration(volume: String, token: Long) {}
        override fun observeSafFingerprint() = flowOf(emptyMap<String, Int>())
        override suspend fun setSafFingerprint(map: Map<String, Int>) {}
        override suspend fun clear() {}
    }

    private suspend fun photo(id: Long, name: String) {
        db.photos().upsertAll(
            listOf(
                PhotoEntity(
                    id = id,
                    contentUri = "content://media/$id",
                    displayName = name,
                    dateTakenMs = id, dateAddedMs = id,
                    width = 1, height = 1, sizeBytes = 1,
                    mimeType = "image/jpeg", folderId = 1,
                ),
            ),
        )
    }

    private suspend fun tag(photoId: Long, label: String, source: String = "label") {
        db.photoTags().upsertTags(
            listOf(PhotoTagEntity(photoId = photoId, label = label, confidence = 0.9f, source = source)),
        )
    }

    @Test
    fun `query matches a photo by its label even when the filename does not`() = runTest {
        photo(1, "IMG_001.jpg")
        tag(1, "Dog")

        val ids = repo.searchPhotos("dog").first().map { it.id.value }
        assertTrue("expected photo 1 via its 'Dog' tag, got $ids", 1L in ids)
    }

    @Test
    fun `query matches inside a caption sentence`() = runTest {
        photo(2, "PXL.jpg")
        tag(2, "a brown dog running on a beach", source = "caption")

        val ids = repo.searchPhotos("beach").first().map { it.id.value }
        assertTrue(2L in ids)
    }

    @Test
    fun `filename and tag hits are merged and de-duplicated`() = runTest {
        photo(1, "dog_at_park.jpg") // filename hit
        tag(1, "Dog") // also a tag hit — must appear once
        photo(2, "IMG_002.jpg")
        tag(2, "Dog") // tag-only hit

        val ids = repo.searchPhotos("dog").first().map { it.id.value }
        assertEquals("both photos, photo 1 not duplicated", listOf(1L, 2L), ids.sorted())
        assertEquals("no duplicates", ids.size, ids.toSet().size)
    }

    @Test
    fun `wildcard characters in the query are treated literally`() = runTest {
        photo(1, "IMG_001.jpg")
        tag(1, "Dog")

        // "%" must NOT behave as match-everything; nothing is labelled "%".
        val ids = repo.searchPhotos("%").first().map { it.id.value }
        assertTrue("'%' should match nothing, got $ids", ids.isEmpty())
    }
}
