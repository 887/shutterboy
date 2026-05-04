package com.eight87.shutterboy.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "photo_favorites",
    foreignKeys = [
        ForeignKey(
            entity = PhotoEntity::class,
            parentColumns = ["id"],
            childColumns = ["photo_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class PhotoFavoriteEntity(
    @PrimaryKey
    @ColumnInfo(name = "photo_id")
    val photoId: Long,
)
