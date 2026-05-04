package com.eight87.shutterboy.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        PhotoEntity::class,
        FolderEntity::class,
        PhotoFavoriteEntity::class,
        PhotoFts::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class ShutterboyDatabase : RoomDatabase() {
    abstract fun photos(): PhotoDao
    abstract fun folders(): FolderDao
    abstract fun favorites(): PhotoFavoriteDao
    abstract fun search(): PhotoSearchDao
}
