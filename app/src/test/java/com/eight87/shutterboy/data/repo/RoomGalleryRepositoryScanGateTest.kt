package com.eight87.shutterboy.data.repo

import android.Manifest
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import org.robolectric.Shadows.shadowOf
import com.eight87.shutterboy.data.db.ShutterboyDatabase
import com.eight87.shutterboy.data.saf.SafSourceManager
import com.eight87.shutterboy.data.scan.ExifEnricher
import com.eight87.shutterboy.data.scan.MediaStoreGenerationSource
import com.eight87.shutterboy.data.scan.MediaStoreScanner
import com.eight87.shutterboy.data.settings.ScanConfigSource
import com.eight87.shutterboy.data.settings.ScanGatePreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Phase incremental-scan C.2 — verify [RoomGalleryRepository.scanIfChanged]'s
 * generation-gate behaviour with a deterministic [MediaStoreGenerationSource]
 * seam: matching token short-circuits the scan; differing token runs the
 * scan and persists the new token; SAF fingerprint mismatch also triggers
 * a scan even when MediaStore matches.
 *
 * The scanner pipeline runs against an in-memory Room + empty MediaStore
 * (Robolectric default), so `deltaCount == 0` is the expected snapshot;
 * what the test asserts is the *gate decision*, observable via the
 * `setMediaStoreGeneration` / `setSafFingerprint` calls recorded on the
 * fake [ScanGatePreferences].
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [33])
class RoomGalleryRepositoryScanGateTest {

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

    private fun makeRepo(
        scanGate: ScanGatePreferences,
        generationSource: MediaStoreGenerationSource,
    ): RoomGalleryRepository {
        val ctx = ApplicationProvider.getApplicationContext<android.app.Application>()
        val scanConfig = object : ScanConfigSource {
            override val safSourceUris: Flow<Set<String>> = flowOf(emptySet())
        }
        return RoomGalleryRepository(
            context = ctx,
            photoDao = db.photos(),
            folderDao = db.folders(),
            favoriteDao = db.favorites(),
            searchDao = db.search(),
            mediaStoreScanner = MediaStoreScanner(ctx),
            exifEnricher = ExifEnricher(ctx),
            safSourceManager = SafSourceManager(ctx),
            scanConfig = scanConfig,
            scanGate = scanGate,
            mediaStoreGeneration = generationSource,
        )
    }

    private class RecordingScanGate(
        initialMediaStoreToken: Long? = null,
        initialSaf: Map<String, Int> = emptyMap(),
    ) : ScanGatePreferences {
        private var mediaStoreToken: Long? = initialMediaStoreToken
        private var saf: Map<String, Int> = initialSaf
        val mediaStoreWrites = mutableListOf<Pair<String, Long>>()
        val safWrites = mutableListOf<Map<String, Int>>()
        var clears = 0

        override fun observeMediaStoreGeneration(volume: String): Flow<Long?> =
            flowOf(mediaStoreToken)
        override suspend fun setMediaStoreGeneration(volume: String, token: Long) {
            mediaStoreToken = token
            mediaStoreWrites += volume to token
        }
        override fun observeSafFingerprint(): Flow<Map<String, Int>> = flowOf(saf)
        override suspend fun setSafFingerprint(map: Map<String, Int>) {
            saf = map
            safWrites += map
        }
        override suspend fun clear() {
            mediaStoreToken = null
            saf = emptyMap()
            clears += 1
        }
    }

    private class FixedGenerationSource(private val token: Long) : MediaStoreGenerationSource {
        override fun current(volume: String): Long = token
    }

    @Test
    fun `matching generation token + matching saf short-circuits the scan`() = runTest {
        val gate = RecordingScanGate(initialMediaStoreToken = 42L, initialSaf = emptyMap())
        val repo = makeRepo(gate, FixedGenerationSource(42L))

        val snap = repo.scanIfChanged()

        assertEquals(0, snap.deltaCount)
        assertTrue(
            "gate should not be re-written when both surfaces match",
            gate.mediaStoreWrites.isEmpty(),
        )
        assertTrue(gate.safWrites.isEmpty())
    }

