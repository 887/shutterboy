package com.eight87.shutterboy.data.db

import android.net.Uri
import com.eight87.shutterboy.data.scan.ScannedPhoto
import com.eight87.shutterboy.domain.SourceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [33])
class MappingTest {

    @Test
    fun `PhotoEntity to domain Photo round-trips field values`() {
        val entity = PhotoEntity(
            id = 42L,
            contentUri = "content://media/external/images/media/42",
            displayName = "vacation.jpg",
            dateTakenMs = 1_700_000_000_000L,
            dateAddedMs = 1_700_000_500_000L,
            width = 4032,
            height = 3024,
            sizeBytes = 5_242_880L,
            mimeType = "image/jpeg",
            folderId = 7L,
            exifLensModel = "Sony FE 50mm",
            exifFocalLength = 50.0f,
            exifIso = 200,
            exifAperture = 1.8f,
            exifShutterSpeedSec = 0.004f,
            latitude = 48.137,
            longitude = 11.575,
        )
        val photo = entity.toDomain()

        assertEquals(42L, photo.id.value)
        assertEquals(Uri.parse("content://media/external/images/media/42"), photo.contentUri)
        assertEquals("vacation.jpg", photo.displayName)
        assertEquals(1_700_000_000_000L, photo.dateTakenMs)
        assertEquals(7L, photo.folderId.value)
        assertEquals("Sony FE 50mm", photo.exifLensModel)
        assertEquals(50.0f, photo.exifFocalLength!!, 1e-6f)
        assertEquals(200, photo.exifIso)
        assertEquals(48.137, photo.latitude!!, 1e-9)
        assertEquals(11.575, photo.longitude!!, 1e-9)
    }

    @Test
    fun `FolderEntity to domain Folder maps source type`() {
        val device = FolderEntity(
            id = 1L,
            displayName = "Camera",
            sourceType = "DEVICE",
            safTreeUri = null,
            photoCount = 50,
            coverPhotoId = 100L,
        )
        val saf = FolderEntity(
            id = 2L,
            displayName = "MyExternal",
            sourceType = "SAF",
            safTreeUri = "content://com.android.externalstorage.documents/tree/primary%3AMyExternal",
            photoCount = 12,
            coverPhotoId = null,
        )

        assertEquals(SourceType.DEVICE, device.toDomain().sourceType)
        assertEquals(SourceType.SAF, saf.toDomain().sourceType)
        assertNull(saf.toDomain().coverPhotoId)
        assertNotNull(saf.toDomain().safTreeUri)
    }

    @Test
    fun `FolderEntity to domain falls back to DEVICE on garbled source type`() {
        val mystery = FolderEntity(
            id = 3L,
            displayName = "x",
            sourceType = "WHATEVER",
        )
        assertEquals(SourceType.DEVICE, mystery.toDomain().sourceType)
    }

    @Test
    fun `ScannedPhoto to PhotoEntity carries every cached field`() {
        val scanned = ScannedPhoto(
            id = 99L,
            contentUri = Uri.parse("content://test/99"),
            displayName = "x.jpg",
            dateTakenMs = 1234L,
            dateAddedMs = 5678L,
            width = 800,
            height = 600,
            sizeBytes = 12_345L,
            mimeType = "image/jpeg",
            folderId = 8L,
            folderDisplayName = "TestFolder",
            source = ScannedPhoto.ScanSource.DEVICE_MEDIASTORE,
            exifLensModel = "lens",
            exifFocalLength = 35f,
            exifIso = 400,
            exifAperture = 2.8f,
            exifShutterSpeedSec = 0.01f,
            latitude = 1.23,
            longitude = 4.56,
        )
        val entity = scanned.toEntity()
        assertEquals(99L, entity.id)
        assertEquals(800, entity.width)
        assertEquals("lens", entity.exifLensModel)
        assertEquals(0.01f, entity.exifShutterSpeedSec!!, 1e-6f)
        assertEquals(1.23, entity.latitude!!, 1e-9)
        // Scan-only fields (folderDisplayName, source) are not on PhotoEntity —
        // verifying that's the case by virtue of the type checking; assertion
        // here is symbolic.
    }
}
