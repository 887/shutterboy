package com.eight87.shutterboy.data.repo

import com.eight87.shutterboy.domain.Photo
import kotlinx.coroutines.flow.Flow

interface PhotoSearch {
    fun searchPhotos(query: String): Flow<List<Photo>>
    fun recentSearches(): Flow<List<String>>
    suspend fun recordSearch(query: String)
}
