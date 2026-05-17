package com.eight87.shutterboy.data.db

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Phase G search reads. Wired in Phase B's schema so the FTS table exists from
 * v1; the actual search-overlay UI lands in Phase G.
 */
@Dao
interface PhotoSearchDao {
    /**
     * FTS substring match on `display_name + exif_lens_model`. Caller is
     * responsible for shaping a safe FTS MATCH expression — strip metacharacters
     * per token, append `*` to the last token for prefix search. See tonearmboy's
     * `data/db/SearchExpressions.ftsMatch` for the canonical sanitisation.
     */
    @Query(
        """
        SELECT photos.* FROM photos
        JOIN photo_fts ON photos.rowid = photo_fts.rowid
        WHERE photo_fts MATCH :ftsQuery
        ORDER BY photos.date_taken_ms DESC
        """
    )
    fun observeMatching(ftsQuery: String): Flow<List<PhotoEntity>>

    /** Folder-name match. Combine with [observeMatching] at the repo layer. */
    @Query(
        """
        SELECT photos.* FROM photos
        INNER JOIN folders ON folders.id = photos.folder_id
        WHERE LOWER(folders.display_name) LIKE LOWER(:pattern)
        ORDER BY photos.date_taken_ms DESC
        """
    )
    fun observeByFolderNameLike(pattern: String): Flow<List<PhotoEntity>>

    /**
     * Phase I.3.d — destructive wipe used by `LibraryScanner.resetAndRescan`.
     * `photo_fts` is a `contentEntity = PhotoEntity` shadow, so deleting
     * from `photos` already cascades; this call is defensive belt-and-
     * braces and a no-op once the photos table is empty.
     */
    @Query("DELETE FROM photo_fts")
    suspend fun deleteAll()
}
