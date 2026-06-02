package com.eight87.shutterboy.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Smart Tags — one on-device-derived tag for a photo. Either a keyword
 * label (ML Kit Image Labeling) or a natural-language caption (ML Kit
 * GenAI Image Description), distinguished by [source].
 *
 * FK CASCADE on `photo_id` so deleting a photo (via scan reconciliation
 * or reset) drops its tags too. The unique index on
 * `(photo_id, label, source)` makes re-running the tagger idempotent —
 * `@Upsert` collapses duplicates instead of stacking rows.
 */
@Entity(
    tableName = "photo_tags",
    foreignKeys = [
        ForeignKey(
            entity = PhotoEntity::class,
            parentColumns = ["id"],
            childColumns = ["photo_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["photo_id"]),
        Index(value = ["label"]),
        Index(value = ["photo_id", "label", "source"], unique = true),
    ],
)
data class PhotoTagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "photo_id") val photoId: Long,
    val label: String,
    val confidence: Float,
    /** "label" (keyword) or "caption" (sentence). */
    val source: String,
)
