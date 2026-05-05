package com.eight87.shutterboy.data.repo

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.eight87.shutterboy.data.db.FolderEntity
import com.eight87.shutterboy.data.db.PhotoEntity
import com.eight87.shutterboy.data.db.ShutterboyDatabase
import com.eight87.shutterboy.data.saf.SafSourceManager
import com.eight87.shutterboy.data.scan.ExifEnricher
import com.eight87.shutterboy.data.scan.MediaStoreScanner
import com.eight87.shutterboy.data.settings.ScanConfigSource
import com.eight87.shutterboy.domain.FolderId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Phase D.5 — verify [RoomGalleryRepository.observeFolderCovers] correctly
 * resolves each folder's `coverPhotoId` to its `Photo` via a single batch
 * read per emission. In-memory Room, real repo, exercise the Flow.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [33])
class FolderCoversTest {

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
            mediaStoreScanner = MediaStoreScanner(ctx),
            exifEnricher = ExifEnricher(ctx),
            safSourceManager = SafSourceManager(ctx),
            scanConfig = scanConfig,
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun makePhoto(id: Long, folderId: Long): PhotoEntity = PhotoEntity(
        id = id,
        contentUri = "content://test/$id",
        displayName = "p$id.jpg",
        dateTakenMs = 1000L * id,
        dateAddedMs = 1000L * id,
        width = 100,
        height = 100,
        sizeBytes = 1000,
        mimeType = "image/jpeg",
        folderId = folderId,
    )

    private fun makeFolder(id: Long, name: String, coverPhotoId: Long?): FolderEntity =
        FolderEntity(
            id = id,
            displayName = name,
            sourceType = "DEVICE",
            coverPhotoId = coverPhotoId,
        )

    @Test
    fun `observeFolderCovers maps each folder to its cover photo`() = runTest {
        db.folders().upsertAll(listOf(
            makeFolder(1L, "Camera", coverPhotoId = 10L),
            makeFolder(2L, "Screenshots", coverPhotoId = 20L),
        ))
        db.photos().upsertAll(listOf(
            makePhoto(10L, folderId = 1L),
            makePhoto(20L, folderId = 2L),
        ))

        val covers = repo.observeFolderCovers().first()
        assertEquals(2, covers.size)
        assertEquals(10L, covers[FolderId(1L)]?.id?.value)
        assertEquals(20L, covers[FolderId(2L)]?.id?.value)
    }

    @Test
    fun `observeFolderCovers omits folders without a cover photo id`() = runTest {
        db.folders().upsertAll(listOf(
            makeFolder(1L, "WithCover", coverPhotoId = 10L),
            makeFolder(2L, "NoCover", coverPhotoId = null),
        ))
        db.photos().upsertAll(listOf(makePhoto(10L, folderId = 1L)))

        val covers = repo.observeFolderCovers().first()
        assertEquals(1, covers.size)
        assertTrue(covers.containsKey(FolderId(1L)))
        assertNull(covers[FolderId(2L)])
    }

    @Test
    fun `observeFolderCovers omits folders whose cover row was deleted`() = runTest {
        db.folders().upsertAll(listOf(
            makeFolder(1L, "Stale", coverPhotoId = 10L),
        ))
        // No corresponding photo row for id=10 — the cover reference dangles.

        val covers = repo.observeFolderCovers().first()
        assertTrue(covers.isEmpty())
    }

    @Test
    fun `observeFolderCovers on empty folder set returns empty map`() = runTest {
        val covers = repo.observeFolderCovers().first()
        assertTrue(covers.isEmpty())
    }
}