    private fun grantReadMediaImages() {
        val ctx = ApplicationProvider.getApplicationContext<android.app.Application>()
        // API 33+ (Config.sdk = 33 above) → READ_MEDIA_IMAGES + READ_MEDIA_VIDEO
        // (MediaImagesPermission.isGranted requires both since the scanner
        // also covers MediaStore.Video).
        shadowOf(ctx).grantPermissions(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
        )
    }

    @Test
    fun `differing generation token triggers scan and persists new token`() = runTest {
        grantReadMediaImages()
        val gate = RecordingScanGate(initialMediaStoreToken = 42L, initialSaf = emptyMap())
        val repo = makeRepo(gate, FixedGenerationSource(43L))

        repo.scanIfChanged()

        assertEquals(1, gate.mediaStoreWrites.size)
        assertEquals(43L, gate.mediaStoreWrites.single().second)
        // SAF fingerprint is always persisted on a successful scan run.
        assertEquals(1, gate.safWrites.size)
    }

    @Test
    fun `no persisted token triggers initial scan`() = runTest {
        grantReadMediaImages()
        val gate = RecordingScanGate(initialMediaStoreToken = null, initialSaf = emptyMap())
        val repo = makeRepo(gate, FixedGenerationSource(100L))

        repo.scanIfChanged()

        assertEquals(100L, gate.mediaStoreWrites.single().second)
    }

    @Test
    fun `ALWAYS_RESCAN sentinel forces scan even when persisted token is null`() = runTest {
        val gate = RecordingScanGate(initialMediaStoreToken = null, initialSaf = emptyMap())
        val repo = makeRepo(
            gate,
            FixedGenerationSource(MediaStoreGenerationSource.ALWAYS_RESCAN),
        )

        repo.scanIfChanged()

        // Scan ran (SAF persists unconditionally on success); MediaStore-token
        // persistence depends on permission — Robolectric default is denied,
        // so no MediaStore write here. The point is the gate did NOT
        // short-circuit on the sentinel.
        assertEquals(1, gate.safWrites.size)
        assertTrue(
            "ALWAYS_RESCAN should never be persisted as a real token",
            gate.mediaStoreWrites.none { it.second == MediaStoreGenerationSource.ALWAYS_RESCAN },
        )
    }

    @Test
    fun `permission-denied scan does not poison the MediaStore gate`() = runTest {
        // Robolectric grants no runtime permissions by default →
        // MediaImagesPermission.isGranted == false → MediaStore token is NOT
        // persisted even on a successful scan run. SAF fingerprint is still
        // persisted (per-tree URI grants are permission-independent).
        val gate = RecordingScanGate(initialMediaStoreToken = null, initialSaf = emptyMap())
        val repo = makeRepo(gate, FixedGenerationSource(7L))

        repo.scanIfChanged()

        assertTrue(
            "MediaStore token must not be persisted when read permission is denied",
            gate.mediaStoreWrites.isEmpty(),
        )
        assertEquals(1, gate.safWrites.size)
    }

    @Test
    fun `forceRescan clears the gate then re-runs the scan`() = runTest {
        val gate = RecordingScanGate(initialMediaStoreToken = 42L, initialSaf = emptyMap())
        val repo = makeRepo(gate, FixedGenerationSource(42L))

        repo.forceRescan()

        assertEquals(1, gate.clears)
        // SAF was persisted on the re-run.
        assertEquals(1, gate.safWrites.size)
        assertNull(
            "gate.clear() should null out the in-memory token before the rescan re-seeds it",
            // Permission denied in Robolectric → no re-seed of the MediaStore
            // token; the cleared null state stays null.
            gate.mediaStoreWrites.lastOrNull(),
        )
        assertFalse(gate.mediaStoreWrites.isNotEmpty())
    }
}
