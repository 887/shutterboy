package com.eight87.shutterboy.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoDao {
    @Query("SELECT * FROM photos ORDER BY date_taken_ms DESC")
    fun observeAllByDateTakenDesc(): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE folder_id = :folderId ORDER BY date_taken_ms DESC")
    fun observeInFolder(folderId: Long): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE id IN (:ids)")
    suspend fun byIds(ids: List<Long>): List<PhotoEntity>

    /** Recents smart-album resolution — `dateTakenMs >= now - WINDOW_MS`. */
    @Query("SELECT * FROM photos WHERE date_taken_ms >= :sinceMs ORDER BY date_taken_ms DESC")
    fun observeRecents(sinceMs: Long): Flow<List<PhotoEntity>>

    /** Camera / Screenshots smart-album resolution — JOIN against folders.display_name. */
    @Query(
        """
        SELECT photos.* FROM photos
        INNER JOIN folders ON folders.id = photos.folder_id
        WHERE LOWER(folders.display_name) = LOWER(:bucketName)
        ORDER BY date_taken_ms DESC
        """
    )
    fun observeInBucketByName(bucketName: String): Flow<List<PhotoEntity>>

    @Upsert
    suspend fun upsertAll(photos: List<PhotoEntity>)

    @Query("DELETE FROM photos WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("SELECT id FROM photos")
    suspend fun allIds(): List<Long>

    @Transaction
    suspend fun replaceWithDelta(toUpsert: List<PhotoEntity>, toDelete: List<Long>) {
        if (toUpsert.isNotEmpty()) upsertAll(toUpsert)
        if (toDelete.isNotEmpty()) deleteByIds(toDelete)
    }
}
