package com.eight87.shutterboy.data.repo

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.eight87.shutterboy.data.db.FolderEntity
import com.eight87.shutterboy.data.db.PhotoEntity
import com.eight87.shutterboy.data.db.PhotoFavoriteEntity
import com.eight87.shutterboy.data.db.ShutterboyDatabase
import com.eight87.shutterboy.domain.PhotoId
import com.eight87.shutterboy.domain.SmartAlbumId
import kotlinx.coroutines.flow.first
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
 * Integration test for B.6 smart-album resolution. In-memory Room DB,
 * fixture data, verify each sealed-dispatch case returns the expected
 * photos.
 *
 * The repo's UI-facing facets (`SmartAlbumSource.observeSmartAlbum`) are
 * tested by going through the DAOs the dispatch routes to. The dispatch
 * itself (`when`-on-`SmartAlbumId`) is verified at compile time by
 * exhaustiveness; this test pins the actual query semantics per case.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [33])
class SmartAlbumResolutionTest {

    private lateinit var db: ShutterboyDatabase

    @Before
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<android.app.Application>()
        db = Room.inMemoryDatabaseBuilder(ctx, ShutterboyDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun makePhoto(
        id: Long,
        folderId: Long,
        displayName: String,
        dateTakenMs: Long,
    ) = PhotoEntity(
        id = id,
        contentUri = "content://test/$id",
        displayName = displayName,
        dateTakenMs = dateTakenMs,
        dateAddedMs = dateTakenMs,
        width = 100,
        height = 100,
        sizeBytes = 1000,
        mimeType = "image/jpeg",
        folderId = folderId,
    )

    private fun makeFolder(id: Long, name: String) = FolderEntity(
        id = id,
        displayName = name,
        sourceType = "DEVICE",
    )

    @Test
    fun `Camera smart-album returns photos in folder named Camera (case-insensitive)`() = runTest {
        db.folders().upsertAll(listOf(makeFolder(1L, "Camera"), makeFolder(2L, "Other")))
        db.photos().upsertAll(listOf(
            makePhoto(10L, folderId = 1L, displayName = "cam1.jpg", dateTakenMs = 1000),
            makePhoto(11L, folderId = 2L, displayName = "other.jpg", dateTakenMs = 2000),
        ))

        val rows = db.photos().observeInBucketByName("camera").first() // lower-case query
        assertEquals(1, rows.size)
        assertEquals(10L, rows[0].id)
    }

    @Test
    fun `Screenshots smart-album resolves to its bucket only`() = runTest {
        db.folders().upsertAll(listOf(makeFolder(3L, "Screenshots"), makeFolder(4L, "Camera")))
        db.photos().upsertAll(listOf(
            makePhoto(20L, folderId = 3L, displayName = "ss1.png", dateTakenMs = 1000),
            makePhoto(21L, folderId = 3L, displayName = "ss2.png", dateTakenMs = 2000),
            makePhoto(22L, folderId = 4L, displayName = "cam.jpg", dateTakenMs = 3000),
        ))

        val rows = db.photos().observeInBucketByName("Screenshots").first()
        assertEquals(2, rows.size)
        assertEquals(setOf(20L, 21L), rows.map(PhotoEntity::id).toSet())
    }

    @Test
    fun `Favorites smart-album joins photo_favorites`() = runTest {
        db.folders().upsertAll(listOf(makeFolder(5L, "Anywhere")))
        db.photos().upsertAll(listOf(
            makePhoto(30L, folderId = 5L, displayName = "fav.jpg", dateTakenMs = 1000),
            makePhoto(31L, folderId = 5L, displayName = "not-fav.jpg", dateTakenMs = 2000),
        ))
        db.favorites().add(PhotoFavoriteEntity(photoId = 30L))

        val rows = db.favorites().observeFavoritePhotos().first()
        assertEquals(1, rows.size)
        assertEquals(30L, rows[0].id)
    }

    @Test
    fun `Recents smart-album returns photos within window`() = runTest {
        val now = System.currentTimeMillis()
        val withinWindow = now - 1L * 24 * 60 * 60 * 1000 // 1 day ago
        val outsideWindow = now - 60L * 24 * 60 * 60 * 1000 // 60 days ago

        db.folders().upsertAll(listOf(makeFolder(6L, "F")))
        db.photos().upsertAll(listOf(
            makePhoto(40L, folderId = 6L, displayName = "recent.jpg", dateTakenMs = withinWindow),
            makePhoto(41L, folderId = 6L, displayName = "old.jpg", dateTakenMs = outsideWindow),
        ))

        val rows = db.photos().observeRecents(now - SmartAlbumId.Recents.WINDOW_MS).first()
        assertEquals(1, rows.size)
        assertEquals(40L, rows[0].id)
    }

    @Test
    fun `Favorites toggle is idempotent on add`() = runTest {
        db.folders().upsertAll(listOf(makeFolder(7L, "F")))
        db.photos().upsertAll(listOf(makePhoto(50L, folderId = 7L, displayName = "p.jpg", dateTakenMs = 1L)))

        db.favorites().add(PhotoFavoriteEntity(photoId = 50L))
        db.favorites().add(PhotoFavoriteEntity(photoId = 50L)) // no-op via OnConflictStrategy.IGNORE

        assertTrue(db.favorites().isFavorite(50L))
    }

    @Test
    fun `Default smart-album order matches design doc`() {
        assertEquals(
            listOf(SmartAlbumId.Camera, SmartAlbumId.Screenshots, SmartAlbumId.Favorites, SmartAlbumId.Recents),
            SmartAlbumId.defaultOrder,
        )
    }

    @Test
    fun `SmartAlbumId fromStorageKey is invertible for every default-order case`() {
        for (id in SmartAlbumId.defaultOrder) {
            assertEquals(id, SmartAlbumId.fromStorageKey(id.storageKey))
        }
    }

    @Test
    fun `replaceWithDelta diff is idempotent on identical re-scan`() = runTest {
        db.folders().upsertAll(listOf(makeFolder(8L, "F")))
        val initial = listOf(
            makePhoto(60L, folderId = 8L, displayName = "a.jpg", dateTakenMs = 1L),
            makePhoto(61L, folderId = 8L, displayName = "b.jpg", dateTakenMs = 2L),
        )
        db.photos().replaceWithDelta(toUpsert = initial, toDelete = emptyList())

        // Second pass with identical data — same id set, no deletes.
        db.photos().replaceWithDelta(toUpsert = initial, toDelete = emptyList())

        val all = db.photos().observeAllByDateTakenDesc().first()
        assertEquals(2, all.size)
    }
}
