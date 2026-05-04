package com.eight87.shutterboy.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey val id: Long,
    @ColumnInfo(name = "display_name") val displayName: String,
    /** "DEVICE" or "SAF" — stored as string for forward-compat with new source kinds. */
    @ColumnInfo(name = "source_type") val sourceType: String,
    @ColumnInfo(name = "saf_tree_uri") val safTreeUri: String? = null,
    @ColumnInfo(name = "photo_count") val photoCount: Int = 0,
    @ColumnInfo(name = "cover_photo_id") val coverPhotoId: Long? = null,
)
