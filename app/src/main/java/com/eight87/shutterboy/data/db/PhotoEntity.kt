package com.eight87.shutterboy.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "photos",
    foreignKeys = [
        ForeignKey(
            entity = FolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folder_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["folder_id"]),
        Index(value = ["date_taken_ms"]),
        Index(value = ["date_added_ms"]),
    ],
)
data class PhotoEntity(
    @PrimaryKey val id: Long,
    @ColumnInfo(name = "content_uri") val contentUri: String,
    @ColumnInfo(name = "display_name") val displayName: String,
    @ColumnInfo(name = "date_taken_ms") val dateTakenMs: Long,
    @ColumnInfo(name = "date_added_ms") val dateAddedMs: Long,
    val width: Int,
    val height: Int,
    @ColumnInfo(name = "size_bytes") val sizeBytes: Long,
    @ColumnInfo(name = "mime_type") val mimeType: String,
    @ColumnInfo(name = "folder_id") val folderId: Long,
    @ColumnInfo(name = "exif_lens_model") val exifLensModel: String? = null,
    @ColumnInfo(name = "exif_focal_length") val exifFocalLength: Float? = null,
    @ColumnInfo(name = "exif_iso") val exifIso: Int? = null,
    @ColumnInfo(name = "exif_aperture") val exifAperture: Float? = null,
    @ColumnInfo(name = "exif_shutter_speed_sec") val exifShutterSpeedSec: Float? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
)
