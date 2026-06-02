package com.eight87.shutterboy.data.db

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Smart Tags B.5 — exercises [MIGRATION_2_3] against a real (in-memory)
 * SQLite database seeded with the v2 `folders` + `photos` tables, then
 * asserts the new `photo_tags` / `photo_tag_status` tables exist, accept
 * rows, and that the `photo_tags → photos` FK cascades on delete.
 *
 * Pure JVM/Robolectric — no device, no MigrationTestHelper asset plumbing.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class Migration2to3Test {

    private val foldersV2 =
        "CREATE TABLE IF NOT EXISTS `folders` (`id` INTEGER NOT NULL, " +
            "`display_name` TEXT NOT NULL, `source_type` TEXT NOT NULL, " +
            "`saf_tree_uri` TEXT, `photo_count` INTEGER NOT NULL, " +
            "`cover_photo_id` INTEGER, PRIMARY KEY(`id`))"

    private val photosV2 =
        "CREATE TABLE IF NOT EXISTS `photos` (`id` INTEGER NOT NULL, " +
            "`content_uri` TEXT NOT NULL, `display_name` TEXT NOT NULL, " +
            "`date_taken_ms` INTEGER NOT NULL, `date_added_ms` INTEGER NOT NULL, " +
            "`width` INTEGER NOT NULL, `height` INTEGER NOT NULL, " +
            "`size_bytes` INTEGER NOT NULL, `mime_type` TEXT NOT NULL, " +
            "`folder_id` INTEGER NOT NULL, `exif_lens_model` TEXT, " +
            "`exif_focal_length` REAL, `exif_iso` INTEGER, `exif_aperture` REAL, " +
            "`exif_shutter_speed_sec` REAL, `latitude` REAL, `longitude` REAL, " +
            "PRIMARY KEY(`id`), FOREIGN KEY(`folder_id`) REFERENCES `folders`(`id`) " +
            "ON UPDATE NO ACTION ON DELETE CASCADE )"

    private fun openV2(): SupportSQLiteDatabase {
        val context: Context = ApplicationProvider.getApplicationContext()
        val callback = object : SupportSQLiteOpenHelper.Callback(2) {
            override fun onCreate(db: SupportSQLiteDatabase) {
                db.execSQL(foldersV2)
                db.execSQL(photosV2)
            }
            override fun onUpgrade(db: SupportSQLiteDatabase, oldV: Int, newV: Int) = Unit
        }
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(null) // in-memory
                .callback(callback)
                .build(),
        )
        return helper.writableDatabase
    }

    @Test
    fun `migration creates tag tables that accept rows`() {
        val db = openV2()
        MIGRATION_2_3.migrate(db)

        val tables = mutableSetOf<String>()
        db.query("SELECT name FROM sqlite_master WHERE type='table'").use { c ->
            while (c.moveToNext()) tables.add(c.getString(0))
        }
        assertTrue("photo_tags table missing: $tables", "photo_tags" in tables)
        assertTrue("photo_tag_status table missing: $tables", "photo_tag_status" in tables)

        db.execSQL("INSERT INTO folders (id, display_name, source_type, photo_count) VALUES (1, 'f', 'DEVICE', 0)")
        db.execSQL("INSERT INTO photos (id, content_uri, display_name, date_taken_ms, date_added_ms, width, height, size_bytes, mime_type, folder_id) VALUES (10, 'u', 'p.jpg', 0, 0, 1, 1, 1, 'image/jpeg', 1)")
        db.execSQL("INSERT INTO photo_tags (photo_id, label, confidence, source) VALUES (10, 'Dog', 0.9, 'label')")
        db.execSQL("INSERT INTO photo_tag_status (photo_id, status, tagged_at_ms, engine) VALUES (10, 'done', 123, 'mlkit-v1')")

        db.query("SELECT COUNT(*) FROM photo_tags").use { c -> c.moveToFirst(); assertEquals(1, c.getInt(0)) }
        db.query("SELECT COUNT(*) FROM photo_tag_status").use { c -> c.moveToFirst(); assertEquals(1, c.getInt(0)) }
    }

    @Test
    fun `deleting a photo cascades into its tags`() {
        val db = openV2()
        MIGRATION_2_3.migrate(db)
        db.execSQL("PRAGMA foreign_keys=ON")

        db.execSQL("INSERT INTO folders (id, display_name, source_type, photo_count) VALUES (1, 'f', 'DEVICE', 0)")
        db.execSQL("INSERT INTO photos (id, content_uri, display_name, date_taken_ms, date_added_ms, width, height, size_bytes, mime_type, folder_id) VALUES (10, 'u', 'p.jpg', 0, 0, 1, 1, 1, 'image/jpeg', 1)")
        db.execSQL("INSERT INTO photo_tags (photo_id, label, confidence, source) VALUES (10, 'Dog', 0.9, 'label')")

        db.execSQL("DELETE FROM photos WHERE id = 10")

        db.query("SELECT COUNT(*) FROM photo_tags").use { c -> c.moveToFirst(); assertEquals(0, c.getInt(0)) }
    }
}
