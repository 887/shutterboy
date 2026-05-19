package com.eight87.shutterboy.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * No foreign-key on `photos` deliberately. With CASCADE delete a
 * temporary MediaStore hiccup (permission timing at boot, a SAF tree
 * unmounted, a single-photo MediaStore index glitch) used to wipe the
 * user's favorites along with the photo rows. Favorites now stand on
 * their own; the inner-join in `observeFavoritePhotos` filters out any
 * orphans, and when the photo row reappears on the next successful
 * scan its favorite is still there.
 */
@Entity(tableName = "photo_favorites")
data class PhotoFavoriteEntity(
    @PrimaryKey
    @ColumnInfo(name = "photo_id")
    val photoId: Long,
)
