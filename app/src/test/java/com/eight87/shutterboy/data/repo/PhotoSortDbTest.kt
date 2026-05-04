package com.eight87.shutterboy.data.repo

import androidx.room.Room
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.test.core.app.ApplicationProvider
import com.eight87.shutterboy.data.db.FolderEntity
import com.eight87.shutterboy.data.db.PhotoEntity
import com.eight87.shutterboy.data.db.ShutterboyDatabase
import com.eight87.shutterboy.domain.sort.Direction
import com.eight87.shutterboy.domain.sort.PhotoSort
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Pins the sort-aware repo path. After R.F-flagged shortcut resolution,
 * `RoomGalleryRepository.observePhotos(sort)` builds a `SimpleSQLiteQuery`
 * with `ORDER BY ${sort.sqlOrderBy}` and routes through `PhotoDao.observeAll`
 * (`@RawQuery` over `PhotoEntity`), so the sort happens DB-side. These tests
 * verify the actual ordering each `PhotoSort` variant produces by hitting
 * Room directly with the same query the repo composes.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [33])
class PhotoSortDbTest {

    private lateinit var db: ShutterboyDatabase

    @Before
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<android.app.Application>()
        db = Room.inMemoryDatabaseBuilder(ctx, ShutterboyDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        db.folders().also { dao ->
            kotlinx.coroutines.runBlocking {
                dao.upsertAll(listOf(FolderEntity(id = 1L, displayName = "F", sourceType = "DEVICE")))
            }
        }
        kotlinx.coroutines.runBlocking {
            db.photos().upsertAll(
                listOf(
                    PhotoEntity(
                        id = 1L, contentUri = "c://1", displayName = "alpha.jpg",
                        dateTakenMs = 3_000L, dateAddedMs = 5_000L,
                        width = 100, height = 100, sizeBytes = 300L,
                        mimeType = "image/jpeg", folderId = 1L,
                    ),
                    PhotoEntity(
                        id = 2L, contentUri = "c://2", displayName = "BETA.JPG",
                        dateTakenMs = 1_000L, dateAddedMs = 7_000L,
                        width = 100, height = 100, sizeBytes = 100L,
                        mimeType = "image/jpeg", folderId = 1L,
                    ),
                    PhotoEntity(
                        id = 3L, contentUri = "c://3", displayName = "Charlie.png",
                        dateTakenMs = 2_000L, dateAddedMs = 6_000L,
                        width = 100, height = 100, sizeBytes = 200L,
                        mimeType = "image/png", folderId = 1L,
                    ),
                ),
            )
        }
    }

    @After
    fun tearDown() = db.close()

    private fun rowsSortedBy(sort: PhotoSort): List<Long> =
        kotlinx.coroutines.runBlocking {
            db.photos()
                .observeAll(SimpleSQLiteQuery("SELECT * FROM photos ORDER BY ${sort.sqlOrderBy}"))
                .first()
                .map(PhotoEntity::id)
        }

    @Test
    fun `ByDateTaken DESC orders newest first DB-side`() {
        // alpha=3000, charlie=2000, beta=1000
        assertEquals(listOf(1L, 3L, 2L), rowsSortedBy(PhotoSort.ByDateTaken(Direction.DESC)))
    }

    @Test
    fun `ByDateTaken ASC orders oldest first DB-side`() {
        assertEquals(listOf(2L, 3L, 1L), rowsSortedBy(PhotoSort.ByDateTaken(Direction.ASC)))
    }

    @Test
    fun `ByDateAdded DESC honours mtime independent of taken`() {
        // beta=7000, charlie=6000, alpha=5000
        assertEquals(listOf(2L, 3L, 1L), rowsSortedBy(PhotoSort.ByDateAdded(Direction.DESC)))
    }

    @Test
    fun `ByName ASC is case-insensitive (BETA before Charlie)`() {
        // alpha < BETA < Charlie in NOCASE collation
        assertEquals(listOf(1L, 2L, 3L), rowsSortedBy(PhotoSort.ByName(Direction.ASC)))
    }

    @Test
    fun `ByName DESC inverts to Charlie BETA alpha`() {
        assertEquals(listOf(3L, 2L, 1L), rowsSortedBy(PhotoSort.ByName(Direction.DESC)))
    }

    @Test
    fun `BySize DESC puts biggest first DB-side`() {
        // alpha=300, charlie=200, beta=100
        assertEquals(listOf(1L, 3L, 2L), rowsSortedBy(PhotoSort.BySize(Direction.DESC)))
    }

    @Test
    fun `BySize ASC inverts to smallest first DB-side`() {
        assertEquals(listOf(2L, 3L, 1L), rowsSortedBy(PhotoSort.BySize(Direction.ASC)))
    }

    @Test
    fun `folder-filtered query honours both filter and sort`() = runTest {
        // Add a second folder + a row that should be excluded.
        db.folders().upsertAll(listOf(FolderEntity(id = 2L, displayName = "Other", sourceType = "DEVICE")))
        db.photos().upsertAll(
            listOf(
                PhotoEntity(
                    id = 99L, contentUri = "c://99", displayName = "zzz.jpg",
                    dateTakenMs = 9_999L, dateAddedMs = 9_999L,
                    width = 100, height = 100, sizeBytes = 9_999L,
                    mimeType = "image/jpeg", folderId = 2L,
                ),
            ),
        )

        val rows = db.photos().observeAll(
            SimpleSQLiteQuery(
                "SELECT * FROM photos WHERE folder_id = ? ORDER BY ${PhotoSort.ByDateTaken(Direction.DESC).sqlOrderBy}",
                arrayOf<Any>(1L),
            ),
        ).first().map(PhotoEntity::id)

        assertEquals(listOf(1L, 3L, 2L), rows)
    }

    @Test
    fun `every PhotoSort variant emits a non-blank ORDER BY fragment`() {
        val variants = listOf(
            PhotoSort.ByDateTaken(Direction.DESC),
            PhotoSort.ByDateTaken(Direction.ASC),
            PhotoSort.ByDateAdded(Direction.DESC),
            PhotoSort.ByDateAdded(Direction.ASC),
            PhotoSort.ByName(Direction.DESC),
            PhotoSort.ByName(Direction.ASC),
            PhotoSort.BySize(Direction.DESC),
            PhotoSort.BySize(Direction.ASC),
        )
        for (v in variants) {
            assert(v.sqlOrderBy.isNotBlank()) { "$v has blank sqlOrderBy" }
            assert(" " in v.sqlOrderBy) { "$v lacks direction keyword" }
        }
    }
}
