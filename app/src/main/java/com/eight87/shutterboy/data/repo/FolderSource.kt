package com.eight87.shutterboy.data.repo

import com.eight87.shutterboy.domain.Folder
import com.eight87.shutterboy.domain.FolderId
import com.eight87.shutterboy.domain.Photo
import kotlinx.coroutines.flow.Flow

interface FolderSource {
    fun observeFolders(): Flow<List<Folder>>
    fun observeFolder(id: FolderId): Flow<Folder?>

    /**
     * Cover photo per folder. Resolves [Folder.coverPhotoId] via a
     * single batch read per emission; folders without a cover photo
     * are absent from the map. Lets the Collections grid render tiles
     * reactively without each `FolderTile` doing its own one-photo
     * query.
     */
    fun observeFolderCovers(): Flow<Map<FolderId, Photo>>
}
