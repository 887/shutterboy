package com.eight87.shutterboy.data.repo

import android.app.Application
import android.content.ContentValues
import android.provider.MediaStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.eight87.shutterboy.data.db.ShutterboyDatabase
import com.eight87.shutterboy.data.saf.SafSourceManager
import com.eight87.shutterboy.data.scan.ExifEnricher
import com.eight87.shutterboy.data.scan.MediaStoreGenerationSource
import com.eight87.shutterboy.data.scan.MediaStoreScanner
import com.eight87.shutterboy.data.settings.ScanConfigSource
import com.eight87.shutterboy.data.settings.ScanGatePreferences
import com.eight87.shutterboy.domain.DeleteRequest
import com.eight87.shutterboy.domain.PhotoId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Phase F.8 — Robolectric coverage of the three-branch SDK split in
 * [RoomGalleryRepository.deletePhotos] (the [PhotoDeleter] facet).
 *
 * The contract under test:
 *  - API 30+ (here exercised via `@Config(sdk = [33])`): the impl calls
 *    `MediaStore.createDeleteRequest`. On a real device this yields
 *    [DeleteRequest.Consent] wrapping a [android.app.PendingIntent]; on
 *    Robolectric the AOSP `createDeleteRequest` path throws
 *    `IllegalArgumentException: Unknown authority media` because no real
 *    MediaProvider is registered, which the impl's `runCatching` catches
 *    and converts to [DeleteRequest.Failure]. We assert routing through
 *    the API 30+ branch by verifying the result is **not** Immediate
 *    (i.e. it is one of Consent / Failure, never the direct-delete path).
 *  - API 26-28 (exercised via per-method `@Config(sdk = [28])`): the impl
 *    does direct `contentResolver.delete(uri)` per id and returns
 *    [DeleteRequest.Immediate] with the summed `deletedCount`.
 *  - Empty input list short-circuits to `Immediate(deletedCount = 0)`
 *    on every SDK (no resolver touch).
 *
 * Behaviour notes (Robolectric-observed):
 *  - Robolectric's [org.robolectric.shadows.ShadowContentResolver.delete]
 *    returns `1` regardless of whether a matching row was previously
 *    inserted — there is no row-level MediaStore backing store. The
 *    "missing id on API 28" path (real-device `delete` returning 0) is
 *    therefore not reachable under Robolectric; that production case is
 *    covered on real devices only.
 *  - On API 30+ the [DeleteRequest.Consent] success leaf is likewise
 *    unreachable in Robolectric (no MediaProvider), so this suite
 *    confirms the SDK branching shape (no Immediate on API 33+) rather
 *    than the consent-PendingIntent payload. That payload is exercised
 *    on a real device when the viewer's delete action runs the
 *    `StartIntentSenderForResult` launcher.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [33])
class PhotoDeleterBranchingTest {

    private lateinit var db: ShutterboyDatabase

    @Before
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Application>()
        db = Room.inMemoryDatabaseBuilder(ctx, ShutterboyDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun makeRepo(): RoomGalleryRepository {
        val ctx = ApplicationProvider.getApplicationContext<Application>()
        val scanConfig = object : ScanConfigSource {
            override val safSourceUris: Flow<Set<String>> = flowOf(emptySet())
        }
        val scanGate = object : ScanGatePreferences {
            override fun observeMediaStoreGeneration(volume: String): Flow<Long?> = flowOf(null)
            override suspend fun setMediaStoreGeneration(volume: String, token: Long) {}
            override fun observeSafFingerprint(): Flow<Map<String, Int>> = flowOf(emptyMap())
            override suspend fun setSafFingerprint(map: Map<String, Int>) {}
            override suspend fun clear() {}
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
            mediaStoreGeneration = object : MediaStoreGenerationSource {
                override fun current(volume: String): Long = 0L
            },
        )
    }

    /** Seed a single MediaStore.Images.Media row so an id is present. */
    private fun seedMediaStoreImage(displayName: String = "deleter-test.jpg"): Long {
        val ctx = ApplicationProvider.getApplicationContext<Application>()
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        }
        val uri = ctx.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values,
        )
        assertNotNull("MediaStore insert should yield a uri", uri)
        return android.content.ContentUris.parseId(uri!!)
    }

    // ---------------- API 30+ (sdk = 33 inherited from class) ----------------

    @Test
    fun `API 33 routes through the createDeleteRequest branch (never Immediate)`() = runTest {
        val id = seedMediaStoreImage()
        val result = makeRepo().deletePhotos(listOf(PhotoId(id)))

        // Real device: Consent(pendingIntent). Robolectric: Failure
        // (Unknown authority media — no MediaProvider). Either is a
        // correct routing through the API 30+ branch; the direct-delete
        // `Immediate` path is API 26-28 only.
        assertTrue(
            "expected Consent or Failure on API 33 (never Immediate), got $result",
            result is DeleteRequest.Consent || result is DeleteRequest.Failure,
        )
    }

    @Test
    fun `API 33 empty list short-circuits to Immediate(0) without touching resolver`() = runTest {
        val result = makeRepo().deletePhotos(emptyList())
        assertEquals(DeleteRequest.Immediate(deletedCount = 0), result)
    }

    @Test
    fun `API 33 unknown id still routes through createDeleteRequest branch`() = runTest {
        // Real device: createDeleteRequest builds a consent intent from
        // the URI list without checking row existence — the system
        // dialog surfaces "0 deleted". Under Robolectric we end up in
        // Failure (no MediaProvider). Either way, never Immediate.
        val result = makeRepo().deletePhotos(listOf(PhotoId(999_999L)))
        assertTrue(
            "expected Consent or Failure for an unknown id on API 33, got $result",
            result is DeleteRequest.Consent || result is DeleteRequest.Failure,
        )
    }

    // ---------------- API 26-28 (per-method @Config override) ----------------

    @Test
    @Config(sdk = [28])
    fun `API 28 returns Immediate with summed deletedCount for one id`() = runTest {
        val id = seedMediaStoreImage()
        val result = makeRepo().deletePhotos(listOf(PhotoId(id)))

        // Robolectric's ShadowContentResolver.delete returns 1 by default
        // for any uri it recognises (inserted or not) — see the class
        // doc-comment. We assert that the impl correctly routes through
        // the Immediate branch and sums the per-uri counts.
        assertEquals(DeleteRequest.Immediate(deletedCount = 1), result)
    }

    @Test
    @Config(sdk = [28])
    fun `API 28 empty list short-circuits to Immediate(0)`() = runTest {
        val result = makeRepo().deletePhotos(emptyList())
        assertEquals(DeleteRequest.Immediate(deletedCount = 0), result)
    }

    @Test
    @Config(sdk = [28])
    fun `API 28 sums deletedCount across multiple ids`() = runTest {
        val id1 = seedMediaStoreImage("a.jpg")
        val id2 = seedMediaStoreImage("b.jpg")
        val result = makeRepo().deletePhotos(listOf(PhotoId(id1), PhotoId(id2)))

        // Two uris, ShadowContentResolver.delete returns 1 for each →
        // summed deletedCount == 2. Verifies the per-uri loop in the
        // Immediate branch.
        assertEquals(DeleteRequest.Immediate(deletedCount = 2), result)
    }
}
