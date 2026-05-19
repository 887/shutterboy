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
    version = 2,
    exportSchema = true,
)
abstract class ShutterboyDatabase : RoomDatabase() {
    abstract fun photos(): PhotoDao
    abstract fun folders(): FolderDao
    abstract fun favorites(): PhotoFavoriteDao
    abstract fun search(): PhotoSearchDao
}

/**
 * Drop the CASCADE foreign key on `photo_favorites(photo_id) ->
 * photos(id)` so a transient empty scan doesn't wipe the user's
 * favorites. Recreates the table without the FK, copying existing
 * rows over.
 */
val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS photo_favorites_new (photo_id INTEGER NOT NULL PRIMARY KEY)")
        db.execSQL("INSERT OR IGNORE INTO photo_favorites_new (photo_id) SELECT photo_id FROM photo_favorites")
        db.execSQL("DROP TABLE photo_favorites")
        db.execSQL("ALTER TABLE photo_favorites_new RENAME TO photo_favorites")
    }
}
