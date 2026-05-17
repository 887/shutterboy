package com.eight87.shutterboy.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders ORDER BY display_name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE id = :id")
    fun observeById(id: Long): Flow<FolderEntity?>

    @Upsert
    suspend fun upsertAll(folders: List<FolderEntity>)

    @Query("DELETE FROM folders WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT id FROM folders")
    suspend fun allIds(): List<Long>

    /** Phase I.3.d — destructive wipe used by `LibraryScanner.resetAndRescan`. */
    @Query("DELETE FROM folders")
    suspend fun deleteAll()
}
