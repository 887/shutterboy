package com.eight87.shutterboy.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4

/**
 * FTS4 shadow over [PhotoEntity] for substring search across `display_name`
 * and `exif_lens_model`. Folder-name match is composed at query time via a
 * JOIN against `folders` (Phase G search).
 *
 * Room's `contentEntity = PhotoEntity::class` keeps this table in sync with
 * the photos table automatically — every insert/update/delete on `photos`
 * triggers the corresponding FTS update.
 */
@Fts4(contentEntity = PhotoEntity::class)
@Entity(tableName = "photo_fts")
data class PhotoFts(
    @ColumnInfo(name = "display_name") val displayName: String,
    @ColumnInfo(name = "exif_lens_model") val exifLensModel: String? = null,
)
