package com.eight87.shutterboy.data.repo

import android.Manifest
import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.eight87.shutterboy.data.db.ShutterboyDatabase
import com.eight87.shutterboy.data.saf.SafSourceManager
import com.eight87.shutterboy.data.scan.ExifEnricher
import com.eight87.shutterboy.data.scan.MediaStoreGenerationSource
import com.eight87.shutterboy.data.scan.MediaStoreScanner
import com.eight87.shutterboy.data.scan.ScannedPhoto
import com.eight87.shutterboy.data.settings.ScanConfigSource
import com.eight87.shutterboy.data.settings.ScanGatePreferences
import com.eight87.shutterboy.domain.ScanProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Verifies that [RoomGalleryRepository.executeScan] emits intermediate
 * `ScanProgress.Running` updates as it walks each scanned photo, in
 * addition to the initial `Running(0, N)` and the terminal `Done(...)`
 * the gate paths emit. Uses a fake [MediaStoreScanner] that returns a
 * deterministic synthetic library, so the test never touches the
 * Robolectric MediaStore shadow.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [33])
class RoomGalleryRepositoryScanProgressTest {

    private lateinit var db: ShutterboyDatabase

    @Before
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<android.app.Application>()
        db = Room.inMemoryDatabaseBuilder(ctx, ShutterboyDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        // Generation gate persists the MediaStore token only when the read
        // permission is granted; that's irrelevant here, but the scan
        // itself runs either way.
        shadowOf(ctx).grantPermissions(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
        )
    }

    @After
    fun tearDown() { db.close() }

    private fun makeRepo(
        scanner: MediaStoreScanner,
        enricher: ExifEnricher,
    ): RoomGalleryRepository {
        val ctx = ApplicationProvider.getApplicationContext<android.app.Application>()
        val scanConfig = object : ScanConfigSource {
            override val safSourceUris: Flow<Set<String>> = flowOf(emptySet())
        }
        val scanGate = object : ScanGatePreferences {
            private var tok: Long? = null
            override fun observeMediaStoreGeneration(volume: String): Flow<Long?> = flowOf(tok)
            override suspend fun setMediaStoreGeneration(volume: String, token: Long) { tok = token }
            override fun observeSafFingerprint(): Flow<Map<String, Int>> = flowOf(emptyMap())
            override suspend fun setSafFingerprint(map: Map<String, Int>) {}
            override suspend fun clear() { tok = null }
        }
        return RoomGalleryRepository(
            context = ctx,
            photoDao = db.photos(),
            folderDao = db.folders(),
            favoriteDao = db.favorites(),
            searchDao = db.search(),
            mediaStoreScanner = scanner,
            exifEnricher = enricher,
            safSourceManager = SafSourceManager(ctx),
            scanConfig = scanConfig,
            scanGate = scanGate,
            mediaStoreGeneration = object : MediaStoreGenerationSource {
                override fun current(volume: String): Long = 1L
            },
        )
    }

    private fun fakePhoto(id: Long, isVideo: Boolean = false) = ScannedPhoto(
        id = id,
        contentUri = Uri.parse("content://media/external/${if (isVideo) "video" else "images"}/media/$id"),
        displayName = if (isVideo) "video_$id.mp4" else "photo_$id.jpg",
        dateTakenMs = 1_700_000_000_000 + id,
        dateAddedMs = 1_700_000_000_000 + id,
        width = 100, height = 100, sizeBytes = 1024,
        mimeType = if (isVideo) "video/mp4" else "image/jpeg",
        folderId = 1L,
        folderDisplayName = "Test",
        source = ScannedPhoto.ScanSource.DEVICE_MEDIASTORE,
    )

    private class FakeScanner(
        ctx: Context,
        val photos: List<ScannedPhoto>,
    ) : MediaStoreScanner(ctx) {
        override suspend fun scanDeviceMediaStore(): ScanResult = ScanResult(
            photos = photos,
            folders = listOf(
                ScannedFolder(id = 1L, displayName = "Test", source = ScannedPhoto.ScanSource.DEVICE_MEDIASTORE),
            ),
        )
    }

    private class IdentityEnricher(ctx: Context) : ExifEnricher(ctx) {
        override suspend fun enrich(scanned: ScannedPhoto): ScannedPhoto = scanned
        override suspend fun enrichBatch(scanned: List<ScannedPhoto>): List<ScannedPhoto> = scanned
    }

    @Test
    fun `executeScan emits intermediate Running with growing processed count`() = runTest {
        val ctx = ApplicationProvider.getApplicationContext<android.app.Application>()
        val n = 12
        val photos = (1L..n.toLong()).map { fakePhoto(it) }
        val repo = makeRepo(FakeScanner(ctx, photos), IdentityEnricher(ctx))

        // Capture all ScanProgress emissions. The repo's flow is a
        // MutableStateFlow — collect into a list synchronously since
        // we drive the test on UnconfinedTestDispatcher.
        val seen = mutableListOf<ScanProgress>()
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val scope = TestScope(dispatcher)
        val job = repo.scanProgress().onEach { seen += it }.launchIn(scope)

        repo.runScan()

        job.cancel()

        // At minimum we expect: an initial Running(0, n), a final
        // Running(n, n, lastTitle), and a Done. The throttle (200 ms)
        // is shorter than the synthetic loop's runtime in a JVM test
        // (the SystemClock progresses with real time even on Robolectric),
        // but the loop's `processed == total` short-circuit guarantees
        // we always see the terminal Running(n, n) regardless.
        val runnings = seen.filterIsInstance<ScanProgress.Running>()
        assertTrue("expected at least 2 Running emissions, got ${runnings.size}", runnings.size >= 2)
        // runScan() emits a placeholder Running(0, null) before executeScan
        // discovers the total. Find the first emission with a known total.
        val first = runnings.first { it.total != null }
        assertEquals(0, first.processed)
        assertEquals(n, first.total)
        val last = runnings.last()
        assertEquals(n, last.processed)
        assertEquals(n, last.total)
        assertNotNull(last.currentTitle)
        // Final state should be Done after the scan completes.
        assertTrue(
            "expected terminal Done, got ${seen.lastOrNull()}",
            seen.last() is ScanProgress.Done,
        )
    }

    @Test
    fun `executeScan covers both images and videos via the same channel`() = runTest {
        val ctx = ApplicationProvider.getApplicationContext<android.app.Application>()
        val photos = listOf(
            fakePhoto(1L, isVideo = false),
            fakePhoto(2L, isVideo = true),
            fakePhoto(3L, isVideo = false),
            fakePhoto(4L, isVideo = true),
        )
        val repo = makeRepo(FakeScanner(ctx, photos), IdentityEnricher(ctx))
        val snap = repo.runScan()
        assertEquals(4, snap.photos.size)
        // Two video rows should be present in the resulting snapshot.
        assertEquals(2, snap.photos.count { it.mimeType.startsWith("video/") })
    }
}
