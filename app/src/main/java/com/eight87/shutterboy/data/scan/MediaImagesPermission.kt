package com.eight87.shutterboy.data.scan

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Read-state of the system permission that gates `MediaStore.Images`
 * cursor access. The manifest permission name differs across SDK
 * boundaries:
 *
 * - API 33+ (`TIRAMISU`): `READ_MEDIA_IMAGES`.
 * - API ≤ 32: `READ_EXTERNAL_STORAGE`.
 *
 * Centralised here so the scanner + the cold-start gate share one
 * answer.
 */
object MediaImagesPermission {

    val manifestName: String
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

    fun isGranted(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, manifestName) ==
            PackageManager.PERMISSION_GRANTED
}
