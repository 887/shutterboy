package com.eight87.shutterboy.ui.multiselect

import com.eight87.shutterboy.domain.Folder
import com.eight87.shutterboy.domain.FolderId
import com.eight87.shutterboy.domain.SourceType
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Phase H.4 — pure-JUnit coverage of [filterMoveTargets]. The picker must
 * never show the source folder as a destination.
 */
class MoveTargetSelectionTest {

    private fun folder(id: Long, name: String) = Folder(
        id = FolderId(id),
        displayName = name,
        sourceType = SourceType.DEVICE,
        photoCount = id.toInt(),
    )

    @Test
    fun filter_drops_the_source_folder() {
        val folders = listOf(folder(1, "Camera"), folder(2, "Screenshots"), folder(3, "Whatsapp"))
        val result = filterMoveTargets(folders, FolderId(2))
        assertEquals(listOf(folder(1, "Camera"), folder(3, "Whatsapp")), result)
    }

    @Test
    fun null_source_keeps_every_folder() {
        val folders = listOf(folder(1, "Camera"), folder(2, "Screenshots"))
        assertEquals(folders, filterMoveTargets(folders, sourceFolderId = null))
    }

    @Test
    fun unknown_source_is_a_noop() {
        val folders = listOf(folder(1, "Camera"))
        assertEquals(folders, filterMoveTargets(folders, FolderId(99)))
    }

    @Test
    fun empty_input_stays_empty() {
        assertEquals(emptyList<Folder>(), filterMoveTargets(emptyList(), FolderId(1)))
    }

    @Test
    fun relative_path_format_for_a_simple_folder() {
        val folder = folder(1, "Camera")
        assertEquals("Pictures/Camera/", relativePathFor(folder))
    }

    @Test
    fun relative_path_strips_stray_slashes() {
        val folder = folder(1, "/Weird/")
        assertEquals("Pictures/Weird/", relativePathFor(folder))
    }
}
