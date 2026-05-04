package com.eight87.shutterboy.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoFavoriteDao {
    @Query(
        """
        SELECT photos.* FROM photos
        INNER JOIN photo_favorites ON photo_favorites.photo_id = photos.id
        ORDER BY photos.date_taken_ms DESC
        """
    )
    fun observeFavoritePhotos(): Flow<List<PhotoEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM photo_favorites WHERE photo_id = :photoId)")
    suspend fun isFavorite(photoId: Long): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun add(entity: PhotoFavoriteEntity)

    @Query("DELETE FROM photo_favorites WHERE photo_id = :photoId")
    suspend fun remove(photoId: Long)
}
