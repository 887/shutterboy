package com.eight87.shutterboy.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.room.Upsert
import androidx.sqlite.db.SupportSQLiteQuery
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoDao {
    /**
     * Sort-aware observation. Repository layer composes a [SupportSQLiteQuery]
     * carrying the right `ORDER BY` fragment from `PhotoSort.sqlOrderBy`.
     * `observedEntities` keeps Flow re-emission tied to row changes on
     * `photos`, just like a regular `@Query`.
     */
    @RawQuery(observedEntities = [PhotoEntity::class])
    fun observeAll(query: SupportSQLiteQuery): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos ORDER BY date_taken_ms DESC")
    fun observeAllByDateTakenDesc(): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE folder_id = :folderId ORDER BY date_taken_ms DESC")
    fun observeInFolder(folderId: Long): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE id IN (:ids)")
    suspend fun byIds(ids: List<Long>): List<PhotoEntity>

    /** Phase F — single-photo reactive read for the fullscreen viewer pager. */
    @Query("SELECT * FROM photos WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<PhotoEntity?>

    @Upsert
    suspend fun upsertAll(photos: List<PhotoEntity>)

    @Query("DELETE FROM photos WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("SELECT id FROM photos")
    suspend fun allIds(): List<Long>

    /**
     * Phase I.3.d — destructive wipe used by `LibraryScanner.resetAndRescan`.
     * Truncates the `photos` table; Room cascades into the FTS shadow
     * (`photo_fts` is `contentEntity = PhotoEntity`) and any FK-bound child
     * tables.
     */
    @Query("DELETE FROM photos")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceWithDelta(toUpsert: List<PhotoEntity>, toDelete: List<Long>) {
        if (toUpsert.isNotEmpty()) upsertAll(toUpsert)
        if (toDelete.isNotEmpty()) deleteByIds(toDelete)
    }
}
