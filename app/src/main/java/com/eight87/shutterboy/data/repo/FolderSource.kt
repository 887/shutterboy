package com.eight87.shutterboy.data.repo

import com.eight87.shutterboy.domain.Folder
import com.eight87.shutterboy.domain.FolderId
import kotlinx.coroutines.flow.Flow

interface FolderSource {
    fun observeFolders(): Flow<List<Folder>>
    fun observeFolder(id: FolderId): Flow<Folder?>
}
