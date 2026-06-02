package com.eight87.shutterboy.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        PhotoEntity::class,
        FolderEntity::class,
        PhotoFavoriteEntity::class,
        PhotoFts::class,
        PhotoTagEntity::class,
        PhotoTagStatusEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class ShutterboyDatabase : RoomDatabase() {
    abstract fun photos(): PhotoDao
    abstract fun folders(): FolderDao
    abstract fun favorites(): PhotoFavoriteDao
    abstract fun search(): PhotoSearchDao
    abstract fun photoTags(): PhotoTagDao
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

/**
 * Smart Tags — adds the `photo_tags` (FK CASCADE on `photos`) and
 * `photo_tag_status` (no FK) tables plus their indices. DDL matches
 * Room's generated schema for v3 exactly so `MigrationTestHelper`
 * validates clean.
 */
val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `photo_tags` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`photo_id` INTEGER NOT NULL, " +
                "`label` TEXT NOT NULL, " +
                "`confidence` REAL NOT NULL, " +
                "`source` TEXT NOT NULL, " +
                "FOREIGN KEY(`photo_id`) REFERENCES `photos`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_photo_tags_photo_id` ON `photo_tags` (`photo_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_photo_tags_label` ON `photo_tags` (`label`)")
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_photo_tags_photo_id_label_source` " +
                "ON `photo_tags` (`photo_id`, `label`, `source`)",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `photo_tag_status` (" +
                "`photo_id` INTEGER NOT NULL, " +
                "`status` TEXT NOT NULL, " +
                "`tagged_at_ms` INTEGER NOT NULL, " +
                "`engine` TEXT NOT NULL, " +
                "PRIMARY KEY(`photo_id`) )",
        )
    }
}
