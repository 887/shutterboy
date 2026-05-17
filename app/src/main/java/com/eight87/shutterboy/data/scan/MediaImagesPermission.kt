package com.eight87.shutterboy.data.scan

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Read-state of the system permissions that gate `MediaStore.Images` +
 * `MediaStore.Video` cursor access. The manifest permission set differs
 * across SDK boundaries:
 *
 * - API 33+ (`TIRAMISU`): `READ_MEDIA_IMAGES` + `READ_MEDIA_VIDEO`.
 * - API ≤ 32: `READ_EXTERNAL_STORAGE` covers both.
 *
 * Centralised here so the scanner + the cold-start gate share one
 * answer.
 */
object MediaImagesPermission {

    /** All manifest permission names needed to read the photo + video library. */
    val manifestNames: List<String>
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

    /** Back-compat: the primary images permission, used where a single name is needed. */
    val manifestName: String
        get() = manifestNames.first()

    fun isGranted(context: Context): Boolean = manifestNames.all { name ->
        ContextCompat.checkSelfPermission(context, name) == PackageManager.PERMISSION_GRANTED
    }
}
