package com.eight87.shutterboy.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * Smart Tags DAO — writes/reads the `photo_tags` + `photo_tag_status`
 * tables. Kept narrow per ISP: the background tagging pass writes,
 * the Settings screen observes counts/size, and wipe/reset truncate.
 */
@Dao
interface PhotoTagDao {

    @Upsert
    suspend fun upsertTags(tags: List<PhotoTagEntity>)

    @Upsert
    suspend fun markStatus(status: PhotoTagStatusEntity)

    /**
     * Photo ids that have never been through the tagger (no status row).
     * Bounded by [limit] so the background pass processes in chunks
     * without loading the whole untagged set into memory at once.
     */
    @Query(
        """
        SELECT id FROM photos
        WHERE id NOT IN (SELECT photo_id FROM photo_tag_status)
        ORDER BY date_taken_ms DESC
        LIMIT :limit
        """,
    )
    suspend fun untaggedPhotoIds(limit: Int): List<Long>

    /** How many photos have been processed (status row present). */
    @Query("SELECT COUNT(*) FROM photo_tag_status")
    fun observeProcessedCount(): Flow<Int>

    /** Total photos in the library — denominator for the coverage line. */
    @Query("SELECT COUNT(*) FROM photos")
    fun observeTotalPhotos(): Flow<Int>

    /** Total tag rows produced (labels + captions). */
    @Query("SELECT COUNT(*) FROM photo_tags")
    fun observeTagCount(): Flow<Int>

    /**
     * Sum of label/caption text byte-length across every tag row. The
     * Settings screen adds a per-row overhead estimate on top to surface
     * an approximate on-disk footprint (the tags share the single SQLite
     * file, so an exact isolated size isn't queryable).
     */
    @Query("SELECT COALESCE(SUM(LENGTH(label)), 0) FROM photo_tags")
    fun observeLabelByteSum(): Flow<Long>

    /** Tags for one photo — for the viewer / future tag chips. */
    @Query("SELECT * FROM photo_tags WHERE photo_id = :photoId ORDER BY confidence DESC")
    suspend fun tagsForPhoto(photoId: Long): List<PhotoTagEntity>

    /**
     * Photos that carry a tag (label or caption) matching [pattern] — a
     * SQL LIKE pattern such as `%dog%`. Powers search-by-content: typing
     * "dog" finds photos the tagger labelled "Dog" or captioned with a
     * sentence containing "dog". Date-desc; the repo merges this with the
     * filename FTS results. Observes both tables so new tags refresh live.
     */
    @Query(
        """
        SELECT photos.* FROM photos
        WHERE photos.id IN (
            SELECT photo_id FROM photo_tags
            WHERE LOWER(label) LIKE LOWER(:pattern) ESCAPE '\'
        )
        ORDER BY photos.date_taken_ms DESC
        """,
    )
    fun observePhotosWithTagLike(pattern: String): kotlinx.coroutines.flow.Flow<List<PhotoEntity>>

    @Query("DELETE FROM photo_tags")
    suspend fun wipeTags()

    @Query("DELETE FROM photo_tag_status")
    suspend fun wipeStatus()
}
