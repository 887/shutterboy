package com.eight87.shutterboy.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Smart Tags — per-photo processing marker. Records that the tagger has
 * already looked at a photo so the background pass skips it next time,
 * even when the photo produced zero tags (a blank wall yields no labels
 * but must not be re-analysed on every scan).
 *
 * Deliberately NO foreign key (mirrors `photo_favorites`): a transient
 * empty scan must not cascade-wipe processing markers and force a full
 * re-tag of the whole library. Orphan rows (photo since deleted) are
 * harmless — they're never read without a join to `photos`.
 *
 * [engine] tags the model/config that produced the marker so a future
 * engine bump can invalidate stale markers and re-tag.
 */
@Entity(tableName = "photo_tag_status")
data class PhotoTagStatusEntity(
    @PrimaryKey @ColumnInfo(name = "photo_id") val photoId: Long,
    /** "done" or "failed". */
    val status: String,
    @ColumnInfo(name = "tagged_at_ms") val taggedAtMs: Long,
    val engine: String,
)
